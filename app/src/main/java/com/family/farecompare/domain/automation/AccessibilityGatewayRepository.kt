package com.family.farecompare.domain.automation

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Holds a reference to the currently running accessibility service (if the
 * user has enabled it) so the automation engine can query the live window
 * content on demand. The service attaches itself when connected and
 * detaches on destroy; if the service is not running, [currentRootNode]
 * simply returns null rather than throwing.
 */
interface AccessibilityGatewayRepository {
    fun attach(gateway: AccessibilityGateway)
    fun detach(gateway: AccessibilityGateway)
    fun currentRootNode(): AccessibilityNodeInfo?
}
