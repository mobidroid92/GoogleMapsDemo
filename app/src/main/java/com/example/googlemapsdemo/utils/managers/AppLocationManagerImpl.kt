package com.example.googlemapsdemo.utils.managers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

class AppLocationManagerImpl @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) : AppLocationManager {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(applicationContext)

    override suspend fun requestEnableLocationService(): IntentSenderRequest {
        return suspendCancellableCoroutine { continuation ->
            val locationSettingsRequest = LocationSettingsRequest.Builder()
                .addLocationRequest(
                    LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        LOCATION_UPDATE_INTERVAL
                    ).build()
                )
                .setAlwaysShow(true)
                .build()

            LocationServices.getSettingsClient(applicationContext)
                .checkLocationSettings(locationSettingsRequest)
                .addOnFailureListener { exception ->
                    if (exception is ResolvableApiException) {
                        val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution)
                            .build()
                        continuation.resume(intentSenderRequest)
                    } else {
                        continuation.resumeWithException(exception)
                    }
                }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getUserLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val currentLocationRequest = CurrentLocationRequest.Builder().build()
            fusedLocationClient.getCurrentLocation(currentLocationRequest, null)
                .addOnSuccessListener { location -> continuation.resume(location) }
                .addOnFailureListener { exception -> continuation.resumeWithException(exception) }
        }
    }

    override fun isLocationPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            applicationContext, LOCATION_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun isLocationServiceEnabled(): Boolean {
        val lm = applicationContext.getSystemService(LocationManager::class.java)
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override fun onClear() {
        fusedLocationClient.flushLocations()
    }

    companion object {
        const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
        const val LOCATION_UPDATE_INTERVAL = 1000L
    }
}