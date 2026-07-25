package com.family.farecompare.domain.settings

import com.family.farecompare.domain.model.RideType
import kotlinx.coroutines.flow.Flow

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoLocationEnabled: Boolean = true,
    val accessibilityAutomationEnabled: Boolean = true,
    val defaultRideType: RideType = RideType.UNKNOWN
)

/**
 * Persists user-configurable app settings (DataStore-backed). All reads are
 * exposed as a single observable [Flow] so the Settings screen and any
 * other consumer (e.g. Home screen respecting auto-location) always see the
 * latest values reactively.
 */
interface AppSettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setAutoLocationEnabled(enabled: Boolean)
    suspend fun setAccessibilityAutomationEnabled(enabled: Boolean)
    suspend fun setDefaultRideType(rideType: RideType)
}
