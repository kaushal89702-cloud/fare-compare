package com.family.farecompare.data.fare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FareTextParserTest {

    @Test
    fun `parses simple rupee amount`() {
        assertEquals(120.0, FareTextParser.parseAmount("₹120"))
    }

    @Test
    fun `parses amount with thousands separator`() {
        assertEquals(1200.0, FareTextParser.parseAmount("₹1,200"))
    }

    @Test
    fun `parses decimal amount`() {
        assertEquals(99.5, FareTextParser.parseAmount("₹99.50"))
    }

    @Test
    fun `parses lower bound of a range`() {
        assertEquals(120.0, FareTextParser.parseAmount("₹120-150"))
    }

    @Test
    fun `parses amount with Rs prefix`() {
        assertEquals(80.0, FareTextParser.parseAmount("Rs. 80"))
    }

    @Test
    fun `parses amount with INR prefix`() {
        assertEquals(250.0, FareTextParser.parseAmount("INR 250"))
    }

    @Test
    fun `ignores discount annotation and keeps first amount`() {
        assertEquals(99.0, FareTextParser.parseAmount("₹99 (20% off)"))
    }

    @Test
    fun `returns null when no currency amount present`() {
        assertNull(FareTextParser.parseAmount("Confirm your ride"))
    }

    @Test
    fun `returns null for bare number without currency symbol`() {
        assertNull(FareTextParser.parseAmount("12 min away"))
    }

    @Test
    fun `parses eta in minutes`() {
        assertEquals(12, FareTextParser.parseEtaMinutes("12 min away"))
    }

    @Test
    fun `parses eta with mins spelling`() {
        assertEquals(5, FareTextParser.parseEtaMinutes("5 mins"))
    }

    @Test
    fun `returns null eta when absent`() {
        assertNull(FareTextParser.parseEtaMinutes("₹120"))
    }
}
