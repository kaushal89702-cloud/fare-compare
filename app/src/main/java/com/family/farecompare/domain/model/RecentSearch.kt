package com.family.farecompare.domain.model

/**
 * A previously used pickup/destination pair, most-recent first.
 */
data class RecentSearch(
    val id: Long,
    val pickupAddress: String,
    val destinationAddress: String,
    val timestampMillis: Long
)
