package com.family.farecompare.presentation.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.family.farecompare.domain.accessibility.AccessibilityStatusChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PermissionsUiState(
    val isLocationGranted: Boolean = false,
    val isNotificationGranted: Boolean = false,
    val isAccessibilityGranted: Boolean = false
)

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accessibilityStatusChecker: AccessibilityStatusChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refreshAccessibilityStatus() = refresh()

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(isLocationGranted = granted) }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(isNotificationGranted = granted) }
    }

    private fun refresh() {
        _uiState.update {
            it.copy(
                isLocationGranted = isLocationGranted(),
                isNotificationGranted = isNotificationGranted(),
                isAccessibilityGranted = accessibilityStatusChecker.isServiceEnabled()
            )
        }
    }

    private fun isLocationGranted(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun isNotificationGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
