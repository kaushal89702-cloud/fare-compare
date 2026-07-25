package com.family.farecompare.domain.model

/**
 * Aggregated result of comparing every configured ride provider once.
 * [successfulQuotes] is pre-sorted ascending by [FareQuote.fareAmount] so
 * the cheapest quote is always index 0.
 */
data class ComparisonResult(
    val successfulQuotes: List<FareQuote>,
    val failures: List<ProviderComparisonOutcome.Failure>
) {
    val cheapestQuote: FareQuote? get() = successfulQuotes.firstOrNull()
    val hasAnyResult: Boolean get() = successfulQuotes.isNotEmpty()
}
