package com.family.farecompare.domain.fare

import com.family.farecompare.domain.model.NodeBounds
import com.family.farecompare.domain.model.RideType

/**
 * A single fare candidate found on screen, before it is wrapped into a
 * [com.family.farecompare.domain.model.FareQuote] with provider/timestamp
 * context by the caller.
 */
data class DetectedFare(
    val rideType: RideType,
    val amount: Double,
    val rawText: String,
    val etaMinutes: Int?,
    val bounds: NodeBounds,
    val confidenceScore: Int
)
