package com.family.farecompare.domain.location

import android.view.accessibility.AccessibilityNodeInfo

/**
 * After typing an address into a pickup/destination field, ride apps show a
 * list of autocomplete-style suggestions. This selector finds the
 * best-matching clickable suggestion node for the address that was typed,
 * using generic text-similarity scoring rather than any provider-specific
 * list/row identifiers.
 */
interface SuggestionSelector {
    fun findBestSuggestion(rootNode: AccessibilityNodeInfo, typedQuery: String): AccessibilityNodeInfo?
}
