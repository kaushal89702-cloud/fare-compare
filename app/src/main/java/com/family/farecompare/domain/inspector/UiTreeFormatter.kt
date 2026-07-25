package com.family.farecompare.domain.inspector

import com.family.farecompare.domain.model.UiNodeSnapshot

/**
 * Formats a [UiNodeSnapshot] tree into a human-readable, indented text
 * dump suitable for on-screen display or file export. Pure formatting
 * logic with no Android framework dependency, so it is easy to reason
 * about and test independently of the accessibility service.
 */
object UiTreeFormatter {

    fun format(root: UiNodeSnapshot): String {
        val lines = mutableListOf<String>()
        appendNode(root, depth = 0, lines)
        return lines.joinToString(separator = "\n")
    }

    fun countNodes(root: UiNodeSnapshot): Int = 1 + root.children.sumOf { countNodes(it) }

    private fun appendNode(node: UiNodeSnapshot, depth: Int, lines: MutableList<String>) {
        lines += buildString {
            append("  ".repeat(depth))
            append("- Class: ").append(node.className)
            append(" | ResourceId: ").append(node.resourceId ?: "-")
            append(" | Text: '").append(node.text.orEmpty()).append("'")
            append(" | Hint: '").append(node.hintText.orEmpty()).append("'")
            append(" | ContentDesc: '").append(node.contentDescription.orEmpty()).append("'")
            append(" | Bounds: (").append(node.bounds.left).append(",").append(node.bounds.top)
            append(")-(").append(node.bounds.right).append(",").append(node.bounds.bottom).append(")")
            append(" | Clickable: ").append(node.clickable)
            append(" | Editable: ").append(node.editable)
            append(" | Enabled: ").append(node.enabled)
            append(" | Visible: ").append(node.visible)
            append(" | ChildCount: ").append(node.childCount)
        }
        node.children.forEach { appendNode(it, depth + 1, lines) }
    }
}
