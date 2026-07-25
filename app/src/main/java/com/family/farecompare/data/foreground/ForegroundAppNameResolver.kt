package com.family.farecompare.data.foreground

import android.content.Context
import android.content.pm.PackageManager
import com.family.farecompare.domain.foreground.KnownForegroundApps
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Resolves a human-readable display name for a package name: known
 * ride-hailing/utility apps first, then the device's installed application
 * label, falling back to the raw package name if nothing else is available.
 */
class ForegroundAppNameResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun resolveDisplayName(packageName: String): String {
        KnownForegroundApps.displayNameFor(packageName)?.let { return it }

        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (exception: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
