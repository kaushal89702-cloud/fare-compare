package com.family.farecompare.domain.automation

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Implemented by the live [com.family.farecompare.service.FareCompareAccessibilityService]
 * instance so domain-layer automation code can read the current window's
 * node tree without depending on the Android Service class directly.
 */
interface AccessibilityGateway {
    fun currentRootNode(): AccessibilityNodeInfo?
}
