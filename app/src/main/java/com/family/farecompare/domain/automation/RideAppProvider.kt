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
