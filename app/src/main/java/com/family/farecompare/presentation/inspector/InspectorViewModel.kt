package com.family.farecompare.presentation.inspector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.farecompare.domain.automation.AccessibilityGatewayRepository
import com.family.farecompare.domain.inspector.ClickableNodeInspector
import com.family.farecompare.domain.inspector.UiInspectorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class InspectorViewModel @Inject constructor(
    uiInspectorRepository: UiInspectorRepository,
    private val clickableNodeInspector: ClickableNodeInspector,
    private val accessibilityGatewayRepository: AccessibilityGatewayRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectorUiState())
    val uiState: StateFlow<InspectorUiState> = _uiState.asStateFlow()

    init {
        uiInspectorRepository.snapshot
            .onEach { snapshot ->
                if (snapshot == null) return@onEach
                _uiState.update {
                    it.copy(
                        treeText = snapshot.treeText,
                        nodeCount = snapshot.nodeCount,
                        isStabilized = snapshot.isStabilized,
                        exportedFilePath = snapshot.exportedFilePath
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onToggleClickableOnly() {
        _uiState.update { it.copy(showClickableOnly = !it.showClickableOnly) }
        if (_uiState.value.showClickableOnly) refreshClickableNodes()
    }

    fun refreshClickableNodes() {
        val rootNode = accessibilityGatewayRepository.currentRootNode()
        val text = if (rootNode != null) {
            clickableNodeInspector.inspect(rootNode)
        } else {
            "No active window - open Uber, Ola, or Rapido first."
        }
        _uiState.update { it.copy(clickableNodesText = text) }
    }
}
