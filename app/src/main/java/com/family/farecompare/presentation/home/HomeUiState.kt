package com.family.farecompare.presentation.home

import com.family.farecompare.domain.model.FareResult
import com.family.farecompare.domain.model.RideProvider

data class HomeUiState(
    val pickup: String = "",
    val destination: String = "",
    val fareResults: List<FareResult> = RideProvider.values().map { FareResult(provider = it) }
)
