package com.family.farecompare.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.farecompare.R
import com.family.farecompare.domain.accessibility.AccessibilityStatusChecker
import com.family.farecompare.domain.automation.AutomationStep
import com.family.farecompare.domain.automation.LaunchRideAppUseCase
import com.family.farecompare.domain.common.ResourceProvider
import com.family.farecompare.domain.foreground.ForegroundAppRepository
import com.family.farecompare.domain.model.RideProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accessibilityStatusChecker: AccessibilityStatusChecker,
    private val launchRideAppUseCase: LaunchRideAppUseCase,
    private val resourceProvider: ResourceProvider,
    foregroundAppRepository: ForegroundAppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    init {
        refreshAccessibilityStatus()
        _uiState.update {
            it.copy(automationStatusText = resourceProvider.getString(R.string.automation_status_idle))
        }
        foregroundAppRepository.currentForegroundApp
            .onEach { app -> _uiState.update { it.copy(currentForegroundApp = app) } }
            .launchIn(viewModelScope)
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
        Log.d(TAG, "Compare button pressed")

        if (!accessibilityStatusChecker.isServiceEnabled()) {
            Log.w(TAG, "Accessibility Service Required")
            viewModelScope.launch {
                _snackbarEvents.emit(resourceProvider.getString(R.string.message_accessibility_required))
            }
            return
        }

        viewModelScope.launch {
            launchRideAppUseCase(RideProvider.UBER).collect { step -> handleAutomationStep(step) }
        }
    }

    private suspend fun handleAutomationStep(step: AutomationStep) {
        val provider = RideProvider.UBER
        when (step) {
            AutomationStep.NotInstalled -> {
                Log.w(TAG, "${provider.displayName} is not installed")
                _uiState.update {
                    it.copy(automationStatusText = resourceProvider.getString(R.string.automation_status_idle))
                }
                _snackbarEvents.emit(
                    resourceProvider.getString(R.string.message_app_not_installed, provider.displayName)
                )
            }

            AutomationStep.Launching -> {
                Log.d(TAG, "Launching ${provider.displayName}")
                _uiState.update {
                    it.copy(
                        automationStatusText = resourceProvider.getString(
                            R.string.automation_status_launching,
                            provider.displayName
                        )
                    )
                }
            }

            AutomationStep.Waiting -> {
                Log.d(TAG, "Waiting for ${provider.displayName}")
                _uiState.update {
                    it.copy(
                        automationStatusText = resourceProvider.getString(
                            R.string.automation_status_waiting,
                            provider.displayName
                        )
                    )
                }
            }

            AutomationStep.Success -> {
                Log.d(TAG, "${provider.displayName} opened")
                _uiState.update {
                    it.copy(
                        automationStatusText = resourceProvider.getString(
                            R.string.automation_status_success,
                            provider.displayName
                        )
                    )
                }
            }

            AutomationStep.Timeout -> {
                Log.w(TAG, "Launch timeout for ${provider.displayName}")
                _uiState.update {
                    it.copy(automationStatusText = resourceProvider.getString(R.string.automation_status_failed))
                }
                _snackbarEvents.emit(
                    resourceProvider.getString(R.string.message_unable_to_launch, provider.displayName)
                )
            }
        }
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
