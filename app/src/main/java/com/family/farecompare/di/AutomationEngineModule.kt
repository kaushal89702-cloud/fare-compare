package com.family.farecompare.di

import com.family.farecompare.data.automation.AccessibilityGatewayRepositoryImpl
import com.family.farecompare.data.automation.WindowContentEventBusImpl
import com.family.farecompare.domain.automation.AccessibilityGatewayRepository
import com.family.farecompare.domain.automation.WindowContentEventBus
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AutomationEngineModule {

    @Binds
    abstract fun bindAccessibilityGatewayRepository(
        impl: AccessibilityGatewayRepositoryImpl
    ): AccessibilityGatewayRepository

    @Binds
    abstract fun bindWindowContentEventBus(impl: WindowContentEventBusImpl): WindowContentEventBus
}
