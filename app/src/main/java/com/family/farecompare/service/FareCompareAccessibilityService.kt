package com.family.farecompare.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.family.farecompare.data.foreground.ForegroundAppNameResolver
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
 * 3. When Uber specifically is in the foreground, hands its window content
 *    to [UiInspectorCoordinator] for the developer UI Inspector, which dumps
 *    the full node tree, re-dumps on every change, and exports the final
 *    tree once the UI stabilizes (Phase 7 - UI Inspector).
 *
 * It never clicks, types, performs gestures, or reads fares - it only reads
 * node metadata.
 */
@AndroidEntryPoint
class FareCompareAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var foregroundAppRepository: ForegroundAppRepository

    @Inject
    lateinit var foregroundAppNameResolver: ForegroundAppNameResolver

    @Inject
    lateinit var pickupDetectionCoordinator: PickupDetectionCoordinator

    @Inject
    lateinit var uiInspectorCoordinator: UiInspectorCoordinator

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
            maybeDetectPickupField(packageName)
            maybeDumpUberUiTree(packageName)
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

    private fun maybeDumpUberUiTree(packageName: String) {
        if (packageName != UBER_PACKAGE_NAME) return
        uiInspectorCoordinator.onRelevantUiEvent { rootInActiveWindow }
    }

    override fun onInterrupt() {
        // Interruption handling is implemented in a later automation phase.
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private companion object {
        const val TAG = "FareCompareAccessibility"
        const val UBER_PACKAGE_NAME = "com.ubercab"
        val KNOWN_RIDE_PROVIDERS = RideProvider.values().toList()
    }
}
