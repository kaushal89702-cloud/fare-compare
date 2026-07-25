package com.family.farecompare.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.data.foreground.ForegroundAppNameResolver
import com.family.farecompare.domain.automation.AccessibilityGateway
import com.family.farecompare.domain.automation.AccessibilityGatewayRepository
import com.family.farecompare.domain.automation.WindowContentEvent
import com.family.farecompare.domain.automation.WindowContentEventBus
import com.family.farecompare.domain.inspector.UiInspectorCoordinator
import com.family.farecompare.domain.location.PickupDetectionCoordinator
import com.family.farecompare.domain.foreground.ForegroundAppRepository
import com.family.farecompare.domain.model.ForegroundApp
import com.family.farecompare.domain.model.RideProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Accessibility service that:
 * 1. Detects the foreground application via window state change events and
 *    reports it to [ForegroundAppRepository] (Phase 5).
 * 2. When a known ride provider (Uber, Ola, Rapido) is in the foreground,
 *    hands its window content to [PickupDetectionCoordinator] so it can
 *    locate and score the pickup location field (Phase 7).
 * 3. When any known ride provider (Uber, Ola, or Rapido) is in the
 *    foreground, hands its window content to [UiInspectorCoordinator] for
 *    the developer UI Inspector.
 * 4. Publishes every window-state/content-changed event to
 *    [WindowContentEventBus] and exposes [rootInActiveWindow] via
 *    [AccessibilityGatewayRepository] so [com.family.farecompare.domain.automation.RideAutomationEngine]
 *    can wait for real UI changes and read/act on the live tree while
 *    filling pickup/destination fields and waiting for fares.
 *
 * The service itself never performs an action beyond what it is asked to by
 * the injected coordinators/engine - it is a thin, testable event dispatcher.
 */
@AndroidEntryPoint
class FareCompareAccessibilityService : AccessibilityService(), AccessibilityGateway {

    @Inject
    lateinit var foregroundAppRepository: ForegroundAppRepository

    @Inject
    lateinit var foregroundAppNameResolver: ForegroundAppNameResolver

    @Inject
    lateinit var pickupDetectionCoordinator: PickupDetectionCoordinator

    @Inject
    lateinit var uiInspectorCoordinator: UiInspectorCoordinator

    @Inject
    lateinit var accessibilityGatewayRepository: AccessibilityGatewayRepository

    @Inject
    lateinit var windowContentEventBus: WindowContentEventBus

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun currentRootNode(): AccessibilityNodeInfo? = rootInActiveWindow

    override fun onServiceConnected() {
        super.onServiceConnected()
        accessibilityGatewayRepository.attach(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventType = event?.eventType ?: return
        val packageName = event.packageName?.toString() ?: return

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handleForegroundAppChanged(packageName)
            uiInspectorCoordinator.onForegroundAppChanged(packageName)
        }

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            windowContentEventBus.publish(WindowContentEvent(packageName))
            maybeDetectPickupField(packageName)
            maybeDumpRideProviderUiTree(packageName)
        }
    }

    private fun handleForegroundAppChanged(packageName: String) {
        if (packageName == foregroundAppRepository.currentForegroundApp.value?.packageName) return

        val displayName = foregroundAppNameResolver.resolveDisplayName(packageName)
        foregroundAppRepository.updateForegroundApp(
            ForegroundApp(packageName = packageName, displayName = displayName)
        )
        Log.d(TAG, "Foreground App: $displayName")
    }

    private fun maybeDetectPickupField(packageName: String) {
        val provider = KNOWN_RIDE_PROVIDERS.firstOrNull { it.packageName == packageName } ?: return
        val rootNode = rootInActiveWindow ?: return

        serviceScope.launch {
            pickupDetectionCoordinator.analyze(rootNode, provider.displayName)
        }
    }

    private fun maybeDumpRideProviderUiTree(packageName: String) {
        uiInspectorCoordinator.onRelevantUiEvent(packageName) { rootInActiveWindow }
    }

    override fun onInterrupt() {
        // Interruption handling is implemented in a later automation phase.
    }

    override fun onDestroy() {
        super.onDestroy()
        accessibilityGatewayRepository.detach(this)
        serviceScope.cancel()
    }

    private companion object {
        const val TAG = "FareCompareAccessibility"
        val KNOWN_RIDE_PROVIDERS = RideProvider.values().toList()
    }
}
