package com.family.farecompare.domain.settings

import com.family.farecompare.domain.model.RideType
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `default settings use system theme and enable automation`() {
        val defaults = AppSettings()

        assertEquals(ThemeMode.SYSTEM, defaults.themeMode)
        assertEquals(true, defaults.autoLocationEnabled)
        assertEquals(true, defaults.accessibilityAutomationEnabled)
        assertEquals(RideType.UNKNOWN, defaults.defaultRideType)
    }

    @Test
    fun `copy overrides only the specified field`() {
        val customized = AppSettings().copy(themeMode = ThemeMode.DARK, autoLocationEnabled = false)

        assertEquals(ThemeMode.DARK, customized.themeMode)
        assertEquals(false, customized.autoLocationEnabled)
        assertEquals(true, customized.accessibilityAutomationEnabled)
    }
}
