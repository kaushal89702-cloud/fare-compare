package com.family.farecompare.data.foreground

import com.family.farecompare.domain.foreground.ForegroundAppRepository
import com.family.farecompare.domain.model.ForegroundApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForegroundAppRepositoryImpl @Inject constructor() : ForegroundAppRepository {

    private val _currentForegroundApp = MutableStateFlow<ForegroundApp?>(null)
    override val currentForegroundApp: StateFlow<ForegroundApp?> = _currentForegroundApp.asStateFlow()

    override fun updateForegroundApp(app: ForegroundApp) {
        _currentForegroundApp.value = app
    }
}
