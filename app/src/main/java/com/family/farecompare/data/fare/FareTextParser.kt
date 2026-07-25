package com.family.farecompare.data.fare

/**
 * Pure text-parsing helpers for fare extraction. Deliberately has zero
 * Android framework dependency so it can be unit tested on the JVM without
 * an emulator or instrumentation.
 */
object FareTextParser {

    // Matches "₹120", "Rs. 120", "INR 120", "$12.50", "120" (as a bare
    // number is only accepted when preceded by a currency-like symbol
    // elsewhere in the caller's context, so this regex alone stays strict
    // to currency-prefixed amounts to avoid false positives on e.g. phone
    // numbers or times).
    private val CURRENCY_AMOUNT_REGEX = Regex(
        "(?:₹|rs\\.?|inr|\\$|usd)\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)",
        RegexOption.IGNORE_CASE
    )

    private val ETA_MINUTES_REGEX = Regex("(\\d+)\\s*min", RegexOption.IGNORE_CASE)

    /**
     * Parses the first currency-looking amount out of [text]. When the text
     * describes a range (e.g. "₹120-150" or "₹120 - ₹150"), the lower bound
     * is returned since that is the fare a user would realistically expect
     * to pay at minimum. Discount annotations like "(20% off)" or strike-
     * through original prices are ignored - only the first matched amount
     * is used.
     */
    fun parseAmount(text: String): Double? {
        val match = CURRENCY_AMOUNT_REGEX.find(text) ?: return null
        val normalized = match.groupValues[1].replace(",", "")
        return normalized.toDoubleOrNull()
    }

    fun parseEtaMinutes(text: String): Int? {
        val match = ETA_MINUTES_REGEX.find(text) ?: return null
        return match.groupValues[1].toIntOrNull()
    }
}
