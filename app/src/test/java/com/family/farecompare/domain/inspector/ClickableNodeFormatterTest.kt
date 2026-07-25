package com.family.farecompare.domain.inspector

import com.family.farecompare.domain.model.NodeBounds
import com.family.farecompare.domain.model.UiNodeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickableNodeFormatterTest {

    private fun node(text: String, clickable: Boolean, children: List<UiNodeSnapshot> = emptyList()) = UiNodeSnapshot(
        className = "android.widget.Button",
        resourceId = "com.rapido.passenger:id/button",
        text = text,
        hintText = null,
        contentDescription = null,
        bounds = NodeBounds(0, 0, 50, 50),
        clickable = clickable,
        editable = false,
        enabled = true,
        visible = true,
        childCount = children.size,
        children = children
    )

    @Test
    fun `findClickableNodes returns only clickable nodes from the tree`() {
        val root = node(
            "root",
            clickable = false,
            children = listOf(
                node("Confirm pickup", clickable = true),
                node("label only", clickable = false),
                node("Confirm destination", clickable = true)
            )
        )

        val clickable = ClickableNodeFormatter.findClickableNodes(root)

        assertEquals(2, clickable.size)
        assertTrue(clickable.all { it.clickable })
        assertEquals(setOf("Confirm pickup", "Confirm destination"), clickable.map { it.text }.toSet())
    }

    @Test
    fun `findClickableNodes returns empty list when nothing is clickable`() {
        val root = node("root", clickable = false, children = listOf(node("child", clickable = false)))
        assertEquals(emptyList<UiNodeSnapshot>(), ClickableNodeFormatter.findClickableNodes(root))
    }

    @Test
    fun `format includes text, resourceId, and bounds for each clickable node`() {
        val clickableNode = node("Book Now", clickable = true)
        val output = ClickableNodeFormatter.format(listOf(clickableNode))

        assertTrue(output.contains("Text: 'Book Now'"))
        assertTrue(output.contains("ResourceId: com.rapido.passenger:id/button"))
        assertTrue(output.contains("Bounds: (0,0)-(50,50)"))
    }
}
