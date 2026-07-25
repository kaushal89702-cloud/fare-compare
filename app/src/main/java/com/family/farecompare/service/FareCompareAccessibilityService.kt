package com.family.farecompare.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service that will drive on-device automation of ride-hailing
 * apps in later development phases. At this stage it only needs to exist and
 * be enable-able so its running status can be detected by the app.
 */
class FareCompareAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event handling is implemented in a later automation phase.
    }

    override fun onInterrupt() {
        // Interruption handling is implemented in a later automation phase.
    }
}
