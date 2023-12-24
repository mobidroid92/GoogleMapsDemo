package com.example.googlemapsdemo.utils.managers

import android.location.Location
import androidx.activity.result.IntentSenderRequest

interface AppLocationManager {

    suspend fun requestEnableLocationService(): IntentSenderRequest
    suspend fun getUserLocation(): Location?
    fun isLocationPermissionGranted(): Boolean
    fun isLocationServiceEnabled(): Boolean
    fun onClear()

}