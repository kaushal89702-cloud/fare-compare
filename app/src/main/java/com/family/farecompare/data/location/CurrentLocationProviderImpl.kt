package com.family.farecompare.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.family.farecompare.domain.location.CurrentLocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

private const val LOCATION_FIX_TIMEOUT_MS = 8_000L

class CurrentLocationProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CurrentLocationProvider {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override suspend fun getCurrentAddress(): String? {
        if (!hasLocationPermission()) return null

        val location = withTimeoutOrNull(LOCATION_FIX_TIMEOUT_MS) { requestFreshLocation() } ?: return null
        return reverseGeocode(location.latitude, location.longitude)
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private suspend fun requestFreshLocation(): android.location.Location? {
        return try {
            val currentLocationRequest = com.google.android.gms.location.CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .build()
            fusedLocationClient.getCurrentLocation(currentLocationRequest, null).await()
        } catch (exception: SecurityException) {
            null
        } catch (exception: Exception) {
            null
        }
    }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                reverseGeocodeAsync(geocoder, latitude, longitude)
            } else {
                reverseGeocodeSync(geocoder, latitude, longitude)
            }
        } catch (exception: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocodeSync(geocoder: Geocoder, latitude: Double, longitude: Double): String? {
        return geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.getAddressLine(0)
    }

    private suspend fun reverseGeocodeAsync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): String? = suspendCancellableCoroutine { continuation ->
        try {
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                if (continuation.isActive) continuation.resume(addresses.firstOrNull()?.getAddressLine(0))
            }
        } catch (exception: Exception) {
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
