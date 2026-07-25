package com.family.farecompare.di

import com.family.farecompare.data.fare.FareExtractorImpl
import com.family.farecompare.domain.fare.FareExtractor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class FareModule {

    @Binds
    abstract fun bindFareExtractor(impl: FareExtractorImpl): FareExtractor

    @Binds
    abstract fun bindNoRidesAvailableDetector(
        impl: com.family.farecompare.data.fare.NoRidesAvailableDetectorImpl
    ): com.family.farecompare.domain.fare.NoRidesAvailableDetector
}
