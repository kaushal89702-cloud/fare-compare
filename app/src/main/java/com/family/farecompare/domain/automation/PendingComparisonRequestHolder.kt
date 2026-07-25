package com.family.farecompare.domain.automation

import javax.inject.Inject
import javax.inject.Singleton

data class PendingComparisonRequest(val pickupAddress: String, val destinationAddress: String)

/**
 * Simple in-memory hand-off point for the pickup/destination the user typed
 * on the Home screen, read once by the Comparison screen right after
 * navigation. Deliberately not persisted - if the process is killed between
 * screens, the user just re-enters their trip, which is an acceptable and
 * simple failure mode for this internal navigation argument.
 */
@Singleton
class PendingComparisonRequestHolder @Inject constructor() {
    @Volatile
    var request: PendingComparisonRequest? = null
}
