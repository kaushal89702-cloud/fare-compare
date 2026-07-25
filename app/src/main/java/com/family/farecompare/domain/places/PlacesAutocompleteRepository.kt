package com.family.farecompare.domain.places

/**
 * Wraps the Google Places SDK for live address autocomplete on the pickup
 * and destination fields.
 *
 * IMPORTANT / KNOWN LIMITATION: the Places SDK requires a Google Cloud API
 * key with Places API enabled and billing configured on the project. This
 * app never hardcodes that key in source - see [isAvailable]. When no key
 * is configured (e.g. this build), every method degrades gracefully:
 * [autocomplete] returns an empty list and [resolvePlace] returns null, so
 * the rest of the app (manual address entry, automation, comparison) keeps
 * working exactly as if Places were simply not present.
 */
interface PlacesAutocompleteRepository {
    val isAvailable: Boolean
    suspend fun autocomplete(query: String): List<PlaceSuggestion>
    suspend fun resolvePlace(placeId: String): ResolvedPlace?
}
