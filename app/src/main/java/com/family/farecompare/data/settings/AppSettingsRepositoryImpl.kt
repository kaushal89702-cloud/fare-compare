package com.family.farecompare.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.family.farecompare.domain.model.RideType
import com.family.farecompare.domain.settings.AppSettings
import com.family.farecompare.domain.settings.AppSettingsRepository
import com.family.farecompare.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val AUTO_LOCATION_ENABLED = booleanPreferencesKey("auto_location_enabled")
    val ACCESSIBILITY_AUTOMATION_ENABLED = booleanPreferencesKey("accessibility_automation_enabled")
    val DEFAULT_RIDE_TYPE = stringPreferencesKey("default_ride_type")
}

class AppSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppSettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            themeMode = preferences[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            autoLocationEnabled = preferences[Keys.AUTO_LOCATION_ENABLED] ?: true,
            accessibilityAutomationEnabled = preferences[Keys.ACCESSIBILITY_AUTOMATION_ENABLED] ?: true,
            defaultRideType = preferences[Keys.DEFAULT_RIDE_TYPE]?.let { runCatching { RideType.valueOf(it) }.getOrNull() }
                ?: RideType.UNKNOWN
        )
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    override suspend fun setAutoLocationEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_LOCATION_ENABLED] = enabled }
    }

    override suspend fun setAccessibilityAutomationEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ACCESSIBILITY_AUTOMATION_ENABLED] = enabled }
    }

    override suspend fun setDefaultRideType(rideType: RideType) {
        dataStore.edit { it[Keys.DEFAULT_RIDE_TYPE] = rideType.name }
    }
}
