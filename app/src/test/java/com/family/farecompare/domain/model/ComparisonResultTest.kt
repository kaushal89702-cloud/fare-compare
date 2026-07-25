package com.family.farecompare.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparisonResultTest {

    private fun quote(provider: RideProvider, amount: Double) = FareQuote(
        provider = provider,
        rideType = RideType.CAB,
        fareAmount = amount,
        rawFareText = "\u20b9${amount.toInt()}",
        etaMinutes = null,
        timestampMillis = 0L
    )

    @Test
    fun `cheapestQuote returns lowest fare when quotes are pre-sorted`() {
        val result = ComparisonResult(
            successfulQuotes = listOf(quote(RideProvider.RAPIDO, 100.0), quote(RideProvider.UBER, 150.0)),
            failures = emptyList()
        )
        assertEquals(RideProvider.RAPIDO, result.cheapestQuote?.provider)
        assertEquals(100.0, result.cheapestQuote?.fareAmount)
    }

    @Test
    fun `cheapestQuote is null when there are no successful quotes`() {
        val result = ComparisonResult(successfulQuotes = emptyList(), failures = emptyList())
        assertNull(result.cheapestQuote)
    }

    @Test
    fun `hasAnyResult reflects presence of successful quotes`() {
        val withResults = ComparisonResult(listOf(quote(RideProvider.OLA, 90.0)), emptyList())
        val withoutResults = ComparisonResult(emptyList(), emptyList())

        assertTrue(withResults.hasAnyResult)
        assertFalse(withoutResults.hasAnyResult)
    }
}
