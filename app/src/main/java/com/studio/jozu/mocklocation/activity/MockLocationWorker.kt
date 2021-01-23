package com.studio.jozu.mocklocation.activity

import android.content.Context
import android.location.Location
import android.os.SystemClock
import android.util.Log
import androidx.work.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

/**
 * Created by r.mori on 2021/01/18.
 * Copyright (c) 2021 rei-frontier. All rights reserved.
 */
class MockLocationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    companion object {
        private const val LOCATION_PROVIDER = "mock_provider"
        private const val WALK_DISTANCE = 0.000007      // (1mくらい) 時速3.6km
        private const val BIKE_DISTANCE = 0.000042      // (6mくらい) 時速21.6km
        private const val CAR_DISTANCE = 0.000105      // (15mくらい) 時速54km
        private const val radius = 100
        const val START_LOCATION_LAT = "com.studio.jozu.mocklocation.activity.MockLocationWorker.START_LAT"
        const val START_LOCATION_LNG = "com.studio.jozu.mocklocation.activity.MockLocationWorker.START_LNG"
        const val MOVE_TYPE = "com.studio.jozu.mocklocation.activity.MockLocationWorker.MOVE_TYPE"
        const val RADIUS = "com.studio.jozu.mocklocation.activity.MockLocationWorker.RADIUS"

        fun createWorkRequest(startLat: Double, startLng: Double, moveType: Int, radius: Int): WorkRequest {
            val inputData = workDataOf(START_LOCATION_LAT to startLat, START_LOCATION_LNG to startLng, MOVE_TYPE to moveType, RADIUS to RADIUS)
            return OneTimeWorkRequestBuilder<MockLocationWorker>()
                .setInputData(inputData)
                .build()
        }
    }

    enum class MoveType(val distance: Double) {
        WALK(WALK_DISTANCE), BIKE(BIKE_DISTANCE), CAR(CAR_DISTANCE);

        companion object {
            fun getType(value: Int): MoveType {
                return values().find { it.ordinal == value } ?: WALK
            }
        }
    }

    enum class DirectionType {
        CENTER_TO_CENTER_NORTH,
        CENTER_NORTH_TO_EAST,
        NORTH_EAST_TO_SOUTH_EAST,
        SOUTH_EAST_TO_SOUTH_WEST,
        SOUTH_WEST_TO_NORTH_WEST,
        NORTH_WEST_TO_CENTER_NORTH,
        CENTER_NORTH_TO_CENTER;
    }

    private var locationClient: FusedLocationProviderClient? = LocationServices.getFusedLocationProviderClient(context)
    private val startLat = params.inputData.getDouble(START_LOCATION_LAT, 0.0)
    private val startLng = params.inputData.getDouble(START_LOCATION_LNG, 0.0)
    private val moveDistance = MoveType.getType(params.inputData.getInt(MOVE_TYPE, 0)).distance
    private val radius = params.inputData.getInt(RADIUS, 100)

    override fun onStopped() {
        locationClient?.setMockMode(false)
        super.onStopped()
    }

    override fun doWork(): Result {
        var lat = startLat
        var lng = startLng
        var result: Result? = null
        var currentDirection = DirectionType.CENTER_TO_CENTER_NORTH

        // とりあえず10秒寝る
        Thread.sleep(10 * 1000)

        while (true) {
            // 1秒間隔で実行
            Thread.sleep(1 * 1000)
            if (isStopped || locationClient == null) {
                break
            }

            // 位置情報を設定する
            val mockLocation = Location(LOCATION_PROVIDER)
            mockLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            mockLocation.time = System.currentTimeMillis()
            mockLocation.accuracy = 1f
            mockLocation.latitude = lat
            mockLocation.longitude = lng
            locationClient?.setMockMode(true)
            locationClient?.setMockLocation(mockLocation)
                ?.addOnFailureListener { error ->
                    Log.d("MockLocation", "mock failed. ${error.localizedMessage}")
                    result = Result.failure()
                }

            // 失敗したら終了
            if (result != null) {
                break
            }

            // 次の緯度経度を設定する(1mくらい)
            if (isArriveTargetPoint(lat, lng, currentDirection)) {
                currentDirection = getNextDirection(currentDirection)
            }
            val nextPoint = getNextPoint(lat, lng, currentDirection)
            lat = nextPoint.latitude
            lng = nextPoint.longitude
        }

        return result ?: Result.success()
    }

    private fun isArriveTargetPoint(lat: Double, lng: Double, direction: DirectionType): Boolean {
        val targetPoint = getTargetPoint(direction)
        val isArrive = (lat.round7() == targetPoint.latitude.round7() && lng.round7() == targetPoint.longitude.round7())

        Log.d("MockLocation", "[$lat, $lng], [${targetPoint.latitude}, ${targetPoint.longitude}] $direction => $isArrive")
        return isArrive
    }

    private fun getNextPoint(lat: Double, lng: Double, direction: DirectionType): LatLng {
        val north = startLat + moveDistance * radius
        val south = startLat - moveDistance * radius
        val east = startLng - moveDistance * radius
        val west = startLng + moveDistance * radius
        val toNorth = (lat + moveDistance).round7()
        val toSouth = (lat - moveDistance).round7()
        val toEast = (lng - moveDistance).round7()
        val toWest = (lng + moveDistance).round7()

        return when (direction) {
            DirectionType.CENTER_TO_CENTER_NORTH -> {
                LatLng(toNorth, startLng)
            }
            DirectionType.CENTER_NORTH_TO_EAST -> {
                LatLng(north, toEast)
            }
            DirectionType.NORTH_EAST_TO_SOUTH_EAST -> {
                LatLng(toSouth, east)
            }
            DirectionType.SOUTH_EAST_TO_SOUTH_WEST -> {
                LatLng(south, toWest)
            }
            DirectionType.SOUTH_WEST_TO_NORTH_WEST -> {
                LatLng(toNorth, west)
            }
            DirectionType.NORTH_WEST_TO_CENTER_NORTH -> {
                LatLng(north, toEast)
            }
            DirectionType.CENTER_NORTH_TO_CENTER -> {
                LatLng(toSouth, startLng)
            }
        }

    }

    private fun getTargetPoint(direction: DirectionType): LatLng {
        val north = startLat + moveDistance * radius
        val south = startLat - moveDistance * radius
        val east = startLng - moveDistance * radius
        val west = startLng + moveDistance * radius

        return when (direction) {
            DirectionType.CENTER_TO_CENTER_NORTH -> {
                LatLng(north, startLng)
            }
            DirectionType.CENTER_NORTH_TO_EAST -> {
                LatLng(north, east)
            }
            DirectionType.NORTH_EAST_TO_SOUTH_EAST -> {
                LatLng(south, east)
            }
            DirectionType.SOUTH_EAST_TO_SOUTH_WEST -> {
                LatLng(south, west)
            }
            DirectionType.SOUTH_WEST_TO_NORTH_WEST -> {
                LatLng(north, west)
            }
            DirectionType.NORTH_WEST_TO_CENTER_NORTH -> {
                LatLng(north, startLng)
            }
            DirectionType.CENTER_NORTH_TO_CENTER -> {
                LatLng(startLat, startLng)
            }
        }
    }

    private fun getNextDirection(currentDirection: DirectionType): DirectionType {
        val position = currentDirection.ordinal
        return if (position >= DirectionType.values().count() - 1) {
            DirectionType.values()[0]
        } else {
            DirectionType.values()[position + 1]
        }
    }

    private fun Double.round7(): Double {
        return (this * 10000000).roundToInt().toDouble() / 10000000
    }
}
