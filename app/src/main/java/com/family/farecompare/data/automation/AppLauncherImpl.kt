package com.family.farecompare.data.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.family.farecompare.domain.automation.AppLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppLauncherImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppLauncher {

    override fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (exception: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun launchApp(packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }
}
