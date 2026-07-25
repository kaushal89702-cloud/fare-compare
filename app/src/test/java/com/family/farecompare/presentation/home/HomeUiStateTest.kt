package com.family.farecompare.presentation.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateTest {

    @Test
    fun `pickup error only shows after field touched and left blank`() {
        val untouched = HomeUiState(pickup = "", isPickupTouched = false)
        val touchedAndBlank = HomeUiState(pickup = "", isPickupTouched = true)
        val touchedAndFilled = HomeUiState(pickup = "MG Road", isPickupTouched = true)

        assertFalse(untouched.isPickupError)
        assertTrue(touchedAndBlank.isPickupError)
        assertFalse(touchedAndFilled.isPickupError)
    }

    @Test
    fun `destination error only shows after field touched and left blank`() {
        val untouched = HomeUiState(destination = "", isDestinationTouched = false)
        val touchedAndBlank = HomeUiState(destination = "", isDestinationTouched = true)
        val touchedAndFilled = HomeUiState(destination = "Whitefield", isDestinationTouched = true)

        assertFalse(untouched.isDestinationError)
        assertTrue(touchedAndBlank.isDestinationError)
        assertFalse(touchedAndFilled.isDestinationError)
    }

    @Test
    fun `compare is enabled only when both pickup and destination are non-blank`() {
        assertFalse(HomeUiState(pickup = "", destination = "").isCompareEnabled)
        assertFalse(HomeUiState(pickup = "A", destination = "").isCompareEnabled)
        assertFalse(HomeUiState(pickup = "", destination = "B").isCompareEnabled)
        assertTrue(HomeUiState(pickup = "A", destination = "B").isCompareEnabled)
    }

    @Test
    fun `compare is disabled when fields contain only whitespace`() {
        assertFalse(HomeUiState(pickup = "   ", destination = "   ").isCompareEnabled)
    }
}
