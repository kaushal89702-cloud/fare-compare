package com.family.farecompare.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.family.farecompare.data.foreground.ForegroundAppNameResolver
import com.family.farecompare.domain.foreground.ForegroundAppRepository
import com.family.farecompare.domain.model.ForegroundApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Accessibility service that detects the foreground application via window
 * state change events and reports it to [ForegroundAppRepository]. It does
 * not read screen content, click, or automate anything in this phase.
 */
@AndroidEntryPoint
class FareCompareAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var foregroundAppRepository: ForegroundAppRepository

    @Inject
    lateinit var foregroundAppNameResolver: ForegroundAppNameResolver

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == foregroundAppRepository.currentForegroundApp.value?.packageName) return

        val displayName = foregroundAppNameResolver.resolveDisplayName(packageName)
        foregroundAppRepository.updateForegroundApp(
            ForegroundApp(packageName = packageName, displayName = displayName)
        )
        Log.d(TAG, "Foreground App: $displayName")
    }

    override fun onInterrupt() {
        // Interruption handling is implemented in a later automation phase.
    }

    private companion object {
        const val TAG = "FareCompareAccessibility"
    }
}
