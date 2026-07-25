package com.family.farecompare.presentation.inspector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InspectorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Accessibility Inspector") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Clickable-only outline", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = uiState.showClickableOnly, onCheckedChange = { viewModel.onToggleClickableOnly() })
            }

            if (uiState.showClickableOnly) {
                Button(onClick = viewModel::refreshClickableNodes) {
                    Text("Refresh clickable nodes")
                }
                Text(
                    text = uiState.clickableNodesText.ifBlank { "Open Uber, Ola, or Rapido, then tap Refresh." },
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
                return@Column
            }

            Text(
                text = "Node count: ${uiState.nodeCount}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = if (uiState.isStabilized) "Status: Stabilized (dump complete)" else "Status: UI still changing...",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Exported to: ${uiState.exportedFilePath ?: "not yet exported"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = uiState.treeText.ifBlank { "Open Uber, Ola, or Rapido to begin capturing its UI tree." },
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )
        }
    }
}
