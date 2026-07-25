package com.family.farecompare.domain.model

data class FareResult(
    val provider: RideProvider,
    val fare: String? = null,
    val isLoading: Boolean = false
)
