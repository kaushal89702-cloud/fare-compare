package com.family.farecompare.di

import com.family.farecompare.data.foreground.ForegroundAppRepositoryImpl
import com.family.farecompare.domain.foreground.ForegroundAppRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ForegroundAppModule {

    @Binds
    abstract fun bindForegroundAppRepository(
        impl: ForegroundAppRepositoryImpl
    ): ForegroundAppRepository
}
