package com.family.farecompare.domain.location

/**
 * Resolves the device's current, live location (as opposed to
 * [DeviceLocationProvider], which only reads the last cached fix for
 * pickup-field verification). Used to auto-fill the Home screen's pickup
 * field. Returns null if permission is not granted or a fix could not be
 * obtained in time - never throws.
 */
interface CurrentLocationProvider {
    suspend fun getCurrentAddress(): String?
}
