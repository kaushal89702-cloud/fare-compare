package com.family.farecompare.domain.automation

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.accessibility.AccessibilityStatusChecker
import com.family.farecompare.domain.connectivity.InternetConnectivityChecker
import com.family.farecompare.domain.fare.FareExtractor
import com.family.farecompare.domain.fare.NoRidesAvailableDetector
import com.family.farecompare.domain.inspector.FailureDiagnosticsRecorder
import com.family.farecompare.domain.location.DetectedField
import com.family.farecompare.domain.location.FieldRole
import com.family.farecompare.domain.location.LocationFieldDetector
import com.family.farecompare.domain.location.SuggestionSelector
import com.family.farecompare.domain.model.AutomationFailureReason
import com.family.farecompare.domain.model.FareQuote
import com.family.farecompare.domain.model.NodeBounds
import com.family.farecompare.domain.model.ProviderComparisonOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private const val FOREGROUND_WAIT_TIMEOUT_MS = 10_000L
private const val FIELD_APPEAR_TIMEOUT_MS = 12_000L
private const val CONFIRM_APPEAR_TIMEOUT_MS = 6_000L
private const val FARE_APPEAR_TIMEOUT_MS = 30_000L
private const val UI_SETTLE_DELAY_MS = 400L
private const val MAX_FIELD_RETRIES = 4
private const val MAX_SCROLL_ATTEMPTS = 3

/**
 * Explicit per-provider state machine that drives the full ride-booking
 * automation flow:
 *
 * WAIT_APP -> WAIT_PICKUP_FIELD -> ENTER_PICKUP -> WAIT_PICKUP_CONFIRM ->
 * WAIT_DESTINATION_FIELD -> ENTER_DESTINATION -> WAIT_DESTINATION_CONFIRM ->
 * WAIT_FARE_SCREEN -> EXTRACT_FARE -> RETURN_TO_COMPARE_APP -> NEXT_APP
 *
 * The engine only ever advances to the next state after verifying the
 * current one actually succeeded (a confirmed pickup field value, a
 * confirmed destination field value, a fare actually read from screen).
 * It never moves on to [AutomationState.NEXT_APP] - i.e. never leaves the
 * current provider app - until both pickup and destination are confirmed,
 * or a bounded timeout/retry budget is exhausted.
 *
 * Every wait is event-driven via [WindowContentEventBus] (real
 * accessibility events), never a fixed polling `delay()` loop, aside from a
 * small bounded settle delay after each UI-mutating action to let the
 * target app finish its own animation/layout pass - standard practice for
 * accessibility automation, not a substitute for event-driven waiting.
 *
 * Field/suggestion lookup uses multiple strategies in order (text, hint,
 * content description, resource id, provider-specific hints, then - only
 * as an explicit last resort - any visible editable field), retries with
 * backoff, and scrolls the screen when nothing is found. Every failure
 * triggers [FailureDiagnosticsRecorder] so the *reason* a field wasn't
 * found is captured (and exported) instead of the engine blindly moving on
 * or giving up silently.
 */
class RideAutomationEngine @Inject constructor(
    private val accessibilityStatusChecker: AccessibilityStatusChecker,
    private val internetConnectivityChecker: InternetConnectivityChecker,
    private val accessibilityGatewayRepository: AccessibilityGatewayRepository,
    private val windowContentEventBus: WindowContentEventBus,
    private val locationFieldDetector: LocationFieldDetector,
    private val suggestionSelector: SuggestionSelector,
    private val accessibilityActionExecutor: AccessibilityActionExecutor,
    private val scrollHelper: ScrollHelper,
    private val fareExtractor: FareExtractor,
    private val noRidesAvailableDetector: NoRidesAvailableDetector,
    private val failureDiagnosticsRecorder: FailureDiagnosticsRecorder,
    private val appReturner: AppReturner
) {
    /**
     * Set by [runFieldStates] right before it gives up on a field, so
     * [fail] can attach a human-readable summary of what was actually
     * found on screen to the emitted [ProviderComparisonOutcome.Failure].
     * Cleared at the start of every provider run.
     */
    private var lastDiagnosticsSummary: String? = null

    operator fun invoke(
        provider: RideAppProvider,
        pickupAddress: String,
        destinationAddress: String
    ): Flow<RideAutomationStep> = flow {
        lastDiagnosticsSummary = null
        try {
            log(provider, AutomationState.WAIT_APP, "Checking accessibility service")
            emit(progress(provider, AutomationState.WAIT_APP, "Checking accessibility..."))
            if (!accessibilityStatusChecker.isServiceEnabled()) {
                emit(fail(provider, AutomationFailureReason.AccessibilityDisabled))
                return@flow
            }

            if (!internetConnectivityChecker.isConnected()) {
                emit(fail(provider, AutomationFailureReason.NoInternet))
                return@flow
            }

            emit(progress(provider, AutomationState.WAIT_APP, "Checking ${provider.displayName} is installed..."))
            if (!provider.isInstalled()) {
                emit(fail(provider, AutomationFailureReason.AppNotInstalled))
                return@flow
            }

            emit(progress(provider, AutomationState.WAIT_APP, "Launching ${provider.displayName}..."))
            provider.launch()

            log(provider, AutomationState.WAIT_APP, "Waiting for ${provider.displayName} to reach foreground")
            if (!provider.waitUntilForeground(FOREGROUND_WAIT_TIMEOUT_MS)) {
                emit(fail(provider, AutomationFailureReason.LaunchTimeout))
                return@flow
            }
            log(provider, AutomationState.WAIT_APP, "${provider.displayName} is now the foreground app")

            // --- Pickup ---
            val pickupResult = runFieldStates(
                provider = provider,
                address = pickupAddress,
                role = FieldRole.PICKUP,
                waitFieldState = AutomationState.WAIT_PICKUP_FIELD,
                enterState = AutomationState.ENTER_PICKUP,
                confirmState = AutomationState.WAIT_PICKUP_CONFIRM,
                hints = provider.pickupFieldHints,
                excludeBounds = null,
                emit = { emit(it) }
            )
            if (pickupResult == null) {
                emit(fail(provider, AutomationFailureReason.PickupFieldNotFound))
                return@flow
            }
            log(provider, AutomationState.WAIT_PICKUP_CONFIRM, "Entered pickup: '$pickupAddress' confirmed")

            // --- Destination: never proceed to NEXT_APP without this succeeding ---
            val destinationResult = runFieldStates(
                provider = provider,
                address = destinationAddress,
                role = FieldRole.DESTINATION,
                waitFieldState = AutomationState.WAIT_DESTINATION_FIELD,
                enterState = AutomationState.ENTER_DESTINATION,
                confirmState = AutomationState.WAIT_DESTINATION_CONFIRM,
                hints = provider.destinationFieldHints,
                excludeBounds = pickupResult,
                emit = { emit(it) }
            )
            if (destinationResult == null) {
                emit(fail(provider, AutomationFailureReason.DestinationFieldNotFound))
                return@flow
            }
            log(provider, AutomationState.WAIT_DESTINATION_CONFIRM, "Entered destination: '$destinationAddress' confirmed")

            // --- Fare ---
            emit(progress(provider, AutomationState.WAIT_FARE_SCREEN, "Waiting for ${provider.displayName} fare..."))
            val quotes = waitForFares(provider)
            if (quotes.isEmpty()) {
                val rootNode = accessibilityGatewayRepository.currentRootNode()
                val failureReason = if (rootNode != null && noRidesAvailableDetector.isNoRidesAvailableScreen(rootNode)) {
                    AutomationFailureReason.NoRidesAvailable
                } else {
                    AutomationFailureReason.FareNotFoundBeforeTimeout
                }
                failureDiagnosticsRecorder.captureFailure(provider.displayName, failureReason.javaClass.simpleName, rootNode)
                emit(fail(provider, failureReason))
                return@flow
            }
            log(provider, AutomationState.EXTRACT_FARE, "Extracted ${quotes.size} fare(s)")
            emit(progress(provider, AutomationState.EXTRACT_FARE, "Fare extracted"))

            emit(progress(provider, AutomationState.RETURN_TO_COMPARE_APP, "Returning to FareCompare..."))
            appReturner.bringFareCompareToForeground()

            emit(progress(provider, AutomationState.NEXT_APP, "Done with ${provider.displayName}"))
            emit(RideAutomationStep.Finished(ProviderComparisonOutcome.Success(quotes)))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Log.e(TAG, "[${provider.displayName}] Unexpected error: ${exception.message}")
            failureDiagnosticsRecorder.captureFailure(
                provider.displayName,
                "Unexpected: ${exception.message}",
                accessibilityGatewayRepository.currentRootNode()
            )
            emit(fail(provider, AutomationFailureReason.Unexpected(exception.message ?: exception.javaClass.simpleName)))
        }
    }

    /**
     * Runs WAIT_*_FIELD -> ENTER_* -> WAIT_*_CONFIRM for one [role].
     * Returns the bounds of the field that was successfully filled, or
     * null if it could not be filled after every retry/scroll/fallback
     * strategy was exhausted - in which case the caller must stop and fail
     * rather than proceeding to the next state.
     */
    private suspend fun runFieldStates(
        provider: RideAppProvider,
        address: String,
        role: FieldRole,
        waitFieldState: AutomationState,
        enterState: AutomationState,
        confirmState: AutomationState,
        hints: List<String>,
        excludeBounds: NodeBounds?,
        emit: suspend (RideAutomationStep) -> Unit
    ): NodeBounds? {
        var retryCount = 0

        while (retryCount <= MAX_FIELD_RETRIES) {
            emit(progress(provider, waitFieldState, "Looking for ${role.label()} field...", retryCount))
            log(provider, waitFieldState, "Attempt ${retryCount + 1}/${MAX_FIELD_RETRIES + 1} to locate ${role.label()} field")

            val fieldLookup = findFieldWithFallbacks(provider, role, hints, excludeBounds, waitFieldState, retryCount)
            if (fieldLookup == null) {
                retryCount++
                continue
            }
            val (fieldNode, isPlaceholder) = fieldLookup

            if (isPlaceholder) {
                log(provider, enterState, "Tapping placeholder to reveal ${role.label()} text input")
                if (!accessibilityActionExecutor.click(fieldNode)) {
                    log(provider, enterState, "Failed to click ${role.label()} placeholder, retrying")
                    retryCount++
                    continue
                }
                settle()
                waitForAnyWindowContentChange(provider.packageName, FIELD_APPEAR_TIMEOUT_MS)
                // After tapping the placeholder, re-search for the now-real editable field.
                retryCount++
                continue
            }

            emit(progress(provider, enterState, "Entering ${role.label()}: $address", retryCount))
            val setTextSucceeded = accessibilityActionExecutor.setText(fieldNode, address)
            if (!setTextSucceeded) {
                log(provider, enterState, "setText failed on ${role.label()} field, retrying")
                retryCount++
                continue
            }
            log(provider, enterState, "Entered ${role.label()}: '$address'")
            settle()

            emit(progress(provider, confirmState, "Confirming ${role.label()}...", retryCount))
            val suggestionRoot = waitForAnyWindowContentChange(provider.packageName, CONFIRM_APPEAR_TIMEOUT_MS)
                ?: accessibilityGatewayRepository.currentRootNode()

            val fieldBounds = boundsOf(fieldNode)

            if (suggestionRoot == null) {
                log(provider, confirmState, "No window content available after entering ${role.label()}, retrying")
                retryCount++
                continue
            }

            val suggestionNode = suggestionSelector.findBestSuggestion(suggestionRoot, address)
            if (suggestionNode == null) {
                log(provider, confirmState, "No matching suggestion found for ${role.label()}, retrying")
                retryCount++
                continue
            }

            val clicked = accessibilityActionExecutor.click(suggestionNode)
            if (!clicked) {
                log(provider, confirmState, "Failed to click suggestion for ${role.label()}, retrying")
                retryCount++
                continue
            }
            settle()
            log(provider, confirmState, "${role.label()} confirmed via suggestion tap")
            return fieldBounds
        }

        log(provider, confirmState, "Giving up on ${role.label()} after $retryCount attempts")
        val rootNode = accessibilityGatewayRepository.currentRootNode()
        val diagnostics = failureDiagnosticsRecorder.captureFailure(
            provider.displayName,
            "${role.label()}FieldNotFound",
            rootNode
        )
        val diagnosticsSummary = buildString {
            append("${diagnostics.totalNodeCount} nodes, ${diagnostics.editableFieldDescriptions.size} editable field(s) present")
            if (diagnostics.editableFieldDescriptions.isNotEmpty()) {
                append(": ")
                append(diagnostics.editableFieldDescriptions.joinToString(separator = " | "))
            }
            diagnostics.exportedFilePath?.let { append(" (exported: $it)") }
        }
        log(provider, waitFieldState, "Diagnostics: $diagnosticsSummary")
        lastDiagnosticsSummary = diagnosticsSummary
        return null
    }

    /**
     * Tries, in order: keyword-matched editable field -> scroll and retry
     * keyword search -> provider-hinted clickable placeholder (e.g. Uber's
     * "Where to?") -> as a last resort on later retries only, any visible
     * editable field that isn't the one already used for the other role.
     * Returns the found node plus whether it was a placeholder (needs a
     * tap before text entry) rather than a real text field.
     */
    private suspend fun findFieldWithFallbacks(
        provider: RideAppProvider,
        role: FieldRole,
        hints: List<String>,
        excludeBounds: NodeBounds?,
        state: AutomationState,
        retryCount: Int
    ): Pair<AccessibilityNodeInfo, Boolean>? {
        val root = accessibilityGatewayRepository.currentRootNode()
        if (root == null) {
            log(provider, state, "No root node available yet")
            waitForAnyWindowContentChange(provider.packageName, FIELD_APPEAR_TIMEOUT_MS / (MAX_FIELD_RETRIES + 1))
            return null
        }

        val detected = locationFieldDetector.detectField(root, role, hints)
        if (detected != null) {
            val node = locateNodeByValueAndBounds(root, detected.value, detected.bounds, depth = 0)
            if (node != null) {
                log(provider, state, "Node found by keyword search for ${role.label()} (resourceId=${detected.resourceId ?: "-"})")
                return node to false
            }
        }
        log(provider, state, "Node NOT found by keyword search for ${role.label()}")

        if (retryCount < MAX_SCROLL_ATTEMPTS) {
            val scrolled = scrollHelper.scrollForward(root)
            log(provider, state, "Scroll attempt for ${role.label()} field: ${if (scrolled) "scrolled" else "nothing scrollable"}")
            if (scrolled) {
                settle()
                waitForAnyWindowContentChange(provider.packageName, FIELD_APPEAR_TIMEOUT_MS / (MAX_FIELD_RETRIES + 1))
                val rescanRoot = accessibilityGatewayRepository.currentRootNode() ?: return null
                val afterScroll = locationFieldDetector.detectField(rescanRoot, role, hints)
                if (afterScroll != null) {
                    val node = locateNodeByValueAndBounds(rescanRoot, afterScroll.value, afterScroll.bounds, depth = 0)
                    if (node != null) {
                        log(provider, state, "Node found after scrolling for ${role.label()}")
                        return node to false
                    }
                }
            }
        }

        val placeholder = locationFieldDetector.findClickablePlaceholder(root, role, hints)
        if (placeholder != null) {
            val node = locateNodeByValueAndBounds(root, placeholder.value, placeholder.bounds, depth = 0)
            if (node != null) {
                log(provider, state, "Clickable placeholder found for ${role.label()}, will tap to reveal text field")
                return node to true
            }
        }

        // Wording-independent: for destination specifically, once pickup's
        // location on screen is known (excludeBounds), the destination
        // input is almost always the very next interactable element below
        // it - this works regardless of whatever text/hint the app uses.
        if (role == FieldRole.DESTINATION && excludeBounds != null && retryCount >= 1) {
            val below = locationFieldDetector.findFieldBelow(root, excludeBounds)
            if (below != null) {
                val node = locateNodeByValueAndBounds(root, below.value, below.bounds, depth = 0)
                if (node != null) {
                    log(provider, state, "Found next field below pickup for ${role.label()} (position-based fallback)")
                    return node to false
                }
            }
        }

        if (retryCount >= MAX_FIELD_RETRIES - 1) {
            val fallback = locationFieldDetector.findAnyEditableField(root, excludeBounds)
            if (fallback != null) {
                val node = locateNodeByValueAndBounds(root, fallback.value, fallback.bounds, depth = 0)
                if (node != null) {
                    log(provider, state, "Falling back to any visible editable field for ${role.label()} as last resort")
                    return node to false
                }
            }
        }

        waitForAnyWindowContentChange(provider.packageName, FIELD_APPEAR_TIMEOUT_MS / (MAX_FIELD_RETRIES + 1))
        return null
    }

    private fun locateNodeByValueAndBounds(
        node: AccessibilityNodeInfo?,
        value: String,
        bounds: NodeBounds,
        depth: Int
    ): AccessibilityNodeInfo? {
        if (node == null || depth > MAX_NODE_SEARCH_DEPTH) return null

        val nodeBounds = Rect()
        node.getBoundsInScreen(nodeBounds)
        val matchesBounds = nodeBounds.left == bounds.left && nodeBounds.top == bounds.top &&
            nodeBounds.right == bounds.right && nodeBounds.bottom == bounds.bottom
        val matchesValue = node.text?.toString() == value || node.hintText?.toString() == value ||
            node.contentDescription?.toString() == value

        if (matchesBounds && matchesValue) return node

        for (i in 0 until node.childCount) {
            locateNodeByValueAndBounds(node.getChild(i), value, bounds, depth + 1)?.let { return it }
        }
        return null
    }

    private fun boundsOf(node: AccessibilityNodeInfo): NodeBounds {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return NodeBounds(rect.left, rect.top, rect.right, rect.bottom)
    }

    private suspend fun waitForFares(provider: RideAppProvider): List<FareQuote> {
        val timestamp = System.currentTimeMillis()
        val deadline = System.currentTimeMillis() + FARE_APPEAR_TIMEOUT_MS

        while (System.currentTimeMillis() < deadline) {
            val rootNode = accessibilityGatewayRepository.currentRootNode()
            if (rootNode != null) {
                val detectedFares = fareExtractor.extractFares(rootNode)
                if (detectedFares.isNotEmpty()) {
                    log(provider, AutomationState.WAIT_FARE_SCREEN, "Fare screen detected with ${detectedFares.size} quote(s)")
                    return detectedFares.map { detected ->
                        FareQuote(
                            provider = com.family.farecompare.domain.model.RideProvider.values()
                                .first { it.packageName == provider.packageName },
                            rideType = detected.rideType,
                            fareAmount = detected.amount,
                            rawFareText = detected.rawText,
                            etaMinutes = detected.etaMinutes,
                            timestampMillis = timestamp
                        )
                    }
                }
            }
            waitForAnyWindowContentChange(provider.packageName, remainingTime(deadline))
        }
        return emptyList()
    }

    private suspend fun waitForAnyWindowContentChange(
        packageName: String,
        timeoutMs: Long
    ): AccessibilityNodeInfo? {
        if (timeoutMs <= 0) return accessibilityGatewayRepository.currentRootNode()
        withTimeoutOrNull(timeoutMs) {
            windowContentEventBus.events.filter { it.packageName == packageName }.first()
        }
        return accessibilityGatewayRepository.currentRootNode()
    }

    private fun remainingTime(deadline: Long): Long = (deadline - System.currentTimeMillis()).coerceAtLeast(0)

    private suspend fun settle() = delay(UI_SETTLE_DELAY_MS)

    private fun progress(provider: RideAppProvider, state: AutomationState, message: String, retryCount: Int = 0): RideAutomationStep =
        RideAutomationStep.InProgress(state = state, message = message, retryCount = retryCount)

    /**
     * Emits the terminal failure step for [provider]. Critically, this also
     * brings FareCompare back to the foreground for every failure reason
     * that can occur *after* the provider app was actually launched -
     * without this, a failed field search left the user stranded staring
     * at the ride app with no visible indication that automation had given
     * up and moved on, which is exactly what was reported: Uber "getting
     * stuck and doing nothing" was this method never returning control to
     * FareCompare on failure, not an infinite loop.
     */
    private suspend fun fail(provider: RideAppProvider, reason: AutomationFailureReason): RideAutomationStep {
        val rideProvider = com.family.farecompare.domain.model.RideProvider.values()
            .first { it.packageName == provider.packageName }
        log(provider, AutomationState.NEXT_APP, "Failed: ${reason.javaClass.simpleName}")

        // Only these three failure reasons can occur before provider.launch()
        // is ever called; every other reason means the provider app is (or
        // may still be) open on screen and FareCompare must reclaim focus.
        val occurredBeforeLaunch = reason is AutomationFailureReason.AccessibilityDisabled ||
            reason is AutomationFailureReason.AppNotInstalled ||
            reason is AutomationFailureReason.NoInternet
        val occurredAfterLaunch = !occurredBeforeLaunch
        if (occurredAfterLaunch) {
            log(provider, AutomationState.RETURN_TO_COMPARE_APP, "Returning to FareCompare after failure")
            appReturner.bringFareCompareToForeground()
        }

        return RideAutomationStep.Finished(
            ProviderComparisonOutcome.Failure(rideProvider, reason, diagnosticsSummary = lastDiagnosticsSummary)
        )
    }

    private fun FieldRole.label(): String = when (this) {
        FieldRole.PICKUP -> "pickup"
        FieldRole.DESTINATION -> "destination"
    }

    private fun log(provider: RideAppProvider, state: AutomationState, message: String) {
        Log.d(TAG, "[${provider.displayName}] [$state] $message")
    }

    private companion object {
        const val TAG = "RideAutomationEngine"
        const val MAX_NODE_SEARCH_DEPTH = 100
    }
}
