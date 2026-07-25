package com.family.farecompare.domain.model

/**
 * A single normalized fare reading extracted from a ride provider's app.
 *
 * [fareAmount] is always the lower bound of any displayed range, with
 * currency symbols and thousands separators stripped, so quotes from
 * different providers can be compared numerically. [rawFareText] preserves
 * exactly what was read from screen (e.g. "₹120-150", "₹99 (20% off)") for
 * display and debugging.
 */
data class FareQuote(
    val provider: RideProvider,
    val rideType: RideType,
    val fareAmount: Double,
    val rawFareText: String,
    val etaMinutes: Int?,
    val timestampMillis: Long
)
