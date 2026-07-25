package com.family.farecompare.domain.automation

import org.junit.Assert.assertTrue
import org.junit.Test

/** A minimal fake exercising the default (empty) hint lists on [RideAppProvider]. */
private class FakeProvider : RideAppProvider {
    override val displayName: String = "Fake"
    override val packageName: String = "com.example.fake"
    override fun isInstalled(): Boolean = true
    override fun launch(): Boolean = true
    override suspend fun waitUntilForeground(timeoutMs: Long): Boolean = true
}

class RideAppProviderHintsTest {

    @Test
    fun `providers default to no extra field hints unless overridden`() {
        val provider = FakeProvider()
        assertTrue(provider.pickupFieldHints.isEmpty())
        assertTrue(provider.destinationFieldHints.isEmpty())
    }
}
