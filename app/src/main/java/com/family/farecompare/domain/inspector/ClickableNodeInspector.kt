package com.family.farecompare.domain.inspector

import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.model.UiNodeSnapshot

/**
 * Accessibility Inspector mode that outlines every *clickable* node only
 * (as opposed to [UiTreeDumper]/[UiTreeFormatter], which dump the entire
 * tree). Used to quickly see every tappable element - buttons, list rows,
 * placeholders - on the current screen, which is what actually matters
 * when diagnosing why an automation click/tap step failed. For each
 * clickable node it reports text, hint, content description, resource ID,
 * class name, and bounds.
 */
interface ClickableNodeInspector {
    fun inspect(rootNode: AccessibilityNodeInfo): String
}

/**
 * Pure formatting logic for [ClickableNodeInspector], separated out so it
 * can be unit tested on the JVM without any Android framework dependency.
 */
object ClickableNodeFormatter {

    fun findClickableNodes(root: UiNodeSnapshot): List<UiNodeSnapshot> {
        val results = mutableListOf<UiNodeSnapshot>()
        collect(root, results)
        return results
    }

    private fun collect(node: UiNodeSnapshot, out: MutableList<UiNodeSnapshot>) {
        if (node.clickable) out.add(node)
        node.children.forEach { collect(it, out) }
    }

    fun format(clickableNodes: List<UiNodeSnapshot>): String = clickableNodes.joinToString(separator = "\n") { node ->
        buildString {
            append("- Class: ").append(node.className)
            append(" | ResourceId: ").append(node.resourceId ?: "-")
            append(" | Text: '").append(node.text.orEmpty()).append("'")
            append(" | Hint: '").append(node.hintText.orEmpty()).append("'")
            append(" | ContentDesc: '").append(node.contentDescription.orEmpty()).append("'")
            append(" | Bounds: (").append(node.bounds.left).append(",").append(node.bounds.top)
            append(")-(").append(node.bounds.right).append(",").append(node.bounds.bottom).append(")")
        }
    }
}
