package com.family.farecompare.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.family.farecompare.R
import com.family.farecompare.domain.model.RideType
import com.family.farecompare.domain.settings.AppSettings
import com.family.farecompare.domain.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        SettingsContent(
            settings = settings,
            modifier = Modifier.padding(padding),
            onEnableAccessibilityClick = { viewModel.openAccessibilitySettings(context) },
            onThemeModeSelected = viewModel::onThemeModeSelected,
            onAutoLocationToggled = viewModel::onAutoLocationToggled,
            onAccessibilityAutomationToggled = viewModel::onAccessibilityAutomationToggled,
            onDefaultRideTypeSelected = viewModel::onDefaultRideTypeSelected,
            onClearHistoryClicked = viewModel::onClearHistoryClicked
        )
    }
}

@Composable
private fun SettingsContent(
    settings: AppSettings,
    onEnableAccessibilityClick: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onAutoLocationToggled: (Boolean) -> Unit,
    onAccessibilityAutomationToggled: (Boolean) -> Unit,
    onDefaultRideTypeSelected: (RideType) -> Unit,
    onClearHistoryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsSection(title = "Appearance") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.values().forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onThemeModeSelected(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }

        SettingsSection(title = "Automation") {
            SettingsToggleRow(
                label = "Automatic current-location pickup",
                checked = settings.autoLocationEnabled,
                onCheckedChange = onAutoLocationToggled
            )
            SettingsToggleRow(
                label = "Enable Accessibility automation",
                checked = settings.accessibilityAutomationEnabled,
                onCheckedChange = onAccessibilityAutomationToggled
            )
        }

        SettingsSection(title = "Default ride type") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RideType.values().toList()) { rideType ->
                    FilterChip(
                        selected = settings.defaultRideType == rideType,
                        onClick = { onDefaultRideTypeSelected(rideType) },
                        label = { Text(rideType.displayName) }
                    )
                }
            }
        }

        SettingsSection(title = "Accessibility Service") {
            Text(
                text = stringResource(R.string.accessibility_settings_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onEnableAccessibilityClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.enable_accessibility_button),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        SettingsSection(title = "History") {
            Button(
                onClick = onClearHistoryClicked,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Clear recent searches")
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
