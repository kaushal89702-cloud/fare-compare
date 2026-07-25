package com.family.farecompare.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.family.farecompare.domain.location.DeviceLocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class DeviceLocationProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceLocationProvider {

    override suspend fun getLastKnownAddress(): String? {
        if (!hasLocationPermission()) return null
        val location = getLastKnownLocation() ?: return null
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

    private fun getLastKnownLocation(): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        return try {
            locationManager.getProviders(true)
                .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
                .maxByOrNull { it.time }
        } catch (exception: SecurityException) {
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
                if (continuation.isActive) {
                    continuation.resume(addresses.firstOrNull()?.getAddressLine(0))
                }
            }
        } catch (exception: Exception) {
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
