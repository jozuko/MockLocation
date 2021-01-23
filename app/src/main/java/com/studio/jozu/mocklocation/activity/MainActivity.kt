package com.studio.jozu.mocklocation.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.SupportMapFragment
import com.studio.jozu.mocklocation.*
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_LOCATION_PERMISSIONS = 1001
        private val MOVE_TYPES = arrayOf("徒歩", "自転車", "車")
    }

    private val viewModel: MainViewModel by lazy {
        MainViewModel(this)
    }

    private val mockLocationServiceIntent by lazy {
        Intent(this, MockLocationService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkLocationPermission()
        setUpGoogleMap()
        setUpMoveType()

        viewHomeButton.setOnClickListener { viewModel.resetStartPosition(MainViewModel.HOME_LAT_LNG) }
        viewTokyoButton.setOnClickListener { viewModel.resetStartPosition(MainViewModel.TOKYO_STATION_LAT_LNG) }
        viewStartButton.setOnClickListener { onClickStartButton() }
        viewStopButton.setOnClickListener { onClickStopButton() }
    }

    private fun setUpGoogleMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.viewMapFragment) as? SupportMapFragment
        viewModel.getMapAsync(mapFragment)
    }

    private fun checkLocationPermission() {
        if (viewModel.hasLocationPermission() == PermissionGrantedType.GRANTED) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(MainViewModel.LOCATION_PERMISSIONS, REQUEST_CODE_LOCATION_PERMISSIONS)
        } else {
            finishPermissionDenied()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSIONS) {
            if (viewModel.isPermissionGranted(grantResults)) {
                return
            }
            finishPermissionDenied()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun finishPermissionDenied() {
        Toast.makeText(this, "Please LocationPermission granted.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun onClickStartButton() {
        val serviceIntent = mockLocationServiceIntent
        serviceIntent.putExtra(MockLocationService.START_LOCATION_LAT, viewModel.startLatLng.latitude)
        serviceIntent.putExtra(MockLocationService.START_LOCATION_LNG, viewModel.startLatLng.longitude)
        serviceIntent.putExtra(MockLocationService.MOVE_TYPE, viewMoveType.selectedItemPosition)
        serviceIntent.putExtra(MockLocationService.RADIUS, viewRadius.text.toString().toIntOrNull() ?: 100)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startService(serviceIntent)
        } else {
            startForegroundService(serviceIntent)
        }
    }

    private fun onClickStopButton() {
        EventBus.getDefault().post(MockLocationStopEvent())
    }

    private fun setUpMoveType() {
        viewMoveType.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, MOVE_TYPES)
    }
}
