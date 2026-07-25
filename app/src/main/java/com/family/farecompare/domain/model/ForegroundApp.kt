package com.family.farecompare.domain.model

/**
 * Represents the application currently in the foreground on the device,
 * as detected by the accessibility service.
 */
data class ForegroundApp(
    val packageName: String,
    val displayName: String
)
