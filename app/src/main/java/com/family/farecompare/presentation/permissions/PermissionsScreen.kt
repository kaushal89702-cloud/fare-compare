package com.family.farecompare.presentation.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.family.farecompare.presentation.common.OnResume

/**
 * Onboarding screen that explains and requests every permission FareCompare
 * can use, in the order requirements specify: Location -> Notifications
 * (API 33+) -> Accessibility (handled via system Settings, same as the
 * existing Settings screen flow). The app is fully usable even if every
 * permission here is denied - each rationale explains the fallback.
 */
@Composable
fun PermissionsScreen(
    onContinueClick: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OnResume { viewModel.refreshAccessibilityStatus() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        viewModel.onLocationPermissionResult(grants.values.any { it })
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Set up FareCompare",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        PermissionCard(
            title = "Location",
            rationale = "Lets FareCompare automatically fill your pickup field with your current address. " +
                "Without it, you can still type your pickup location manually.",
            granted = uiState.isLocationGranted,
            actionLabel = "Grant Location",
            onActionClick = {
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionCard(
                title = "Notifications",
                rationale = "Lets FareCompare notify you when a fare comparison finishes running in the " +
                    "background. Without it, results still appear when you return to the app.",
                granted = uiState.isNotificationGranted,
                actionLabel = "Grant Notifications",
                onActionClick = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }

        PermissionCard(
            title = "Accessibility Service",
            rationale = "Required for FareCompare to open Uber, Ola, and Rapido, read their displayed fares, " +
                "and fill in your pickup/destination. Without it, automatic comparison cannot run.",
            granted = uiState.isAccessibilityGranted,
            actionLabel = "Open Accessibility Settings",
            onActionClick = onOpenAccessibilitySettings
        )

        Button(
            onClick = onContinueClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }

        TextButton(onClick = onContinueClick, modifier = Modifier.fillMaxWidth()) {
            Text("Skip for now")
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    rationale: String,
    granted: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (granted) "$title \u2705" else title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(text = rationale, style = MaterialTheme.typography.bodyMedium)
            if (!granted) {
                Button(onClick = onActionClick) {
                    Text(actionLabel)
                }
            }
        }
    }
}
