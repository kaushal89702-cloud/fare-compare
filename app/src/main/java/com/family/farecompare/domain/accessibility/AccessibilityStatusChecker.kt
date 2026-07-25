package com.family.farecompare.domain.accessibility

/**
 * Checks whether the Fare Compare accessibility service is currently
 * enabled by the user in Android system settings.
 */
interface AccessibilityStatusChecker {
    fun isServiceEnabled(): Boolean
}
