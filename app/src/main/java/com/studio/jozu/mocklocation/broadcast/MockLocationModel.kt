package com.studio.jozu.mocklocation.broadcast

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import kotlin.math.roundToInt

/**
 * Created by r.mori on 2021/01/23.
 * Copyright (c) 2021 rei-frontier. All rights reserved.
 */
data class MockLocationModel(
    val speed: Float?,
    val repeat: Int?,
    val reverse: Boolean?,
    val delay: Long?,
    val coordinates: List<MockLocation>?
) {
    data class MockLocation(
        val latitude: Double?,
        val longitude: Double?
    )

    companion object {
        fun getModel(jsonString: String?): MockLocationModel {
            return jsonString?.let {
                GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create()
                    .fromJson(jsonString, MockLocationModel::class.java)
            } ?: MockLocationModel(null, null, null, null, null)
        }
    }


    fun getAllCoordinates(): List<LatLng> {
        val distancePerSeconds = getDistanceMeterPerSeconds()
        if (distancePerSeconds == 0F) {
            return emptyList()
        }

        val trimmedCoordinates = trimmedCoordinates()
        if (trimmedCoordinates.isEmpty()) {
            return emptyList()
        }

        val coordinates = mutableListOf<LatLng>()
        for (index in 0 until trimmedCoordinates.count()) {
            if (index == 0) {
                coordinates.add(trimmedCoordinates[index])
                continue
            }

            val preLocation = trimmedCoordinates[index - 1]
            val currentLocation = trimmedCoordinates[index]

            val distance = preLocation.distanceTo(currentLocation)
            val divisionCount = (distance / distancePerSeconds).toInt()
            val divisionLat = ((currentLocation.latitude - preLocation.latitude) / divisionCount).round7()
            val divisionLng = ((currentLocation.longitude - preLocation.longitude) / divisionCount).round7()

            for (i in 0 until divisionCount) {
                val newLatLng = LatLng(preLocation.latitude + (divisionLat * i), preLocation.longitude + (divisionLng * i))
                coordinates.add(newLatLng)
            }
            coordinates.lastOrNull()?.let { lastLocation ->
                if (!lastLocation.same(currentLocation)) {
                    coordinates.add(currentLocation)
                }
            }
        }

        return coordinates
    }

    private fun trimmedCoordinates(): List<LatLng> {
        return coordinates?.mapNotNull {
            if (it.latitude != null && it.longitude != null) {
                LatLng(it.latitude.round7(), it.longitude.round7())
            } else {
                null
            }
        } ?: emptyList()
    }

    private fun getDistanceMeterPerSeconds(): Float {
        if (speed == null || speed == 0F) {
            return 0F
        }
        return (speed * 1000) / 3600
    }

    private fun LatLng.distanceTo(latLng: LatLng): Float {
        val floatArray = floatArrayOf(0F, 0F, 0F)
        Location.distanceBetween(this.latitude, this.longitude, latLng.latitude, latLng.longitude, floatArray)
        return floatArray[0]
    }

    private fun LatLng.same(latLng: LatLng): Boolean {
        return this.latitude.round7() == latLng.latitude.round7() && this.longitude.round7() == latLng.longitude.round7()
    }

    private fun Double.round7(): Double {
        return (this * 10000000).roundToInt().toDouble() / 10000000
    }
}

