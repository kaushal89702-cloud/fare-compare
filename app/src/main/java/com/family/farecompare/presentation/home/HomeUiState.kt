package com.family.farecompare.presentation.home

import com.family.farecompare.domain.model.ForegroundApp
import com.family.farecompare.domain.model.PickupDetectionResult
import com.family.farecompare.domain.model.RecentSearch
import com.family.farecompare.domain.places.PlaceSuggestion

enum class ActiveAddressField { NONE, PICKUP, DESTINATION }

data class HomeUiState(
    val pickup: String = "",
    val destination: String = "",
    val isPickupTouched: Boolean = false,
    val isDestinationTouched: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val currentForegroundApp: ForegroundApp? = null,
    val pickupDetectionResult: PickupDetectionResult? = null,
    val recentSearches: List<RecentSearch> = emptyList(),
    val activeAddressField: ActiveAddressField = ActiveAddressField.NONE,
    val addressSuggestions: List<PlaceSuggestion> = emptyList(),
    val isPlacesAvailable: Boolean = false,
    val isResolvingCurrentLocation: Boolean = false
) {
    val isPickupError: Boolean get() = isPickupTouched && pickup.isBlank()
    val isDestinationError: Boolean get() = isDestinationTouched && destination.isBlank()
    val isCompareEnabled: Boolean get() = pickup.isNotBlank() && destination.isNotBlank()
}
