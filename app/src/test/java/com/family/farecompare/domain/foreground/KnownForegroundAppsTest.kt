package com.family.farecompare.domain.foreground

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KnownForegroundAppsTest {

    @Test
    fun `resolves display name for every known ride provider package`() {
        assertEquals("Uber", KnownForegroundApps.displayNameFor("com.ubercab"))
        assertEquals("Ola", KnownForegroundApps.displayNameFor("com.olacabs.customer"))
        assertEquals("Rapido", KnownForegroundApps.displayNameFor("com.rapido.passenger"))
    }

    @Test
    fun `resolves display name for FareCompare and WhatsApp`() {
        assertEquals("FareCompare", KnownForegroundApps.displayNameFor("com.family.farecompare"))
        assertEquals("WhatsApp", KnownForegroundApps.displayNameFor("com.whatsapp"))
    }

    @Test
    fun `returns null for unknown package`() {
        assertNull(KnownForegroundApps.displayNameFor("com.example.somerandomapp"))
    }
}
