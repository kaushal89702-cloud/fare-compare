package com.family.farecompare.domain.model

/**
 * Snapshot of the most recent pickup-field detection attempt, shown on the
 * developer card on the Home screen.
 */
data class PickupDetectionResult(
    val providerDisplayName: String,
    val detected: Boolean,
    val confidenceScore: Int,
    val currentValue: String?,
    val bounds: NodeBounds?,
    val verification: PickupVerificationStatus
)
