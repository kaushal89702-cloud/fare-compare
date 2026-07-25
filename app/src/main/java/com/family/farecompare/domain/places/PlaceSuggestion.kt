package com.family.farecompare.domain.places

/**
 * A single autocomplete suggestion. [placeId] is the Places API identifier
 * used to resolve full details (formatted address + lat/lng) when the user
 * taps this suggestion.
 */
data class PlaceSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String
)

/**
 * A fully resolved place: formatted address plus coordinates, filled in
 * after the user taps a [PlaceSuggestion].
 */
data class ResolvedPlace(
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double
)
