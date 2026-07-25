package com.family.farecompare.domain.automation

import android.util.Log
import com.family.farecompare.domain.accessibility.AccessibilityStatusChecker
import com.family.farecompare.domain.connectivity.InternetConnectivityChecker
import com.family.farecompare.domain.fare.FareExtractor
import com.family.farecompare.domain.fare.NoRidesAvailableDetector
import com.family.farecompare.domain.location.FieldRole
import com.family.farecompare.domain.location.LocationFieldDetector
import com.family.farecompare.domain.location.SuggestionSelector
import com.family.farecompare.domain.model.AutomationFailureReason
import com.family.farecompare.domain.model.FareQuote
import com.family.farecompare.domain.model.ProviderComparisonOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private const val FOREGROUND_WAIT_TIMEOUT_MS = 10_000L
private const val FIELD_APPEAR_TIMEOUT_MS = 8_000L
private const val SUGGESTION_APPEAR_TIMEOUT_MS = 8_000L
private const val FARE_APPEAR_TIMEOUT_MS = 25_000L
private const val UI_SETTLE_DELAY_MS = 400L

/**
 * End-to-end automation pipeline for a single [RideAppProvider]:
 *
 * accessibility check -> installed check -> launch -> wait for foreground
 * -> locate + fill pickup field -> select a matching suggestion -> locate +
 * fill destination field -> select a matching suggestion -> wait for a fare
 * to appear on screen -> extract it -> return to FareCompare.
 *
 * Every wait is driven by [WindowContentEventBus] (real accessibility
 * events), never a fixed `delay()` polling loop, except for a very small
 * settle delay after each UI-mutating action to let the target app finish
 * its own animation/layout pass before the next read - this is standard
 * practice for accessibility automation and is bounded, not a substitute
 * for event-driven waiting.
 *
 * This class is provider-agnostic: it depends only on [RideAppProvider],
 * never on Uber/Ola/Rapido-specific code, so adding a fourth provider only
 * requires a new [RideAppProvider] implementation.
 */
class RideAutomationEngine @Inject constructor(
    private val accessibilityStatusChecker: AccessibilityStatusChecker,
    private val internetConnectivityChecker: InternetConnectivityChecker,
    private val accessibilityGatewayRepository: AccessibilityGatewayRepository,
    private val windowContentEventBus: WindowContentEventBus,
    private val locationFieldDetector: LocationFieldDetector,
    private val suggestionSelector: SuggestionSelector,
    private val accessibilityActionExecutor: AccessibilityActionExecutor,
    private val fareExtractor: FareExtractor,
    private val noRidesAvailableDetector: NoRidesAvailableDetector,
    private val appReturner: AppReturner
) {
    operator fun invoke(
        provider: RideAppProvider,
        pickupAddress: String,
        destinationAddress: String
    ): Flow<RideAutomationStep> = flow {
        try {
            emit(RideAutomationStep.CheckingAccessibility)
            if (!accessibilityStatusChecker.isServiceEnabled()) {
                emit(finished(provider, AutomationFailureReason.AccessibilityDisabled))
                return@flow
            }

            if (!internetConnectivityChecker.isConnected()) {
                emit(finished(provider, AutomationFailureReason.NoInternet))
                return@flow
            }

            emit(RideAutomationStep.CheckingInstalled)
            if (!provider.isInstalled()) {
                emit(finished(provider, AutomationFailureReason.AppNotInstalled))
                return@flow
            }

            emit(RideAutomationStep.Launching)
            provider.launch()

            emit(RideAutomationStep.WaitingForForeground)
            if (!provider.waitUntilForeground(FOREGROUND_WAIT_TIMEOUT_MS)) {
                emit(finished(provider, AutomationFailureReason.LaunchTimeout))
                return@flow
            }

            emit(RideAutomationStep.FillingPickup)
            if (!fillFieldWithRetry(provider.packageName, FieldRole.PICKUP, pickupAddress)) {
                emit(finished(provider, AutomationFailureReason.PickupFieldNotFound))
                return@flow
            }

            emit(RideAutomationStep.FillingDestination)
            if (!fillFieldWithRetry(provider.packageName, FieldRole.DESTINATION, destinationAddress)) {
                emit(finished(provider, AutomationFailureReason.DestinationFieldNotFound))
                return@flow
            }

            emit(RideAutomationStep.WaitingForFare)
            val quotes = waitForFares(provider)
            if (quotes.isEmpty()) {
                val rootNode = accessibilityGatewayRepository.currentRootNode()
                val failureReason = if (rootNode != null && noRidesAvailableDetector.isNoRidesAvailableScreen(rootNode)) {
                    AutomationFailureReason.NoRidesAvailable
                } else {
                    AutomationFailureReason.FareNotFoundBeforeTimeout
                }
                emit(finished(provider, failureReason))
                return@flow
            }

            emit(RideAutomationStep.ReturningToFareCompare)
            appReturner.bringFareCompareToForeground()

            emit(RideAutomationStep.Finished(ProviderComparisonOutcome.Success(quotes)))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Log.e(TAG, "Unexpected error automating ${provider.displayName}: ${exception.message}")
            emit(finished(provider, AutomationFailureReason.Unexpected(exception.message ?: exception.javaClass.simpleName)))
        }
    }

    private suspend fun fillFieldWithRetry(packageName: String, role: FieldRole, address: String): Boolean {
        val fieldNode = waitForNode(packageName, FIELD_APPEAR_TIMEOUT_MS) { root ->
            findEditableFieldNode(root, role)
        } ?: return false

        val setTextSucceeded = accessibilityActionExecutor.setText(fieldNode, address)
        if (!setTextSucceeded) return false

        settle()

        val suggestionRoot = waitForAnyWindowContentChange(packageName, SUGGESTION_APPEAR_TIMEOUT_MS)
            ?: accessibilityGatewayRepository.currentRootNode()
            ?: return false

        val suggestionNode = suggestionSelector.findBestSuggestion(suggestionRoot, address) ?: return false
        val clicked = accessibilityActionExecutor.click(suggestionNode)
        if (clicked) settle()
        return clicked
    }

    /**
     * The generic [LocationFieldDetector] returns a text summary, not the
     * live node - re-walk the tree here to get the actual editable
     * [android.view.accessibility.AccessibilityNodeInfo] to act on. Kept
     * private to this engine since only automation (not the read-only
     * pickup-detection dashboard) needs the live node reference.
     */
    private fun findEditableFieldNode(
        rootNode: android.view.accessibility.AccessibilityNodeInfo,
        role: FieldRole
    ): android.view.accessibility.AccessibilityNodeInfo? {
        val detected = locationFieldDetector.detectField(rootNode, role) ?: return null
        return locateNodeByValueAndBounds(rootNode, detected.value, detected.bounds, depth = 0)
    }

    private fun locateNodeByValueAndBounds(
        node: android.view.accessibility.AccessibilityNodeInfo?,
        value: String,
        bounds: com.family.farecompare.domain.model.NodeBounds,
        depth: Int
    ): android.view.accessibility.AccessibilityNodeInfo? {
        if (node == null || depth > MAX_NODE_SEARCH_DEPTH) return null

        val nodeBounds = android.graphics.Rect()
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

    private suspend fun waitForFares(provider: RideAppProvider): List<FareQuote> {
        val timestamp = System.currentTimeMillis()
        val deadline = System.currentTimeMillis() + FARE_APPEAR_TIMEOUT_MS

        while (System.currentTimeMillis() < deadline) {
            val rootNode = accessibilityGatewayRepository.currentRootNode()
            if (rootNode != null) {
                val detectedFares = fareExtractor.extractFares(rootNode)
                if (detectedFares.isNotEmpty()) {
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

    private suspend fun waitForNode(
        packageName: String,
        timeoutMs: Long,
        finder: (android.view.accessibility.AccessibilityNodeInfo) -> android.view.accessibility.AccessibilityNodeInfo?
    ): android.view.accessibility.AccessibilityNodeInfo? {
        val deadline = System.currentTimeMillis() + timeoutMs

        accessibilityGatewayRepository.currentRootNode()?.let { root -> finder(root)?.let { return it } }

        while (System.currentTimeMillis() < deadline) {
            waitForAnyWindowContentChange(packageName, remainingTime(deadline))
            val root = accessibilityGatewayRepository.currentRootNode() ?: continue
            finder(root)?.let { return it }
        }
        return null
    }

    private suspend fun waitForAnyWindowContentChange(
        packageName: String,
        timeoutMs: Long
    ): android.view.accessibility.AccessibilityNodeInfo? {
        if (timeoutMs <= 0) return null
        withTimeoutOrNull(timeoutMs) {
            windowContentEventBus.events.filter { it.packageName == packageName }.first()
        }
        return accessibilityGatewayRepository.currentRootNode()
    }

    private fun remainingTime(deadline: Long): Long = (deadline - System.currentTimeMillis()).coerceAtLeast(0)

    private suspend fun settle() = kotlinx.coroutines.delay(UI_SETTLE_DELAY_MS)

    private fun finished(provider: RideAppProvider, reason: AutomationFailureReason): RideAutomationStep {
        val rideProvider = com.family.farecompare.domain.model.RideProvider.values()
            .first { it.packageName == provider.packageName }
        return RideAutomationStep.Finished(ProviderComparisonOutcome.Failure(rideProvider, reason))
    }

    private companion object {
        const val TAG = "RideAutomationEngine"
        const val MAX_NODE_SEARCH_DEPTH = 100
    }
}
