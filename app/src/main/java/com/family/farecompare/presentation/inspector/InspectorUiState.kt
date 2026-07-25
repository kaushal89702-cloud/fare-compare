package com.family.farecompare.presentation.inspector

data class InspectorUiState(
    val treeText: String = "",
    val nodeCount: Int = 0,
    val isStabilized: Boolean = false,
    val exportedFilePath: String? = null,
    val showClickableOnly: Boolean = false,
    val clickableNodesText: String = ""
)
