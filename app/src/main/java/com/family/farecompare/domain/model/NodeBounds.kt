package com.family.farecompare.domain.model

/**
 * Screen-space bounding box of a detected accessibility node, decoupled
 * from Android's [android.graphics.Rect] so the domain layer stays a plain
 * data holder.
 */
data class NodeBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)
