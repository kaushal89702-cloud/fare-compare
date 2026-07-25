package com.family.farecompare.presentation.comparison

import com.family.farecompare.domain.model.ComparisonResult
import com.family.farecompare.domain.model.FareQuote

data class ProviderProgressUi(
    val providerDisplayName: String,
    val statusText: String,
    val isFinished: Boolean,
    val diagnosticsText: String? = null
)

data class ComparisonUiState(
    val pickup: String = "",
    val destination: String = "",
    val providerProgress: List<ProviderProgressUi> = emptyList(),
    val result: ComparisonResult? = null,
    val isRunning: Boolean = false,
    val bookingQuote: FareQuote? = null
)
