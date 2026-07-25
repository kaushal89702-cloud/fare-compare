package com.family.farecompare.data.automation

import android.content.Context
import android.content.Intent
import com.family.farecompare.domain.automation.AppReturner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SelfAppReturnerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppReturner {

    override fun bringFareCompareToForeground(): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(launchIntent)
        return true
    }
}
