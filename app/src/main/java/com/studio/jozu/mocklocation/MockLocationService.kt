package com.studio.jozu.mocklocation

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.Operation
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by r.mori on 2021/01/18.
 * Copyright (c) 2021 rei-frontier. All rights reserved.
 */
class MockLocationService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_NAME = "MockLocationUpdating..."

        const val START_LOCATION_LAT = "com.studio.jozu.mocklocation.MockLocationService.START_LAT"
        const val START_LOCATION_LNG = "com.studio.jozu.mocklocation.MockLocationService.START_LNG"
        const val MOVE_TYPE = "com.studio.jozu.mocklocation.MockLocationService.MOVE_TYPE"
        const val RADIUS = "com.studio.jozu.mocklocation.MockLocationService.RADIUS"
    }

    private var startId: Int? = null

    private var mockLocationOperation: Operation? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

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
        startForeground()
        Log.d("MockSercice", "onStart $startId")

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

//region foreground

    private fun startForeground() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notification = createNotification() ?: return
        startForeground(NOTIFICATION_ID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Nullable
    private fun createNotification(): Notification? {
        val channelId = "RFLForegroundService"
        val chan = NotificationChannel(channelId, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)

        chan.lightColor = Color.BLUE
        chan.importance = NotificationManager.IMPORTANCE_NONE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(chan)

        val activityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT)


        @Suppress("DEPRECATION")
        return NotificationCompat.Builder(this)
            .setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(NOTIFICATION_CHANNEL_NAME)
            .setContentText("")
            .setOngoing(true)
            .setWhen(System.currentTimeMillis())
            .setChannelId(channelId)
            .setContentIntent(pendingIntent)
            .build()
    }

//endregion foreground
}