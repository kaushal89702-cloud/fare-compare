package com.family.farecompare.domain.automation

import kotlinx.coroutines.flow.SharedFlow

data class WindowContentEvent(val packageName: String)

/**
 * Broadcasts every window-state/content-changed accessibility event the
 * service observes, so the automation engine can react to real UI changes
 * (wait for a screen to load, wait for suggestions to appear, wait for a
 * fare to render) instead of using fixed delays or polling loops.
 */
interface WindowContentEventBus {
    val events: SharedFlow<WindowContentEvent>
    fun publish(event: WindowContentEvent)
}
