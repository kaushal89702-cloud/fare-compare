package com.family.farecompare.domain.foreground

import com.family.farecompare.domain.model.ForegroundApp
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the most recently detected foreground application. Updated by the
 * accessibility service, observed by the presentation layer.
 */
interface ForegroundAppRepository {
    val currentForegroundApp: StateFlow<ForegroundApp?>
    fun updateForegroundApp(app: ForegroundApp)
}
