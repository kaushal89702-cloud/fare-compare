package com.family.farecompare.presentation.comparison

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.farecompare.domain.automation.CompareAllProvidersUseCase
import com.family.farecompare.domain.automation.PendingComparisonRequestHolder
import com.family.farecompare.domain.automation.RideAppProvider
import com.family.farecompare.domain.automation.RideAutomationStep
import com.family.farecompare.domain.model.AutomationFailureReason
import com.family.farecompare.domain.model.ProviderComparisonOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComparisonViewModel @Inject constructor(
    private val compareAllProvidersUseCase: CompareAllProvidersUseCase,
    rideAppProviders: Set<@JvmSuppressWildcards RideAppProvider>,
    pendingComparisonRequestHolder: PendingComparisonRequestHolder
) : ViewModel() {

    private val providersByPackageName = rideAppProviders.associateBy { it.packageName }

    private val _uiState = MutableStateFlow(ComparisonUiState())
    val uiState: StateFlow<ComparisonUiState> = _uiState.asStateFlow()

    init {
        val request = pendingComparisonRequestHolder.request
        if (request != null) {
            _uiState.update { it.copy(pickup = request.pickupAddress, destination = request.destinationAddress) }
            startComparison(request.pickupAddress, request.destinationAddress)
        }
    }

    /**
     * Automation up to this point has already opened the cheapest
     * provider's app, filled the trip, and read its fare. Per Uber/Ola/
     * Rapido terms of service and Android's own accessibility-service
     * guidelines, FareCompare does not attempt to tap "Confirm ride" or
     * complete payment on the user's behalf - only the human user may give
     * that final confirmation. This action simply re-launches (or brings
     * to foreground) the cheapest provider's app so the user can review
     * the already-filled trip and tap Book/Confirm themselves.
     */
    fun onBookNowClicked() {
        val quote = _uiState.value.bookingQuote ?: return
        val provider = providersByPackageName[quote.provider.packageName] ?: return
        provider.launch()
    }

    private fun startComparison(pickup: String, destination: String) {
        _uiState.update { it.copy(isRunning = true) }
        viewModelScope.launch {
            compareAllProvidersUseCase(pickup, destination).collect { event ->
                when (event) {
                    is CompareAllProvidersUseCase.CompareEvent.ProviderProgress -> {
                        updateProgress(event.provider, event.step)
                    }
                    is CompareAllProvidersUseCase.CompareEvent.AllFinished -> {
                        _uiState.update {
                            it.copy(
                                isRunning = false,
                                result = event.result,
                                bookingQuote = event.result.cheapestQuote
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateProgress(provider: RideAppProvider, step: RideAutomationStep) {
        val statusText = statusTextFor(provider.displayName, step)
        val diagnosticsSummary = (step as? RideAutomationStep.Finished)
            ?.let { (it.outcome as? ProviderComparisonOutcome.Failure)?.diagnosticsSummary }

        _uiState.update { state ->
            val existingIndex = state.providerProgress.indexOfFirst { it.providerDisplayName == provider.displayName }
            val updatedEntry = ProviderProgressUi(
                providerDisplayName = provider.displayName,
                statusText = statusText,
                isFinished = step is RideAutomationStep.Finished,
                diagnosticsText = diagnosticsSummary
            )
            val updatedList = if (existingIndex >= 0) {
                state.providerProgress.toMutableList().apply { set(existingIndex, updatedEntry) }
            } else {
                state.providerProgress + updatedEntry
            }
            state.copy(providerProgress = updatedList)
        }
    }

    private fun statusTextFor(providerName: String, step: RideAutomationStep): String = when (step) {
        is RideAutomationStep.InProgress -> {
            val retrySuffix = if (step.retryCount > 0) " (retry ${step.retryCount})" else ""
            "${step.message}$retrySuffix"
        }
        is RideAutomationStep.Finished -> finishedStatusText(providerName, step)
    }

    private fun finishedStatusText(providerName: String, step: RideAutomationStep.Finished): String {
        return when (val outcome = step.outcome) {
            is ProviderComparisonOutcome.Success -> {
                val cheapest = outcome.quotes.minByOrNull { it.fareAmount }
                if (cheapest != null) "$providerName: \u20b9${cheapest.fareAmount.toInt()}" else "$providerName: done"
            }
            is ProviderComparisonOutcome.Failure -> "$providerName failed: ${failureMessage(outcome)}"
        }
    }

    /**
     * Every [AutomationFailureReason] mapped to a short, human-readable
     * explanation. Kept exhaustive (no `else` branch) so adding a new
     * failure reason forces a matching UI message to be written too.
     */
    private fun failureMessage(failure: ProviderComparisonOutcome.Failure): String = when (val reason = failure.reason) {
        AutomationFailureReason.AccessibilityDisabled -> "Accessibility Service is disabled"
        AutomationFailureReason.AppNotInstalled -> "app not installed"
        AutomationFailureReason.LaunchTimeout -> "took too long to open"
        AutomationFailureReason.PickupFieldNotFound -> "couldn't find the pickup field"
        AutomationFailureReason.DestinationFieldNotFound -> "couldn't find the destination field"
        AutomationFailureReason.SuggestionNotFound -> "no matching address suggestion"
        AutomationFailureReason.FareNotFoundBeforeTimeout -> "fare didn't appear in time"
        AutomationFailureReason.NoRidesAvailable -> "no rides available right now"
        AutomationFailureReason.NoInternet -> "no internet connection"
        is AutomationFailureReason.Unexpected -> "unexpected error (${reason.message})"
    }
}
