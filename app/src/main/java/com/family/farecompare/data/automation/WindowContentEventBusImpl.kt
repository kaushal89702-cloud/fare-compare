package com.family.farecompare.data.automation

import com.family.farecompare.domain.automation.WindowContentEvent
import com.family.farecompare.domain.automation.WindowContentEventBus
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WindowContentEventBusImpl @Inject constructor() : WindowContentEventBus {

    private val _events = MutableSharedFlow<WindowContentEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val events: SharedFlow<WindowContentEvent> = _events.asSharedFlow()

    override fun publish(event: WindowContentEvent) {
        _events.tryEmit(event)
    }
}
