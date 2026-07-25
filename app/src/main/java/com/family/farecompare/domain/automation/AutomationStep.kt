package com.family.farecompare.domain.automation

/**
 * Progress steps emitted while attempting to launch a ride provider app and
 * confirm it reached the foreground.
 */
sealed class AutomationStep {
    data object NotInstalled : AutomationStep()
    data object Launching : AutomationStep()
    data object Waiting : AutomationStep()
    data object Success : AutomationStep()
    data object Timeout : AutomationStep()
}
