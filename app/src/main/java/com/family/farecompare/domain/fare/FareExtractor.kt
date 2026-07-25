package com.family.farecompare.domain.fare

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Companion detector for the common "no rides available in your area right
 * now" empty-state screens ride apps show. Kept as a small separate
 * interface (rather than folded into [FareExtractor]) so unit tests and
 * callers can reason about "we found nothing because there is genuinely
 * nothing" versus "we found nothing because our detector missed it".
 */
interface NoRidesAvailableDetector {
    fun isNoRidesAvailableScreen(rootNode: AccessibilityNodeInfo): Boolean
}

/**
 * Provider-agnostic fare extraction engine.
 *
 * Implementations walk the accessibility node tree of whichever ride app is
 * currently in the foreground and locate every node whose text looks like a
 * fare (currency symbol + number, with or without a range or discount
 * annotation), pairing each with the nearest ride-tier label ("Bike",
 * "Auto", "Mini", etc.) and ETA text found nearby. No provider-specific
 * resource IDs, class names, or layouts are ever referenced, so the same
 * implementation is reused for Uber, Ola, and Rapido.
 */
interface FareExtractor {
    fun extractFares(rootNode: AccessibilityNodeInfo): List<DetectedFare>
}
