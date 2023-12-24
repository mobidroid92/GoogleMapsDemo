package com.example.googlemapsdemo.presentation

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.googlemapsdemo.R
import com.example.googlemapsdemo.databinding.ActivityMainBinding
import com.example.googlemapsdemo.utils.AppUtils
import com.example.googlemapsdemo.utils.AppUtils.bitmapDescriptorFromVector
import com.example.googlemapsdemo.utils.managers.AppLocationManagerImpl
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.awaitMap
import com.google.maps.android.ktx.cameraMoveEvents
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    private lateinit var googleMap: GoogleMap

    private var routePolyline: Polyline? = null
    private var destinationMarker: Marker? = null

    private var isUserInitiatedMove = false

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isPermissionGranted ->
        if (isPermissionGranted) {
            checkLocationService()
        }
    }

    private val enableLocationServiceRequest = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            checkLocationPermission()
        } else {
            showToast(getString(R.string.location_not_enabled_msg))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setup()
        listeners()
        observers()
    }

    private fun setup() {
        setSupportActionBar(binding.toolbar)
        setupMap()
    }

    private fun listeners() {
        binding.routeButton.setOnClickListener {
            val route = viewModel.getRoute(googleMap.cameraPosition.target)
            if (route.isNotEmpty()) {
                resetViews()
                addViews(route)
            }
        }
    }

    private fun resetViews() {
        binding.wantedLocation.isVisible = false
        binding.routeButton.isEnabled = false
        routePolyline?.remove()
        destinationMarker?.remove()
    }

    private fun addViews(route: List<LatLng>) {
        val polylineOptions = PolylineOptions().addAll(route)
            .color(Color.BLACK)
            .width(10f)
        routePolyline = googleMap.addPolyline(polylineOptions)
        destinationMarker = googleMap.addMarker {
            position(route.last())
            title(getString(R.string.destination_location))
            icon(
                bitmapDescriptorFromVector(this@MainActivity, R.drawable.ic_location_marker)
            )
        }
    }

    private fun observers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.currentUserLocation.collect { latLng ->
                        handleCurrentUserLocation(latLng)
                    }
                }
                launch {
                    viewModel.requestEnableLocationService.collect { intentSenderRequest ->
                        enableLocationServiceRequest.launch(intentSenderRequest)
                    }
                }
                launch {
                    viewModel.toastMsg.collect {
                        showToast(it)
                    }
                }
            }
        }
    }

    private fun setupMap() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    initMap()
                    checkLocationPermission()
                    handleCameraMoveEvent()
                }
            }
        }
    }

    private suspend fun initMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        googleMap = mapFragment.awaitMap()
    }

    private suspend fun handleCameraMoveEvent() {
        googleMap.cameraMoveEvents().collect {
            if (isUserInitiatedMove && binding.wantedLocation.isVisible.not()) {
                binding.wantedLocation.isVisible = true
                binding.routeButton.isEnabled = true
            }
        }
    }

    private fun handleCurrentUserLocation(latLng: LatLng?) {
        latLng?.let { latLongNotNull ->
            isUserInitiatedMove = false
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(latLongNotNull, DEFAULT_ZOOM_LEVEL),
                object : GoogleMap.CancelableCallback {
                    override fun onCancel() {
                        isUserInitiatedMove = true
                    }

                    override fun onFinish() {
                        isUserInitiatedMove = true
                    }
                }
            )
            googleMap.addMarker {
                position(latLongNotNull)
                title(getString(R.string.my_current_location))
                icon(
                    bitmapDescriptorFromVector(this@MainActivity, R.drawable.ic_location_marker)
                )
            }
        }
    }

    private fun checkLocationPermission() {
        if (viewModel.isLocationPermissionGranted()) {
            checkLocationService()
        } else {
            locationPermissionRequest.launch(AppLocationManagerImpl.LOCATION_PERMISSION)
        }
    }

    private fun checkLocationService() {
        if (viewModel.isLocationServiceEnabled()) {
            viewModel.getUserLocation()
        } else {
            viewModel.requestEnableLocationService()
        }
    }

    private fun showToast(msg: String?) {
        msg?.let {
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val DEFAULT_ZOOM_LEVEL = 11f
    }
}