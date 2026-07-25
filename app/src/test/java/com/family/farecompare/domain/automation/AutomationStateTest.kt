package com.family.farecompare.domain.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class AutomationStateTest {

    /**
     * Locks in the exact required state sequence so a future refactor can't
     * silently drop or reorder a state without a test failing.
     */
    @Test
    fun `contains every required state in the required order`() {
        val expected = listOf(
            AutomationState.WAIT_APP,
            AutomationState.WAIT_PICKUP_FIELD,
            AutomationState.ENTER_PICKUP,
            AutomationState.WAIT_PICKUP_CONFIRM,
            AutomationState.WAIT_DESTINATION_FIELD,
            AutomationState.ENTER_DESTINATION,
            AutomationState.WAIT_DESTINATION_CONFIRM,
            AutomationState.WAIT_FARE_SCREEN,
            AutomationState.EXTRACT_FARE,
            AutomationState.RETURN_TO_COMPARE_APP,
            AutomationState.NEXT_APP
        )

        assertEquals(expected, AutomationState.values().toList())
    }
}
