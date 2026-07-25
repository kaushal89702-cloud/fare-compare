package com.family.farecompare.domain.model

/**
 * Generic ride-tier classification used across all providers. Not every
 * provider exposes every tier; [UNKNOWN] is used when a fare is extracted
 * but its tier label could not be confidently classified.
 */
enum class RideType(val displayName: String) {
    BIKE("Bike"),
    AUTO("Auto"),
    CAB("Cab"),
    MINI("Mini"),
    PREMIUM("Premium"),
    UNKNOWN("Ride")
}
