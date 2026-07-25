package com.family.farecompare.domain.automation

import com.family.farecompare.domain.model.ProviderComparisonOutcome

/**
 * Progress emitted while running the full fill-pickup / fill-destination /
 * wait-for-fare pipeline for a single provider. The terminal [Finished]
 * step always carries a [ProviderComparisonOutcome] - success or a specific
 * failure reason - so callers never need to guess why a run ended.
 */
sealed class RideAutomationStep {
    data object CheckingAccessibility : RideAutomationStep()
    data object CheckingInstalled : RideAutomationStep()
    data object Launching : RideAutomationStep()
    data object WaitingForForeground : RideAutomationStep()
    data object FillingPickup : RideAutomationStep()
    data object FillingDestination : RideAutomationStep()
    data object WaitingForFare : RideAutomationStep()
    data object ReturningToFareCompare : RideAutomationStep()
    data class Finished(val outcome: ProviderComparisonOutcome) : RideAutomationStep()
}
