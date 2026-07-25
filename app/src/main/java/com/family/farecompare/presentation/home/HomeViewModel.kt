package com.family.farecompare.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.farecompare.R
import com.family.farecompare.domain.accessibility.AccessibilityStatusChecker
import com.family.farecompare.domain.automation.AutomationStep
import com.family.farecompare.domain.automation.LaunchRideAppUseCase
import com.family.farecompare.domain.automation.RideAppProvider
import com.family.farecompare.domain.common.ResourceProvider
import com.family.farecompare.domain.foreground.ForegroundAppRepository
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
    rideAppProviders: Set<@JvmSuppressWildcards RideAppProvider>,
    foregroundAppRepository: ForegroundAppRepository
) : ViewModel() {

    // Phase 6 supports Uber only. Ola/Rapido will be added to
    // AutomationModule in later phases without touching this ViewModel.
    private val uberProvider: RideAppProvider = rideAppProviders.first { it.packageName == "com.ubercab" }

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
        Log.d(TAG, "Compare pressed")
        viewModelScope.launch {
            Log.d(TAG, "Checking provider: ${uberProvider.displayName}")
            launchRideAppUseCase(uberProvider).collect { step -> handleAutomationStep(step) }
        }
    }

    private suspend fun handleAutomationStep(step: AutomationStep) {
        val providerName = uberProvider.displayName
        when (step) {
            AutomationStep.CheckingAccessibility -> {
                _uiState.update {
                    it.copy(automationStatusText = resourceProvider.getString(R.string.automation_status_checking_accessibility))
                }
            }

            AutomationStep.AccessibilityDisabled -> {
                Log.w(TAG, "Accessibility Service Required")
                refreshAccessibilityStatus()
                _uiState.update {
                    it.copy(automationStatusText = resourceProvider.getString(R.string.automation_status_idle))
                }
                _snackbarEvents.emit(resourceProvider.getString(R.string.message_accessibility_required))
            }

            AutomationStep.CheckingInstalled -> {
                _uiState.update {
                    it.copy(
                        automationStatusText = resourceProvider.getString(
                            R.string.automation_status_checking_installed,
                            providerName
                        )
                    )
                }
            }

            AutomationStep.NotInstalled -> {
                Log.w(TAG, "$providerName is not installed")
                _uiState.update {
                    it.copy(automationStatusText = resourceProvider.getString(R.string.automation_status_idle))
                }
                _snackbarEvents.emit(
                    resourceProvider.getString(R.string.message_app_not_installed, providerName)
                )
            }

            AutomationStep.Launching -> {
                Log.d(TAG, "Launching $providerName")
                _uiState.update {
                    it.copy(
                        automationStatusText = resourceProvider.getString(
                            R.string.automation_status_launching,
                            providerName
                        )
                    )
                }
            }

            AutomationStep.Waiting -> {
                Log.d(TAG, "Waiting for $providerName (foreground events)")
                _uiState.update {
                    it.copy(
                        automationStatusText = resourceProvider.getString(
                            R.string.automation_status_waiting,
                            providerName
                        )
                    )
                }
            }

            AutomationStep.Success -> {
                Log.d(TAG, "Foreground changed: $providerName opened")
                _uiState.update {
                    it.copy(
                        automationStatusText = resourceProvider.getString(
                            R.string.automation_status_success,
                            providerName
                        )
                    )
                }
            }

            AutomationStep.Timeout -> {
                Log.w(TAG, "Timeout waiting for $providerName")
                _uiState.update {
                    it.copy(automationStatusText = resourceProvider.getString(R.string.automation_status_failed))
                }
                _snackbarEvents.emit(
                    resourceProvider.getString(R.string.message_unable_to_launch, providerName)
                )
            }

            is AutomationStep.UnexpectedError -> {
                Log.e(TAG, "Unexpected error launching $providerName: ${step.message}")
                _uiState.update {
                    it.copy(automationStatusText = resourceProvider.getString(R.string.automation_status_failed))
                }
                _snackbarEvents.emit(
                    resourceProvider.getString(R.string.message_unable_to_launch, providerName)
                )
            }
        }
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
