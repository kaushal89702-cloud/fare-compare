package com.family.farecompare.domain.automation

/**
 * Brings FareCompare itself back to the foreground, e.g. after a
 * developer diagnostic action running inside another app's process
 * (accessibility service) has finished.
 */
interface AppReturner {
    fun bringFareCompareToForeground(): Boolean
}
