package com.family.farecompare.data.fare

import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.fare.NoRidesAvailableDetector
import javax.inject.Inject

class NoRidesAvailableDetectorImpl @Inject constructor() : NoRidesAvailableDetector {

    override fun isNoRidesAvailableScreen(rootNode: AccessibilityNodeInfo): Boolean {
        return containsNoRidesText(rootNode, depth = 0)
    }

    private fun containsNoRidesText(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > MAX_TRAVERSAL_DEPTH) return false

        val text = (node.text?.toString().orEmpty() + " " + node.contentDescription?.toString().orEmpty()).lowercase()
        if (NO_RIDES_KEYWORDS.any { text.contains(it) }) return true

        for (i in 0 until node.childCount) {
            if (containsNoRidesText(node.getChild(i), depth + 1)) return true
        }
        return false
    }

    private companion object {
        const val MAX_TRAVERSAL_DEPTH = 100
        val NO_RIDES_KEYWORDS = listOf(
            "no rides available",
            "no cabs available",
            "no vehicles available",
            "not available in your area",
            "no drivers available",
            "sorry, no rides"
        )
    }
}
