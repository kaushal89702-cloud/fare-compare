package com.family.farecompare.di

import com.family.farecompare.data.location.DeviceLocationProviderImpl
import com.family.farecompare.data.location.LocationFieldDetectorImpl
import com.family.farecompare.data.location.PickupDetectionRepositoryImpl
import com.family.farecompare.data.location.SuggestionSelectorImpl
import com.family.farecompare.domain.location.DeviceLocationProvider
import com.family.farecompare.domain.location.LocationFieldDetector
import com.family.farecompare.domain.location.PickupDetectionRepository
import com.family.farecompare.domain.location.SuggestionSelector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    abstract fun bindLocationFieldDetector(impl: LocationFieldDetectorImpl): LocationFieldDetector

    @Binds
    abstract fun bindDeviceLocationProvider(impl: DeviceLocationProviderImpl): DeviceLocationProvider

    @Binds
    abstract fun bindPickupDetectionRepository(impl: PickupDetectionRepositoryImpl): PickupDetectionRepository

    @Binds
    abstract fun bindSuggestionSelector(impl: SuggestionSelectorImpl): SuggestionSelector

    @Binds
    abstract fun bindCurrentLocationProvider(
        impl: com.family.farecompare.data.location.CurrentLocationProviderImpl
    ): com.family.farecompare.domain.location.CurrentLocationProvider
}
