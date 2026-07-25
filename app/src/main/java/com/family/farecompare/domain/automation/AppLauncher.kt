package com.family.farecompare.domain.automation

/**
 * Checks for and launches installed applications by package name.
 */
interface AppLauncher {
    fun isAppInstalled(packageName: String): Boolean
    fun launchApp(packageName: String): Boolean
}
