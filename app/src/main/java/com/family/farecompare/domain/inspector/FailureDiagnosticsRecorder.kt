package com.family.farecompare.domain.inspector

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Captures a full node-tree dump whenever a step of the automation state
 * machine fails, exports it to Documents/FareCompare (same mechanism as the
 * developer UI Inspector) and returns a short summary of every editable
 * field found on screen at the time of failure. That summary is used by
 * [com.family.farecompare.domain.automation.RideAutomationEngine] to decide
 * *why* a field wasn't found (e.g. "there is no editable field visible at
 * all" vs "there are editable fields, but none matched our keywords")
 * instead of blindly retrying or giving up.
 */
interface FailureDiagnosticsRecorder {
    suspend fun captureFailure(providerDisplayName: String, reason: String, rootNode: AccessibilityNodeInfo?): FailureDiagnosticsSummary
}

data class FailureDiagnosticsSummary(
    val exportedFilePath: String?,
    val editableFieldDescriptions: List<String>,
    val totalNodeCount: Int
)
