package com.family.farecompare.data.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.family.farecompare.domain.automation.RideAppProvider
import com.family.farecompare.domain.foreground.ForegroundAppRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Shared install-check / launch / foreground-wait logic for every
 * [RideAppProvider]. Concrete providers (Uber, Ola, Rapido) only need to
 * supply [displayName] and [packageName].
 */
abstract class BaseRideAppProvider(
    private val context: Context,
    private val foregroundAppRepository: ForegroundAppRepository
) : RideAppProvider {

    override fun isInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (exception: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun launch(): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    override suspend fun waitUntilForeground(timeoutMs: Long): Boolean {
        val reachedForeground = withTimeoutOrNull(timeoutMs) {
            foregroundAppRepository.currentForegroundApp
                .filterNotNull()
                .first { it.packageName == packageName }
        }
        return reachedForeground != null
    }
}
