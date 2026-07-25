package com.family.farecompare.data.automation

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.automation.AccessibilityActionExecutor
import javax.inject.Inject

class AccessibilityActionExecutorImpl @Inject constructor() : AccessibilityActionExecutor {

    override fun setText(node: AccessibilityNodeInfo, text: String): Boolean {
        if (!node.isEditable) {
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        }
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    override fun click(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        // Some suggestion rows mark only an ancestor as clickable while the
        // text itself lives on a child label - walk up to find it.
        var current: AccessibilityNodeInfo? = node.parent
        var hops = 0
        while (current != null && hops < MAX_CLICKABLE_ANCESTOR_HOPS) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
            hops++
        }
        return false
    }

    private companion object {
        const val MAX_CLICKABLE_ANCESTOR_HOPS = 6
    }
}
