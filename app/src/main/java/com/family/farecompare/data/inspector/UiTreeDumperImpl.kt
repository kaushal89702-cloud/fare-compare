package com.family.farecompare.data.inspector

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.inspector.UiTreeDumper
import com.family.farecompare.domain.model.NodeBounds
import com.family.farecompare.domain.model.UiNodeSnapshot
import javax.inject.Inject

/**
 * Traverses every node of an [AccessibilityNodeInfo] tree, read-only.
 * Individual nodes can become stale mid-traversal while the source app's UI
 * is still loading; any such node is captured as a placeholder rather than
 * allowed to crash the dump.
 */
class UiTreeDumperImpl @Inject constructor() : UiTreeDumper {

    override fun dump(rootNode: AccessibilityNodeInfo): UiNodeSnapshot = dumpNode(rootNode, depth = 0)

    private fun dumpNode(node: AccessibilityNodeInfo, depth: Int): UiNodeSnapshot {
        return try {
            if (depth > MAX_TRAVERSAL_DEPTH) return staleNodePlaceholder("max depth reached")

            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val children = (0 until node.childCount).mapNotNull { index ->
                node.getChild(index)?.let { child -> dumpNode(child, depth + 1) }
            }

            UiNodeSnapshot(
                className = node.className?.toString() ?: "Unknown",
                resourceId = node.viewIdResourceName,
                text = node.text?.toString(),
                hintText = node.hintText?.toString(),
                contentDescription = node.contentDescription?.toString(),
                bounds = NodeBounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
                clickable = node.isClickable,
                editable = node.isEditable,
                enabled = node.isEnabled,
                visible = node.isVisibleToUser,
                childCount = node.childCount,
                children = children
            )
        } catch (exception: Exception) {
            Log.w(TAG, "Skipped a stale node while dumping: ${exception.message}")
            staleNodePlaceholder(exception.message ?: "unknown error")
        }
    }

    private fun staleNodePlaceholder(reason: String): UiNodeSnapshot = UiNodeSnapshot(
        className = "Unavailable",
        resourceId = null,
        text = null,
        hintText = null,
        contentDescription = "Node became unavailable: $reason",
        bounds = NodeBounds(0, 0, 0, 0),
        clickable = false,
        editable = false,
        enabled = false,
        visible = false,
        childCount = 0,
        children = emptyList()
    )

    private companion object {
        const val TAG = "UiTreeDumper"
        const val MAX_TRAVERSAL_DEPTH = 100
    }
}
