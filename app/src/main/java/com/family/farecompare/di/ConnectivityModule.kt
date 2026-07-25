package com.family.farecompare.di

import com.family.farecompare.data.connectivity.InternetConnectivityCheckerImpl
import com.family.farecompare.domain.connectivity.InternetConnectivityChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {

    @Binds
    abstract fun bindInternetConnectivityChecker(impl: InternetConnectivityCheckerImpl): InternetConnectivityChecker
}
