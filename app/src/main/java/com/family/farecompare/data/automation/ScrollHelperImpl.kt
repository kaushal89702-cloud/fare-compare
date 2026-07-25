package com.family.farecompare.data.automation

import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.automation.ScrollHelper
import javax.inject.Inject

class ScrollHelperImpl @Inject constructor() : ScrollHelper {

    override fun scrollForward(rootNode: AccessibilityNodeInfo): Boolean {
        val scrollable = findScrollableNode(rootNode, depth = 0) ?: return false
        return scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo?, depth: Int): AccessibilityNodeInfo? {
        if (node == null || depth > MAX_TRAVERSAL_DEPTH) return null
        if (node.isVisibleToUser && node.isScrollable) return node
        for (i in 0 until node.childCount) {
            findScrollableNode(node.getChild(i), depth + 1)?.let { return it }
        }
        return null
    }

    private companion object {
        const val MAX_TRAVERSAL_DEPTH = 100
    }
}
