package com.family.farecompare.domain.automation

/**
 * Explicit states of the per-provider ride-booking automation state
 * machine. [RideAutomationEngine] only ever moves forward one state at a
 * time, and only after verifying the current state actually succeeded -
 * it never advances to [NEXT_APP] while pickup or destination are still
 * unconfirmed.
 */
enum class AutomationState {
    WAIT_APP,
    WAIT_PICKUP_FIELD,
    ENTER_PICKUP,
    WAIT_PICKUP_CONFIRM,
    WAIT_DESTINATION_FIELD,
    ENTER_DESTINATION,
    WAIT_DESTINATION_CONFIRM,
    WAIT_FARE_SCREEN,
    EXTRACT_FARE,
    RETURN_TO_COMPARE_APP,
    NEXT_APP
}
