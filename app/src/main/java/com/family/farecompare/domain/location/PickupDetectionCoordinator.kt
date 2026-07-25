package com.family.farecompare.domain.location

import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.model.PickupDetectionResult
import com.family.farecompare.domain.model.PickupVerificationStatus
import javax.inject.Inject

private const val TOKEN_OVERLAP_THRESHOLD = 0.4
private const val GENERIC_AUTO_LOCATION_PHRASES = "current location|your location|use current location"

/**
 * Ties together field detection and GPS-based verification, then publishes
 * the result to [PickupDetectionRepository]. The accessibility service only
 * needs to call [analyze] whenever a ride provider's window content is
 * worth inspecting; this class contains all the business logic so the
 * service itself stays a thin event dispatcher.
 */
class PickupDetectionCoordinator @Inject constructor(
    private val locationFieldDetector: LocationFieldDetector,
    private val deviceLocationProvider: DeviceLocationProvider,
    private val pickupDetectionRepository: PickupDetectionRepository
) {
    suspend fun analyze(rootNode: AccessibilityNodeInfo, providerDisplayName: String) {
        val detected = locationFieldDetector.detectPickupField(rootNode)
        if (detected == null) {
            pickupDetectionRepository.updateResult(
                PickupDetectionResult(
                    providerDisplayName = providerDisplayName,
                    detected = false,
                    confidenceScore = 0,
                    currentValue = null,
                    bounds = null,
                    verification = PickupVerificationStatus.NOT_DETECTED
                )
            )
            return
        }

        val address = deviceLocationProvider.getLastKnownAddress()
        pickupDetectionRepository.updateResult(
            PickupDetectionResult(
                providerDisplayName = providerDisplayName,
                detected = true,
                confidenceScore = detected.confidenceScore,
                currentValue = detected.value,
                bounds = detected.bounds,
                verification = verify(detected.value, address)
            )
        )
    }

    private fun verify(detectedValue: String, address: String?): PickupVerificationStatus {
        val normalizedValue = detectedValue.lowercase().trim()

        // Many providers show a generic "Current Location" placeholder once
        // they have already locked onto the device's GPS position - that
        // counts as already correct without needing an address match.
        if (GENERIC_AUTO_LOCATION_PHRASES.split('|').any { normalizedValue.contains(it) }) {
            return PickupVerificationStatus.ALREADY_CORRECT
        }

        if (address.isNullOrBlank()) return PickupVerificationStatus.LOCATION_UNAVAILABLE

        val detectedTokens = tokenize(detectedValue)
        val addressTokens = tokenize(address)
        if (detectedTokens.isEmpty() || addressTokens.isEmpty()) return PickupVerificationStatus.LOCATION_UNAVAILABLE

        val overlap = detectedTokens.intersect(addressTokens).size.toDouble() / detectedTokens.size
        return if (overlap >= TOKEN_OVERLAP_THRESHOLD) {
            PickupVerificationStatus.ALREADY_CORRECT
        } else {
            PickupVerificationStatus.NEEDS_UPDATE
        }
    }

    private fun tokenize(text: String): Set<String> = text.lowercase()
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .split(Regex("\\s+"))
        .filter { it.length > 2 }
        .toSet()
}
