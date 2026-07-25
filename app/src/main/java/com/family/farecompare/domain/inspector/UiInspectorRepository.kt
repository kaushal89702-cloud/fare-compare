package com.family.farecompare.domain.inspector

import com.family.farecompare.domain.model.UiInspectorSnapshot
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the most recent UI Inspector dump. Updated by the accessibility
 * service, observed by the developer screen.
 */
interface UiInspectorRepository {
    val snapshot: StateFlow<UiInspectorSnapshot?>
    fun updateSnapshot(snapshot: UiInspectorSnapshot)
}
