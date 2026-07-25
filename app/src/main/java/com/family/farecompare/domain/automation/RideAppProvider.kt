package com.family.farecompare.domain.automation

/**
 * Abstraction for a ride-hailing app that FareCompare can launch and detect
 * on-screen via the accessibility service. Each supported app (Uber, and
 * later Ola / Rapido) provides one implementation of this interface; the
 * orchestration logic in [LaunchRideAppUseCase] never needs to change when a
 * new provider is added.
 */
interface RideAppProvider {
    val displayName: String
    val packageName: String

    /**
     * Extra pickup/destination keywords specific to this provider's actual
     * UI wording, layered on top of the generic keyword set used by
     * [com.family.farecompare.domain.location.LocationFieldDetector]. Lets
     * the shared, provider-agnostic detection engine still handle each
     * app's real layout differences without hardcoding resource IDs.
     */
    val pickupFieldHints: List<String> get() = emptyList()
    val destinationFieldHints: List<String> get() = emptyList()

    fun isInstalled(): Boolean

    /** Launches the app. Returns true if a launch intent was found and started. */
    fun launch(): Boolean

    /**
     * Suspends (via accessibility foreground events, not polling) until this
     * provider's app becomes the foreground application, or [timeoutMs]
     * elapses.
     *
     * @return true if the app reached the foreground before the timeout.
     */
    suspend fun waitUntilForeground(timeoutMs: Long): Boolean
}
