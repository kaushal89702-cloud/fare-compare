package com.family.farecompare.domain.foreground

/**
 * Package name to display name mapping for applications FareCompare
 * explicitly recognizes. Any package not listed here is treated as unknown
 * and resolved via the device's PackageManager instead.
 */
object KnownForegroundApps {

    private val KNOWN_PACKAGE_DISPLAY_NAMES: Map<String, String> = mapOf(
        "com.ubercab" to "Uber",
        "com.olacabs.customer" to "Ola",
        "com.rapido.passenger" to "Rapido",
        "com.family.farecompare" to "FareCompare",
        "com.whatsapp" to "WhatsApp"
    )

    fun displayNameFor(packageName: String): String? = KNOWN_PACKAGE_DISPLAY_NAMES[packageName]
}
