package com.studio.jozu.mocklocation

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.Operation
import androidx.work.WorkManager
import com.google.android.gms.maps.model.LatLng
import com.studio.jozu.mocklocation.R
import com.studio.jozu.mocklocation.activity.MainActivity
import com.studio.jozu.mocklocation.activity.MockLocationStopEvent
import com.studio.jozu.mocklocation.activity.MockLocationWorker
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by r.mori on 2021/01/18.
 * Copyright (c) 2021 rei-frontier. All rights reserved.
 */
abstract class ForegroundServiceBase : Service() {
    abstract val notificationId: Int
    abstract val notificationChannelName: String

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground()

        return START_NOT_STICKY
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notification = createNotification() ?: return
        startForeground(notificationId, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Nullable
    private fun createNotification(): Notification? {
        val channelId = "RFLForegroundService"
        val chan = NotificationChannel(channelId, this::class.java.name, NotificationManager.IMPORTANCE_HIGH)

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
            .setContentTitle(notificationChannelName)
            .setContentText("")
            .setOngoing(true)
            .setWhen(System.currentTimeMillis())
            .setChannelId(channelId)
            .setContentIntent(pendingIntent)
            .build()
    }
}