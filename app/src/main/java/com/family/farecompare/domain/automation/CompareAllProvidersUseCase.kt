package com.family.farecompare.domain.automation

import com.family.farecompare.domain.model.ComparisonResult
import com.family.farecompare.domain.model.ProviderComparisonOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Runs [RideAutomationEngine] once per registered [RideAppProvider],
 * sequentially (only one ride app can realistically be in the foreground at
 * a time), and aggregates the results into a sorted [ComparisonResult].
 *
 * Emits a [RideAutomationStep] for every step of every provider (so the UI
 * can show live progress per-provider) followed by one final synthetic
 * "all done" event carrying the aggregated [ComparisonResult].
 */
class CompareAllProvidersUseCase @Inject constructor(
    private val rideAutomationEngine: RideAutomationEngine,
    providers: Set<@JvmSuppressWildcards RideAppProvider>
) {
    private val orderedProviders = providers.sortedBy { it.displayName }

    sealed class CompareEvent {
        data class ProviderProgress(val provider: RideAppProvider, val step: RideAutomationStep) : CompareEvent()
        data class AllFinished(val result: ComparisonResult) : CompareEvent()
    }

    operator fun invoke(pickupAddress: String, destinationAddress: String): Flow<CompareEvent> = flow {
        val outcomes = mutableListOf<ProviderComparisonOutcome>()

        for (provider in orderedProviders) {
            rideAutomationEngine(provider, pickupAddress, destinationAddress).collect { step ->
                emit(CompareEvent.ProviderProgress(provider, step))
                if (step is RideAutomationStep.Finished) {
                    outcomes.add(step.outcome)
                }
            }
        }

        val successfulQuotes = outcomes
            .filterIsInstance<ProviderComparisonOutcome.Success>()
            .flatMap { it.quotes }
            .sortedBy { it.fareAmount }

        val failures = outcomes.filterIsInstance<ProviderComparisonOutcome.Failure>()

        emit(CompareEvent.AllFinished(ComparisonResult(successfulQuotes, failures)))
    }
}
