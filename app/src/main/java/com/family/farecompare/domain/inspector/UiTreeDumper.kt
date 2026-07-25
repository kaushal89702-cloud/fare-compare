package com.family.farecompare.domain.inspector

import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.model.UiNodeSnapshot

/**
 * Recursively captures an [AccessibilityNodeInfo] tree into a plain
 * [UiNodeSnapshot] tree for developer inspection. Read-only: never clicks,
 * types, or performs any action on the nodes it visits.
 */
interface UiTreeDumper {
    fun dump(rootNode: AccessibilityNodeInfo): UiNodeSnapshot
}
