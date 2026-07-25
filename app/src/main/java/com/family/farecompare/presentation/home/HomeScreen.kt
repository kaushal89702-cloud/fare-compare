package com.family.farecompare.presentation.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.family.farecompare.R
import com.family.farecompare.domain.model.FareResult
import com.family.farecompare.domain.model.RideProvider
import com.family.farecompare.presentation.common.OnResume
import com.family.farecompare.presentation.components.CurrentAppCard
import com.family.farecompare.presentation.components.FareResultCard
import com.family.farecompare.presentation.theme.FareCompareTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    OnResume { viewModel.refreshAccessibilityStatus() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        HomeScreenContent(
            modifier = Modifier.padding(padding),
            uiState = uiState,
            onPickupChanged = viewModel::onPickupChanged,
            onDestinationChanged = viewModel::onDestinationChanged,
            onPickupFocusLost = viewModel::onPickupFocusLost,
            onDestinationFocusLost = viewModel::onDestinationFocusLost,
            onCompareClicked = viewModel::onCompareClicked,
            onSettingsClick = onSettingsClick
        )
    }
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onPickupChanged: (String) -> Unit,
    onDestinationChanged: (String) -> Unit,
    onPickupFocusLost: () -> Unit,
    onDestinationFocusLost: () -> Unit,
    onCompareClicked: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings_icon)
                )
            }
        }

        Text(
            text = if (uiState.isAccessibilityEnabled) {
                stringResource(R.string.accessibility_enabled_status)
            } else {
                stringResource(R.string.accessibility_disabled_status)
            },
            style = MaterialTheme.typography.bodyMedium
        )

        CurrentAppCard(currentAppDisplayName = uiState.currentForegroundApp?.displayName)

        Text(
            text = uiState.automationStatusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = uiState.pickup,
            onValueChange = onPickupChanged,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) onPickupFocusLost()
                },
            label = { Text(stringResource(R.string.pickup_label)) },
            singleLine = true,
            isError = uiState.isPickupError,
            supportingText = {
                if (uiState.isPickupError) {
                    Text(stringResource(R.string.pickup_required))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )

        OutlinedTextField(
            value = uiState.destination,
            onValueChange = onDestinationChanged,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) onDestinationFocusLost()
                },
            label = { Text(stringResource(R.string.destination_label)) },
            singleLine = true,
            isError = uiState.isDestinationError,
            supportingText = {
                if (uiState.isDestinationError) {
                    Text(stringResource(R.string.destination_required))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )

        Button(
            onClick = onCompareClicked,
            enabled = uiState.isCompareEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.compare_button),
                style = MaterialTheme.typography.titleMedium
            )
        }

        FareResultsSection(
            fareResults = uiState.fareResults,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

@Composable
private fun FareResultsSection(
    fareResults: List<FareResult>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val isWideLayout = maxWidth >= 600.dp
        if (isWideLayout) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                fareResults.forEach { result ->
                    FareResultCard(result = result, modifier = Modifier.weight(1f))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                fareResults.forEach { result ->
                    FareResultCard(result = result, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun HomeScreenPreviewLight() {
    FareCompareTheme(darkTheme = false, dynamicColor = false) {
        Surface {
            HomeScreenContent(
                uiState = HomeUiState(automationStatusText = "Idle"),
                onPickupChanged = {},
                onDestinationChanged = {},
                onPickupFocusLost = {},
                onDestinationFocusLost = {},
                onCompareClicked = {},
                onSettingsClick = {}
            )
        }
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreviewDark() {
    FareCompareTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            HomeScreenContent(
                uiState = HomeUiState(
                    pickup = "Koramangala",
                    destination = "Whitefield",
                    isAccessibilityEnabled = true,
                    automationStatusText = "Waiting for Uber...",
                    fareResults = RideProvider.values().map { FareResult(provider = it) }
                ),
                onPickupChanged = {},
                onDestinationChanged = {},
                onPickupFocusLost = {},
                onDestinationFocusLost = {},
                onCompareClicked = {},
                onSettingsClick = {}
            )
        }
    }
}
