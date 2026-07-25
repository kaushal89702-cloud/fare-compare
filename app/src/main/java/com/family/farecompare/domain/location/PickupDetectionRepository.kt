package com.family.farecompare.domain.location

import com.family.farecompare.domain.model.PickupDetectionResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the most recent pickup-field detection result. Updated by the
 * accessibility service, observed by the presentation layer.
 */
interface PickupDetectionRepository {
    val pickupDetectionResult: StateFlow<PickupDetectionResult?>
    fun updateResult(result: PickupDetectionResult?)
}
