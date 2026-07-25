package com.family.farecompare.domain.connectivity

/**
 * Reports whether the device currently has any active, validated network
 * connection. Used to short-circuit automation with a clear "no internet"
 * failure reason instead of letting every downstream step time out
 * one-by-one when there's no connectivity at all.
 */
interface InternetConnectivityChecker {
    fun isConnected(): Boolean
}
