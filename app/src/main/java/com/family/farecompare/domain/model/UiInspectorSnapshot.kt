package com.family.farecompare.domain.model

/**
 * Latest state of the UI Inspector: the formatted tree dump, whether the
 * source UI has stopped changing (stabilized), and where the dump was
 * exported to, if anywhere.
 */
data class UiInspectorSnapshot(
    val treeText: String,
    val nodeCount: Int,
    val isStabilized: Boolean,
    val exportedFilePath: String?,
    val lastUpdatedAtMillis: Long
)
