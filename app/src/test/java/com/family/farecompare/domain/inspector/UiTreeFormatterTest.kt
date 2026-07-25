package com.family.farecompare.domain.inspector

import com.family.farecompare.domain.model.NodeBounds
import com.family.farecompare.domain.model.UiNodeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiTreeFormatterTest {

    private fun leafNode(text: String) = UiNodeSnapshot(
        className = "android.widget.TextView",
        resourceId = "com.ubercab:id/label",
        text = text,
        hintText = null,
        contentDescription = null,
        bounds = NodeBounds(0, 0, 100, 40),
        clickable = false,
        editable = false,
        enabled = true,
        visible = true,
        childCount = 0,
        children = emptyList()
    )

    @Test
    fun `countNodes counts root plus all descendants`() {
        val root = leafNode("root").copy(children = listOf(leafNode("child1"), leafNode("child2")))
        assertEquals(3, UiTreeFormatter.countNodes(root))
    }

    @Test
    fun `countNodes returns 1 for a single leaf node`() {
        assertEquals(1, UiTreeFormatter.countNodes(leafNode("leaf")))
    }

    @Test
    fun `format includes every required field for a node`() {
        val output = UiTreeFormatter.format(leafNode("Pickup location"))

        assertTrue(output.contains("Class: android.widget.TextView"))
        assertTrue(output.contains("ResourceId: com.ubercab:id/label"))
        assertTrue(output.contains("Text: 'Pickup location'"))
        assertTrue(output.contains("Clickable: false"))
        assertTrue(output.contains("Editable: false"))
        assertTrue(output.contains("Enabled: true"))
        assertTrue(output.contains("Visible: true"))
        assertTrue(output.contains("ChildCount: 0"))
    }

    @Test
    fun `format indents children deeper than their parent`() {
        val child = leafNode("child")
        val root = leafNode("root").copy(childCount = 1, children = listOf(child))

        val lines = UiTreeFormatter.format(root).split("\n")

        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("- Class:"))
        assertTrue(lines[1].startsWith("  - Class:"))
    }
}
