package com.family.farecompare.domain.location

import com.family.farecompare.domain.model.NodeBounds

/**
 * A single detected candidate field, already scored and normalized to a
 * 0-100 confidence percentage.
 */
data class DetectedField(
    val value: String,
    val confidenceScore: Int,
    val bounds: NodeBounds,
    val resourceId: String?
)
