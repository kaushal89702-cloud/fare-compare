package com.family.farecompare.domain.model

enum class PickupVerificationStatus {
    /** Detected pickup value already matches (or defers to) the device's current GPS location. */
    ALREADY_CORRECT,

    /** Detected pickup value does not match the device's current GPS location. */
    NEEDS_UPDATE,

    /** GPS location or reverse-geocoded address could not be obtained. */
    LOCATION_UNAVAILABLE,

    /** No pickup field could be found on screen. */
    NOT_DETECTED
}
