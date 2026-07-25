package com.family.farecompare.domain.model

/**
 * Result of running the full automation pipeline for a single provider:
 * either a list of fare quotes (usually one per ride tier found on screen)
 * or a specific failure reason. Never throws - every possible outcome is
 * represented as data.
 */
sealed class ProviderComparisonOutcome {
    data class Success(val quotes: List<FareQuote>) : ProviderComparisonOutcome()
    data class Failure(val provider: RideProvider, val reason: AutomationFailureReason) : ProviderComparisonOutcome()
}
