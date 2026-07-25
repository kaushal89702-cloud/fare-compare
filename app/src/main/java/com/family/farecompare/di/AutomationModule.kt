package com.family.farecompare.di

import com.family.farecompare.data.automation.AppLauncherImpl
import com.family.farecompare.domain.automation.AppLauncher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AutomationModule {

    @Binds
    abstract fun bindAppLauncher(impl: AppLauncherImpl): AppLauncher
}
