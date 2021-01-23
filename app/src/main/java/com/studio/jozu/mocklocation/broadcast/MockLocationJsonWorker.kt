package com.studio.jozu.mocklocation.broadcast

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
class MockLocationJsonWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    companion object {
        private const val LOCATION_PROVIDER = "mock_provider"
        const val EXTRA_JSON = "com.studio.jozu.mocklocation.broadcast.MockLocationJsonWorker.JSON"

        fun createWorkRequest(jsonString: String?): WorkRequest {
            val inputData = workDataOf(EXTRA_JSON to jsonString)
            return OneTimeWorkRequestBuilder<MockLocationJsonWorker>()
                .setInputData(inputData)
                .build()
        }
    }

    private var locationClient: FusedLocationProviderClient? = LocationServices.getFusedLocationProviderClient(context)
    private val jsonString = params.inputData.getString(EXTRA_JSON)

    override fun onStopped() {
        locationClient?.setMockMode(false)
        super.onStopped()
    }

    override fun doWork(): Result {
        val model = MockLocationModel.getModel(jsonString)
        val repeatCount = model.repeat ?: 0
        val reverse = model.reverse ?: true
        val delay = model.delay ?: 10 * 1000

        val allCoordinates = model.getAllCoordinates()
        val reversedCoordinates = if (reverse) allCoordinates.reversed() else emptyList()
        if (allCoordinates.isEmpty()) {
            Log.d("MockLocationJson", "coordinate is empty")
            while (true) {
                if (isStopped) {
                    return Result.success()
                }
                Thread.sleep(1000)
            }
        }

        // delay
        Thread.sleep(delay)

        var result: Result? = null
        var repeatedCount = 0

        while (repeatCount == -1 || repeatCount <= repeatedCount) {
            // 順番通りに移動
            allCoordinates.forEach { latLng ->
                // 停止条件を満たせば終了
                if (isStopped || locationClient == null || result != null) {
                    return result ?: Result.success()
                }

                // 位置情報を設定する
                val mockLocation = getMockLocation(latLng)
                locationClient?.setMockMode(true)
                locationClient?.setMockLocation(mockLocation)
                    ?.addOnFailureListener { error ->
                        Log.d("MockLocation", "mock failed. ${error.localizedMessage}")
                        result = Result.failure()
                    }

                // 1秒間隔で実行
                Thread.sleep(1 * 1000)
            }
            // 停止条件を満たせば終了
            if (isStopped || locationClient == null || result != null) {
                return result ?: Result.success()
            }

            // 逆順に移動
            reversedCoordinates.forEach { latLng ->
                // 停止条件を満たせば終了
                if (isStopped || locationClient == null || result != null) {
                    return result ?: Result.success()
                }

                // 位置情報を設定する
                val mockLocation = getMockLocation(latLng)
                locationClient?.setMockMode(true)
                locationClient?.setMockLocation(mockLocation)
                    ?.addOnFailureListener { error ->
                        Log.d("MockLocation", "mock failed. ${error.localizedMessage}")
                        result = Result.failure()
                    }

                // 1秒間隔で実行
                Thread.sleep(1 * 1000)
            }
            // 停止条件を満たせば終了
            if (isStopped || locationClient == null || result != null) {
                return result ?: Result.success()
            }

            // 繰り返し回数インクリメント
            repeatedCount++
        }

        // 外部から停止要求が来るまで動き続ける
        while (true) {
            if (isStopped) {
                return result ?: Result.success()
            }
            Thread.sleep(1000)
        }
    }

    private fun getMockLocation(latLng: LatLng): Location {
        val mockLocation = Location(LOCATION_PROVIDER)
        mockLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        mockLocation.time = System.currentTimeMillis()
        mockLocation.accuracy = 1f
        mockLocation.latitude = latLng.latitude
        mockLocation.longitude = latLng.longitude
        return mockLocation
    }
}
