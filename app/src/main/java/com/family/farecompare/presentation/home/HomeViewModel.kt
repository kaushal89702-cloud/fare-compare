package com.family.farecompare.presentation.home

import androidx.lifecycle.ViewModel
import com.family.farecompare.domain.accessibility.AccessibilityStatusChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accessibilityStatusChecker: AccessibilityStatusChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshAccessibilityStatus()
    }

    fun refreshAccessibilityStatus() {
        _uiState.update {
            it.copy(isAccessibilityEnabled = accessibilityStatusChecker.isServiceEnabled())
        }
    }

    fun onPickupChanged(value: String) {
        _uiState.update { it.copy(pickup = value) }
    }

    fun onDestinationChanged(value: String) {
        _uiState.update { it.copy(destination = value) }
    }

    fun onPickupFocusLost() {
        _uiState.update { it.copy(isPickupTouched = true) }
    }

    fun onDestinationFocusLost() {
        _uiState.update { it.copy(isDestinationTouched = true) }
    }

    fun onCompareClicked() {
        // Implemented in a later phase (automation). Input validity is already
        // enforced by disabling the button, so this is only reachable when valid.
    }
}
