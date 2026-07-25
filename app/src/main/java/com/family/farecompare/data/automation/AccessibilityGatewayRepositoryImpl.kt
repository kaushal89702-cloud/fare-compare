package com.family.farecompare.data.automation

import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.automation.AccessibilityGateway
import com.family.farecompare.domain.automation.AccessibilityGatewayRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityGatewayRepositoryImpl @Inject constructor() : AccessibilityGatewayRepository {

    @Volatile
    private var activeGateway: AccessibilityGateway? = null

    override fun attach(gateway: AccessibilityGateway) {
        activeGateway = gateway
    }

    override fun detach(gateway: AccessibilityGateway) {
        if (activeGateway === gateway) {
            activeGateway = null
        }
    }

    override fun currentRootNode(): AccessibilityNodeInfo? = activeGateway?.currentRootNode()
}
