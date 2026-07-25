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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.family.farecompare.R
import com.family.farecompare.domain.model.FareResult
import com.family.farecompare.domain.model.RideProvider
import com.family.farecompare.presentation.components.FareResultCard
import com.family.farecompare.presentation.theme.FareCompareTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreenContent(
        modifier = modifier,
        uiState = uiState,
        onPickupChanged = viewModel::onPickupChanged,
        onDestinationChanged = viewModel::onDestinationChanged,
        onCompareClicked = viewModel::onCompareClicked
    )
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onPickupChanged: (String) -> Unit,
    onDestinationChanged: (String) -> Unit,
    onCompareClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = uiState.pickup,
            onValueChange = onPickupChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.pickup_label)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        OutlinedTextField(
            value = uiState.destination,
            onValueChange = onDestinationChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.destination_label)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Button(
            onClick = onCompareClicked,
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
                uiState = HomeUiState(),
                onPickupChanged = {},
                onDestinationChanged = {},
                onCompareClicked = {}
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
                    fareResults = RideProvider.values().map { FareResult(provider = it) }
                ),
                onPickupChanged = {},
                onDestinationChanged = {},
                onCompareClicked = {}
            )
        }
    }
}
