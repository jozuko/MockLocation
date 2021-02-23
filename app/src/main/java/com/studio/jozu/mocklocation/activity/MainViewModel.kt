package com.studio.jozu.mocklocation.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Created by r.mori on 2021/01/18.
 * Copyright (c) 2021 rei-frontier. All rights reserved.
 */
class MainViewModel(private val activity: MainActivity) {
    companion object {
        val LOCATION_PERMISSIONS = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        val TOKYO_STATION_LAT_LNG = LatLng(35.6809591, 139.7673068)
        val HOME_LAT_LNG = LatLng(36.325704, 137.831932)
    }

    private var googleMap: GoogleMap? = null
    var startLatLng = HOME_LAT_LNG
        private set

    fun getMapAsync(supportMapFragment: SupportMapFragment?) {
        supportMapFragment ?: return
        supportMapFragment.getMapAsync {
            this.googleMap = it
            googleMap?.isMyLocationEnabled = true
            googleMap?.uiSettings?.let { uiSettings ->
                uiSettings.isZoomControlsEnabled = true
                uiSettings.isScrollGesturesEnabled = true
                uiSettings.isMyLocationButtonEnabled = true
                uiSettings.isRotateGesturesEnabled = true
                uiSettings.isZoomGesturesEnabled = true
                uiSettings.isIndoorLevelPickerEnabled = true
                uiSettings.isCompassEnabled = true
                uiSettings.isTiltGesturesEnabled = true
            }

            // 開始地点にズーム
            resetStartPosition(startLatLng)

            googleMap?.setOnMapClickListener { clickedLatLng -> onClickMap(clickedLatLng) }
        }
    }

    fun hasLocationPermission(): PermissionGrantedType {
        if (hasPermission(LOCATION_PERMISSIONS)) {
            return PermissionGrantedType.GRANTED
        }

        return PermissionGrantedType.DENIED
    }

    private fun hasPermission(permissions: Array<String>): Boolean {
        val grantResults = permissions.map { permission -> ContextCompat.checkSelfPermission(activity, permission) }
            .toIntArray()

        return isPermissionGranted(grantResults)
    }

    fun isPermissionGranted(grantResults: IntArray): Boolean {
        grantResults.forEach { result ->
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun zoomToLatLng(latLng: LatLng) {
        val cameraPosition = CameraPosition.Builder()
            .target(latLng)
            .zoom(16f)
            .build()

        googleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    fun resetStartPosition(latLng: LatLng) {
        val markerOptions = MarkerOptions()
            .position(latLng)
            .title("ここから移動開始")

        startLatLng = latLng
        googleMap?.let { map ->
            map.clear()
            map.addMarker(markerOptions).showInfoWindow()
        }
        zoomToLatLng(latLng)
    }

    private fun onClickMap(clickedLatLng: LatLng) {
        resetStartPosition(clickedLatLng)
    }
}