package com.family.farecompare.di

import com.family.farecompare.data.places.PlacesAutocompleteRepositoryImpl
import com.family.farecompare.domain.places.PlacesAutocompleteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PlacesModule {

    @Binds
    abstract fun bindPlacesAutocompleteRepository(
        impl: PlacesAutocompleteRepositoryImpl
    ): PlacesAutocompleteRepository
}
