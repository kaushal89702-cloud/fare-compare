package com.family.farecompare.domain.automation

import com.family.farecompare.domain.model.ProviderComparisonOutcome

/**
 * Progress emitted while running the [AutomationState] state machine for a
 * single provider. [InProgress] always carries the current state, a
 * human-readable message, and the retry attempt count for whatever the
 * current step is doing (0 when not retrying). The terminal [Finished]
 * step always carries a [ProviderComparisonOutcome] - success or a specific
 * failure reason - so callers never need to guess why a run ended.
 */
sealed class RideAutomationStep {
    data class InProgress(
        val state: AutomationState,
        val message: String,
        val retryCount: Int = 0
    ) : RideAutomationStep()

    data class Finished(val outcome: ProviderComparisonOutcome) : RideAutomationStep()
}
