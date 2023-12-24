package com.example.googlemapsdemo.presentation

import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.googlemapsdemo.BuildConfig
import com.example.googlemapsdemo.utils.managers.AppLocationManager
import com.google.android.gms.maps.model.LatLng
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appLocationManager: AppLocationManager
) : ViewModel() {

    private val _requestEnableLocationService = MutableSharedFlow<IntentSenderRequest>()
    val requestEnableLocationService = _requestEnableLocationService.asSharedFlow()

    private val _currentUserLocation = MutableStateFlow<LatLng?>(null)
    val currentUserLocation = _currentUserLocation.asStateFlow()

    private val _toastMsg = MutableSharedFlow<String?>()
    val toastMsg = _toastMsg.asSharedFlow()

    fun requestEnableLocationService() {
        viewModelScope.launch {
            try {
                _requestEnableLocationService.emit(
                    appLocationManager.requestEnableLocationService()
                )
            } catch (e: Exception) {
                _toastMsg.emit(e.message)
            }
        }
    }

    fun getUserLocation() {
        viewModelScope.launch {
            try {
                appLocationManager.getUserLocation()?.let { locationNotNull ->
                    _currentUserLocation.update {
                        LatLng(locationNotNull.latitude, locationNotNull.longitude)
                    }
                }
            } catch (e: Exception) {
                _toastMsg.emit(e.message)
            }
        }
    }

    fun getRoute(targetLatLng: LatLng): List<LatLng> {
        return try {
            val directionsResult = requestDirections(targetLatLng)
            parseDirections(directionsResult)
        } catch (e: Exception) {
            viewModelScope.launch {
                _toastMsg.emit(e.message)
            }
            emptyList()
        }
    }

    private fun requestDirections(targetLatLng: LatLng): DirectionsResult {
        val geoApiContext = GeoApiContext.Builder()
            .apiKey(BuildConfig.MAPS_API_KEY)
            .build()

        val originLatLng = "${currentUserLocation.value?.latitude},${currentUserLocation.value?.longitude}"
        val destinationLatLng = "${targetLatLng.latitude},${targetLatLng.longitude}"

        return DirectionsApi.getDirections(geoApiContext, originLatLng, destinationLatLng).await()
    }

    private fun parseDirections(directionsResult: DirectionsResult): List<LatLng> {
        val route = ArrayList<LatLng>()
        try {
            directionsResult.routes.firstOrNull()?.legs?.forEach { leg ->
                leg.steps.forEach { step ->
                    if(step.steps.isNullOrEmpty().not()) {
                        step.steps.forEach { innerStep ->
                            innerStep.polyline.decodePath().forEach {
                                route.add(LatLng(it.lat,it.lng))
                            }
                        }
                    } else {
                        step.polyline.decodePath().forEach {
                            route.add(LatLng(it.lat,it.lng))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _toastMsg.emit(e.message)
            }
        }
        return route
    }

    fun isLocationPermissionGranted(): Boolean {
        return appLocationManager.isLocationPermissionGranted()
    }

    fun isLocationServiceEnabled(): Boolean {
        return appLocationManager.isLocationServiceEnabled()
    }

    override fun onCleared() {
        appLocationManager.onClear()
        super.onCleared()
    }
}