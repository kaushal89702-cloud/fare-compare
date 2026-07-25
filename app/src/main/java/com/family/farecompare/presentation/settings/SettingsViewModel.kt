package com.family.farecompare.presentation.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.farecompare.domain.history.SearchHistoryRepository
import com.family.farecompare.domain.model.RideType
import com.family.farecompare.domain.settings.AppSettingsRepository
import com.family.farecompare.domain.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    val settings: StateFlow<com.family.farecompare.domain.settings.AppSettings> = appSettingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.family.farecompare.domain.settings.AppSettings())

    private val _historyCleared = MutableStateFlow(false)
    val historyCleared: StateFlow<Boolean> = _historyCleared

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun onThemeModeSelected(themeMode: ThemeMode) {
        viewModelScope.launch { appSettingsRepository.setThemeMode(themeMode) }
    }

    fun onAutoLocationToggled(enabled: Boolean) {
        viewModelScope.launch { appSettingsRepository.setAutoLocationEnabled(enabled) }
    }

    fun onAccessibilityAutomationToggled(enabled: Boolean) {
        viewModelScope.launch { appSettingsRepository.setAccessibilityAutomationEnabled(enabled) }
    }

    fun onDefaultRideTypeSelected(rideType: RideType) {
        viewModelScope.launch { appSettingsRepository.setDefaultRideType(rideType) }
    }

    fun onClearHistoryClicked() {
        viewModelScope.launch {
            searchHistoryRepository.clearHistory()
            _historyCleared.value = true
        }
    }
}
