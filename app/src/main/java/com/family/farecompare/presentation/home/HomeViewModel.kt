package com.family.farecompare.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.farecompare.domain.accessibility.AccessibilityStatusChecker
import com.family.farecompare.domain.automation.PendingComparisonRequest
import com.family.farecompare.domain.automation.PendingComparisonRequestHolder
import com.family.farecompare.domain.foreground.ForegroundAppRepository
import com.family.farecompare.domain.history.SearchHistoryRepository
import com.family.farecompare.domain.location.CurrentLocationProvider
import com.family.farecompare.domain.location.PickupDetectionRepository
import com.family.farecompare.domain.places.PlacesAutocompleteRepository
import com.family.farecompare.domain.settings.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val AUTOCOMPLETE_DEBOUNCE_MS = 300L

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accessibilityStatusChecker: AccessibilityStatusChecker,
    private val currentLocationProvider: CurrentLocationProvider,
    private val placesAutocompleteRepository: PlacesAutocompleteRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val pendingComparisonRequestHolder: PendingComparisonRequestHolder,
    foregroundAppRepository: ForegroundAppRepository,
    pickupDetectionRepository: PickupDetectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isPlacesAvailable = placesAutocompleteRepository.isAvailable))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private var autocompleteJob: Job? = null

    init {
        refreshAccessibilityStatus()

        foregroundAppRepository.currentForegroundApp
            .onEach { app -> _uiState.update { it.copy(currentForegroundApp = app) } }
            .launchIn(viewModelScope)

        pickupDetectionRepository.pickupDetectionResult
            .onEach { result -> _uiState.update { it.copy(pickupDetectionResult = result) } }
            .launchIn(viewModelScope)

        searchHistoryRepository.observeRecent()
            .onEach { recent -> _uiState.update { it.copy(recentSearches = recent) } }
            .launchIn(viewModelScope)

        maybeAutoFillCurrentLocation()
    }

    fun refreshAccessibilityStatus() {
        _uiState.update {
            it.copy(isAccessibilityEnabled = accessibilityStatusChecker.isServiceEnabled())
        }
    }

    fun onCurrentLocationClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingCurrentLocation = true) }
            val address = currentLocationProvider.getCurrentAddress()
            if (address != null) {
                _uiState.update { it.copy(pickup = address, isResolvingCurrentLocation = false) }
            } else {
                _uiState.update { it.copy(isResolvingCurrentLocation = false) }
                _snackbarEvents.emit("Current location unavailable. Enter pickup manually.")
            }
        }
    }

    fun onPickupChanged(value: String) {
        _uiState.update { it.copy(pickup = value, activeAddressField = ActiveAddressField.PICKUP) }
        requestAutocomplete(value)
    }

    fun onDestinationChanged(value: String) {
        _uiState.update { it.copy(destination = value, activeAddressField = ActiveAddressField.DESTINATION) }
        requestAutocomplete(value)
    }

    fun onSuggestionSelected(suggestion: com.family.farecompare.domain.places.PlaceSuggestion) {
        viewModelScope.launch {
            val resolved = placesAutocompleteRepository.resolvePlace(suggestion.placeId)
            val fullAddress = resolved?.formattedAddress ?: "${suggestion.primaryText}, ${suggestion.secondaryText}"
            when (_uiState.value.activeAddressField) {
                ActiveAddressField.PICKUP -> _uiState.update { it.copy(pickup = fullAddress) }
                ActiveAddressField.DESTINATION -> _uiState.update { it.copy(destination = fullAddress) }
                ActiveAddressField.NONE -> Unit
            }
            _uiState.update { it.copy(addressSuggestions = emptyList(), activeAddressField = ActiveAddressField.NONE) }
        }
    }

    fun onRecentSearchSelected(recentSearch: com.family.farecompare.domain.model.RecentSearch) {
        _uiState.update {
            it.copy(pickup = recentSearch.pickupAddress, destination = recentSearch.destinationAddress)
        }
    }

    fun onPickupFocusLost() {
        _uiState.update { it.copy(isPickupTouched = true) }
        clearSuggestionsIfActive(ActiveAddressField.PICKUP)
    }

    fun onDestinationFocusLost() {
        _uiState.update { it.copy(isDestinationTouched = true) }
        clearSuggestionsIfActive(ActiveAddressField.DESTINATION)
    }

    /**
     * Stores the current pickup/destination for the Comparison screen to
     * pick up, records the search in history, and signals the caller
     * (Home screen) to navigate. The actual multi-provider automation runs
     * entirely on the Comparison screen so it can show live per-provider
     * progress without being tied to Home's lifecycle.
     */
    fun onCompareClicked(onReadyToNavigate: () -> Unit) {
        val state = _uiState.value
        if (!state.isCompareEnabled) return

        Log.d(TAG, "Compare pressed: pickup=${state.pickup}, destination=${state.destination}")

        if (!accessibilityStatusChecker.isServiceEnabled()) {
            viewModelScope.launch { _snackbarEvents.emit("Accessibility Service Required") }
            return
        }

        pendingComparisonRequestHolder.request = PendingComparisonRequest(state.pickup, state.destination)
        viewModelScope.launch { searchHistoryRepository.recordSearch(state.pickup, state.destination) }
        onReadyToNavigate()
    }

    private fun clearSuggestionsIfActive(field: ActiveAddressField) {
        if (_uiState.value.activeAddressField == field) {
            _uiState.update { it.copy(addressSuggestions = emptyList(), activeAddressField = ActiveAddressField.NONE) }
        }
    }

    private fun requestAutocomplete(query: String) {
        autocompleteJob?.cancel()
        if (!placesAutocompleteRepository.isAvailable || query.length < MIN_AUTOCOMPLETE_QUERY_LENGTH) {
            _uiState.update { it.copy(addressSuggestions = emptyList()) }
            return
        }
        autocompleteJob = viewModelScope.launch {
            delay(AUTOCOMPLETE_DEBOUNCE_MS)
            val suggestions = placesAutocompleteRepository.autocomplete(query)
            _uiState.update { it.copy(addressSuggestions = suggestions) }
        }
    }

    private fun maybeAutoFillCurrentLocation() {
        viewModelScope.launch {
            val autoLocationEnabled = appSettingsRepository.settings.first().autoLocationEnabled
            if (!autoLocationEnabled) return@launch
            val address = currentLocationProvider.getCurrentAddress() ?: return@launch
            if (_uiState.value.pickup.isBlank()) {
                _uiState.update { it.copy(pickup = address) }
            }
        }
    }

    private companion object {
        const val TAG = "HomeViewModel"
        const val MIN_AUTOCOMPLETE_QUERY_LENGTH = 3
    }
}
