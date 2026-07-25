package com.family.farecompare.domain.automation

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Finds the nearest scrollable ancestor/descendant of the current window
 * and scrolls it forward, used as a retry strategy when a field or
 * suggestion cannot be located on the currently visible portion of the
 * screen (e.g. a long autocomplete suggestion list, or a field pushed below
 * the fold by a banner/promo).
 */
interface ScrollHelper {
    /** Returns true if a scrollable container was found and scrolled. */
    fun scrollForward(rootNode: AccessibilityNodeInfo): Boolean
}
