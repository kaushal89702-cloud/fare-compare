package com.family.farecompare.domain.model

/**
 * A single node captured from an [android.view.accessibility.AccessibilityNodeInfo]
 * tree, along with its children. Used purely for developer diagnostics (the
 * UI Inspector) - never for automation.
 */
data class UiNodeSnapshot(
    val className: String,
    val resourceId: String?,
    val text: String?,
    val hintText: String?,
    val contentDescription: String?,
    val bounds: NodeBounds,
    val clickable: Boolean,
    val editable: Boolean,
    val enabled: Boolean,
    val visible: Boolean,
    val childCount: Int,
    val children: List<UiNodeSnapshot>
)
