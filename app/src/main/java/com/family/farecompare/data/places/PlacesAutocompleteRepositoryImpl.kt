package com.family.farecompare.data.places

import android.content.Context
import com.family.farecompare.BuildConfig
import com.family.farecompare.domain.places.PlaceSuggestion
import com.family.farecompare.domain.places.PlacesAutocompleteRepository
import com.family.farecompare.domain.places.ResolvedPlace
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlacesAutocompleteRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PlacesAutocompleteRepository {

    override val isAvailable: Boolean = BuildConfig.PLACES_API_KEY.isNotBlank()

    private val placesClient: PlacesClient? by lazy {
        if (!isAvailable) return@lazy null
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.PLACES_API_KEY)
        }
        Places.createClient(context)
    }

    override suspend fun autocomplete(query: String): List<PlaceSuggestion> {
        val client = placesClient ?: return emptyList()
        if (query.isBlank()) return emptyList()

        return try {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build()
            client.findAutocompletePredictions(request).await().autocompletePredictions.map { prediction ->
                PlaceSuggestion(
                    placeId = prediction.placeId,
                    primaryText = prediction.getPrimaryText(null).toString(),
                    secondaryText = prediction.getSecondaryText(null).toString()
                )
            }
        } catch (exception: Exception) {
            emptyList()
        }
    }

    override suspend fun resolvePlace(placeId: String): ResolvedPlace? {
        val client = placesClient ?: return null
        return try {
            val fields = listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG)
            val request = FetchPlaceRequest.builder(placeId, fields).build()
            val response = client.fetchPlace(request).await()
            val place = response.place
            val latLng = place.latLng ?: return null
            ResolvedPlace(
                formattedAddress = place.address.orEmpty(),
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )
        } catch (exception: Exception) {
            null
        }
    }
}
