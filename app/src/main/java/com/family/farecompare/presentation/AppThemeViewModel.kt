package com.family.farecompare.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.farecompare.domain.settings.AppSettingsRepository
import com.family.farecompare.domain.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppThemeViewModel @Inject constructor(
    appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = appSettingsRepository.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
}
