package com.studio.jozu.mocklocation.broadcast

import android.content.Intent
import android.util.Log
import androidx.work.Operation
import androidx.work.WorkManager
import com.studio.jozu.mocklocation.ForegroundServiceBase
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by r.mori on 2021/01/18.
 * Copyright (c) 2021 rei-frontier. All rights reserved.
 */
class MockLocationJsonService : ForegroundServiceBase() {
    companion object {
        const val EXTRA_JSON = "com.studio.jozu.mocklocation.broadcast.MockLocationJsonService.JSON"
    }

    override val notificationId: Int
        get() = 1002

    override val notificationChannelName: String
        get() = "MockLocation from Broadcast"

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
        Log.d("MockLocationJson", "onStart")

        val jsonString = intent?.extras?.getString(EXTRA_JSON)
        startMockLocationSet(jsonString)

        return START_NOT_STICKY
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceiveStop(@Suppress("UNUSED_PARAMETER") event: MockLocationJsonStopEvent) {
        Log.d("MockLocationJson", "onReceiveStop")
        stopMockLocationSet()
        stopSelf()
    }

    private fun startMockLocationSet(jsonString: String?) {
        stopMockLocationSet()

        val workRequest = MockLocationJsonWorker.createWorkRequest(jsonString)
        mockLocationOperation = WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun stopMockLocationSet() {
        mockLocationOperation ?: return
        WorkManager.getInstance(this).cancelAllWork()
        mockLocationOperation = null
    }
}