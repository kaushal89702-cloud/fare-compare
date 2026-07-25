package com.family.farecompare.presentation.home

import com.family.farecompare.domain.model.FareResult
import com.family.farecompare.domain.model.RideProvider

data class HomeUiState(
    val pickup: String = "",
    val destination: String = "",
    val isPickupTouched: Boolean = false,
    val isDestinationTouched: Boolean = false,
    val fareResults: List<FareResult> = RideProvider.values().map { FareResult(provider = it) }
) {
    val isPickupError: Boolean get() = isPickupTouched && pickup.isBlank()
    val isDestinationError: Boolean get() = isDestinationTouched && destination.isBlank()
    val isCompareEnabled: Boolean get() = pickup.isNotBlank() && destination.isNotBlank()
}
