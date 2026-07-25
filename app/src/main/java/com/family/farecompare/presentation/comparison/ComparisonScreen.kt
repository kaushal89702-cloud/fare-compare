package com.family.farecompare.presentation.comparison

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.family.farecompare.domain.model.FareQuote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ComparisonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Fare Comparison") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${uiState.pickup} \u2192 ${uiState.destination}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (uiState.isRunning) {
                ProviderProgressList(uiState)
            }

            val result = uiState.result
            if (result != null) {
                if (result.hasAnyResult) {
                    result.successfulQuotes.forEachIndexed { index, quote ->
                        FareQuoteCard(quote = quote, isCheapest = index == 0)
                    }
                } else {
                    Text(
                        text = "No fares could be read from any provider. Check that the ride apps are " +
                            "installed and Accessibility is enabled, then try again.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                val cheapest = result.cheapestQuote
                if (cheapest != null) {
                    Text(
                        text = "${cheapest.provider.displayName} has the cheapest fare.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = viewModel::onBookNowClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Book Now with ${cheapest.provider.displayName}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderProgressList(uiState: ComparisonUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        uiState.providerProgress.forEach { progress ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = progress.providerDisplayName, fontWeight = FontWeight.Bold)
                Text(text = progress.statusText, style = MaterialTheme.typography.bodySmall)
                if (!progress.isFinished) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp))
                }
            }
            progress.diagnosticsText?.let { diagnostics ->
                Text(
                    text = "Diagnostics: $diagnostics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun FareQuoteCard(quote: FareQuote, isCheapest: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCheapest) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = quote.provider.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = quote.rideType.displayName, style = MaterialTheme.typography.bodySmall)
                quote.etaMinutes?.let { eta ->
                    Text(text = "$eta min away", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                text = "\u20b9${quote.fareAmount.toInt()}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isCheapest) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
        }
    }
}
