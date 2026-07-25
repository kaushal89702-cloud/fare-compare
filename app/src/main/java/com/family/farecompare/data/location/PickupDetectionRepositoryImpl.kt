package com.family.farecompare.data.location

import com.family.farecompare.domain.location.PickupDetectionRepository
import com.family.farecompare.domain.model.PickupDetectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PickupDetectionRepositoryImpl @Inject constructor() : PickupDetectionRepository {

    private val _pickupDetectionResult = MutableStateFlow<PickupDetectionResult?>(null)
    override val pickupDetectionResult: StateFlow<PickupDetectionResult?> = _pickupDetectionResult.asStateFlow()

    override fun updateResult(result: PickupDetectionResult?) {
        _pickupDetectionResult.value = result
    }
}
