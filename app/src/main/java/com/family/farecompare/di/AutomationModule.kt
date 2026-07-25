package com.family.farecompare.di

import com.family.farecompare.data.automation.AccessibilityActionExecutorImpl
import com.family.farecompare.data.automation.OlaProvider
import com.family.farecompare.data.automation.RapidoProvider
import com.family.farecompare.data.automation.UberProvider
import com.family.farecompare.domain.automation.AccessibilityActionExecutor
import com.family.farecompare.domain.automation.RideAppProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Registers every supported [RideAppProvider]. Adding a new provider only
 * requires one new @Binds @IntoSet entry here plus a new provider class -
 * no other file needs to change.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AutomationModule {

    @Binds
    @IntoSet
    abstract fun bindUberProvider(impl: UberProvider): RideAppProvider

    @Binds
    @IntoSet
    abstract fun bindOlaProvider(impl: OlaProvider): RideAppProvider

    @Binds
    @IntoSet
    abstract fun bindRapidoProvider(impl: RapidoProvider): RideAppProvider

    @Binds
    abstract fun bindAccessibilityActionExecutor(impl: AccessibilityActionExecutorImpl): AccessibilityActionExecutor

    @Binds
    abstract fun bindScrollHelper(
        impl: com.family.farecompare.data.automation.ScrollHelperImpl
    ): com.family.farecompare.domain.automation.ScrollHelper
}
