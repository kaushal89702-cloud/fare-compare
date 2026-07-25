package com.family.farecompare.data.accessibility

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.family.farecompare.domain.accessibility.AccessibilityStatusChecker
import com.family.farecompare.service.FareCompareAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AccessibilityStatusCheckerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AccessibilityStatusChecker {

    override fun isServiceEnabled(): Boolean {
        val expectedComponent = ComponentName(context, FareCompareAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabledServices) }
        for (componentNameString in splitter) {
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent == expectedComponent) {
                return true
            }
        }
        return false
    }
}
