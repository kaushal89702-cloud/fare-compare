package com.family.farecompare.domain.automation

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Performs the two low-level, generic accessibility actions the automation
 * engine needs on any provider's UI: setting text into an editable field,
 * and clicking a node (used both for the field itself and for tapping an
 * address suggestion from the resulting list). Implementations use only
 * standard [AccessibilityNodeInfo] actions (ACTION_SET_TEXT / ACTION_CLICK /
 * ACTION_FOCUS) - no gestures, no synthetic touch events, and nothing that
 * bypasses the app's own input handling.
 */
interface AccessibilityActionExecutor {
    fun setText(node: AccessibilityNodeInfo, text: String): Boolean
    fun click(node: AccessibilityNodeInfo): Boolean
}
