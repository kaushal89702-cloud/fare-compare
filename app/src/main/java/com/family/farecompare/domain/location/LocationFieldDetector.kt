package com.family.farecompare.domain.location

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Provider-agnostic pickup location field detector.
 *
 * Implementations search the accessibility node tree of whichever
 * ride-hailing app is currently in the foreground and score candidate
 * editable fields using generic signals (editability, hint/text/content
 * description keywords, resource id naming, nearby labels, on-screen
 * position, node depth). No provider-specific resource IDs or layouts are
 * ever hardcoded, so the same implementation works for Uber, Ola, Rapido,
 * or any future provider without modification.
 *
 * Note: [AccessibilityNodeInfo] is referenced directly here (rather than an
 * abstracted domain type) because tree traversal is inherently tied to the
 * Android accessibility framework; introducing an abstraction layer over it
 * would add complexity without a corresponding benefit for this app.
 */
interface LocationFieldDetector {
    fun detectPickupField(rootNode: AccessibilityNodeInfo): DetectedField?
}
