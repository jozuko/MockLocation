package com.studio.jozu.mocklocation.activity

import android.content.Intent
import android.util.Log
import androidx.work.Operation
import androidx.work.WorkManager
import com.google.android.gms.maps.model.LatLng
import com.studio.jozu.mocklocation.ForegroundServiceBase
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by r.mori on 2021/01/18.
 * Copyright (c) 2021 rei-frontier. All rights reserved.
 */
class MockLocationService : ForegroundServiceBase() {
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_NAME = "MockLocationUpdating..."

        const val START_LOCATION_LAT = "com.studio.jozu.mocklocation.activity.MockLocationService.START_LAT"
        const val START_LOCATION_LNG = "com.studio.jozu.mocklocation.activity.MockLocationService.START_LNG"
        const val MOVE_TYPE = "com.studio.jozu.mocklocation.activity.MockLocationService.MOVE_TYPE"
        const val RADIUS = "com.studio.jozu.mocklocation.activity.MockLocationService.RADIUS"
    }

    override val notificationId: Int
        get() = NOTIFICATION_ID

    override val notificationChannelName: String
        get() = NOTIFICATION_CHANNEL_NAME

    private var startId: Int? = null

    private var mockLocationOperation: Operation? = null

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("MockService", "onStart $startId")

        if (this.startId == null) {
            val startLat = intent?.getDoubleExtra(START_LOCATION_LAT, 0.0) ?: 0.0
            val startLng = intent?.getDoubleExtra(START_LOCATION_LNG, 0.0) ?: 0.0
            val moveType = intent?.getIntExtra(MOVE_TYPE, 0) ?: 0
            val radius = intent?.getIntExtra(RADIUS, 100) ?: 100
            this.startId = startId
            startMockLocationSet(LatLng(startLat, startLng), moveType, radius)
        }

        return START_NOT_STICKY
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceiveStop(@Suppress("UNUSED_PARAMETER") event: MockLocationStopEvent) {
        startId?.let {
            stopMockLocationSet()
            startId = null
            stopSelf(it)
        }
    }

    private fun startMockLocationSet(startPoint: LatLng, moveType: Int, radius: Int) {
        if (mockLocationOperation != null) {
            return
        }
        val workRequest = MockLocationWorker.createWorkRequest(startPoint.latitude, startPoint.longitude, moveType, radius)
        WorkManager.getInstance(this).cancelAllWork()
        mockLocationOperation = WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun stopMockLocationSet() {
        mockLocationOperation ?: return
        WorkManager.getInstance(this).cancelAllWork()
        mockLocationOperation = null
    }
}