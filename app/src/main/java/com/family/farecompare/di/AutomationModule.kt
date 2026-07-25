package com.family.farecompare.di

import com.family.farecompare.data.automation.UberProvider
import com.family.farecompare.domain.automation.RideAppProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Registers every supported [RideAppProvider]. Adding Ola or Rapido support
 * in a future phase only requires one new @Binds @IntoSet entry here plus a
 * new provider class - no other file needs to change.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AutomationModule {

    @Binds
    @IntoSet
    abstract fun bindUberProvider(impl: UberProvider): RideAppProvider
}
