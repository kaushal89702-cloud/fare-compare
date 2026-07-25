package com.family.farecompare.domain.model

/**
 * Result of running the full automation pipeline for a single provider:
 * either a list of fare quotes (usually one per ride tier found on screen)
 * or a specific failure reason. Never throws - every possible outcome is
 * represented as data.
 */
sealed class ProviderComparisonOutcome {
    data class Success(val quotes: List<FareQuote>) : ProviderComparisonOutcome()

    /**
     * [diagnosticsSummary], when present, describes exactly what the
     * automation engine actually found on screen at the moment of failure
     * (e.g. "3 editable fields present: resourceId=... text='...' ...") so
     * the failure can be diagnosed and the field-detection keywords/hints
     * tuned without needing logcat access.
     */
    data class Failure(
        val provider: RideProvider,
        val reason: AutomationFailureReason,
        val diagnosticsSummary: String? = null
    ) : ProviderComparisonOutcome()
}
