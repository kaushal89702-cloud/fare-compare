package com.family.farecompare.di

import com.family.farecompare.data.common.ResourceProviderImpl
import com.family.farecompare.domain.common.ResourceProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonModule {

    @Binds
    abstract fun bindResourceProvider(impl: ResourceProviderImpl): ResourceProvider
}
