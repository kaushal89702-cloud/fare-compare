package com.family.farecompare.domain.location

/**
 * Resolves a human-readable address for the device's last known GPS
 * location, used to verify whether a detected pickup field already reflects
 * the user's current location. Returns null if location permission is not
 * granted, no last-known location is available, or geocoding fails - never
 * throws.
 */
interface DeviceLocationProvider {
    suspend fun getLastKnownAddress(): String?
}
