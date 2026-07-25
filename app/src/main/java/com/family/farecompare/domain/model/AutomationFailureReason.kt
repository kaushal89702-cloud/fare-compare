package com.family.farecompare.domain.model

/**
 * Every way a single provider's automation run can fail to produce a fare.
 * Kept exhaustive and sealed so every call site (ViewModel, UI) is forced to
 * handle each case explicitly rather than falling back to a generic error.
 */
sealed class AutomationFailureReason {
    data object AccessibilityDisabled : AutomationFailureReason()
    data object AppNotInstalled : AutomationFailureReason()
    data object LaunchTimeout : AutomationFailureReason()
    data object PickupFieldNotFound : AutomationFailureReason()
    data object DestinationFieldNotFound : AutomationFailureReason()
    data object SuggestionNotFound : AutomationFailureReason()
    data object FareNotFoundBeforeTimeout : AutomationFailureReason()
    data object NoRidesAvailable : AutomationFailureReason()
    data object NoInternet : AutomationFailureReason()
    data class Unexpected(val message: String) : AutomationFailureReason()
}
