package com.studio.jozu.mocklocation.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import org.greenrobot.eventbus.EventBus

/**
 * Created by r.mori on 2021/01/23.
 * Copyright (c) 2021 rei-frontier. All rights reserved.
 */
class LocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return
        val action = intent.action ?: return
        when (action) {
            "ACTION_START_MOCK_LOCATION" -> {
                onReceiveStart(context, intent.extras)
            }
            "ACTION_STOP_MOCK_LOCATION" -> {
                onReceiveStop()
            }
        }
    }

    private fun onReceiveStart(context: Context, extras: Bundle?) {
        val jsonString = extras?.getString("mock") ?: ""
        Log.d("LocationReceiver", "onReceiveStart, $jsonString")
        //val jsonString = "{\"speed\":4.0,\"repeat\":-1,\"reverse\":true,\"delay\":5000,\"coordinates\":[{\"latitude\":36.325928,\"longitude\":137.832105},{\"latitude\":36.326135, \"longitude\":137.832173},{\"latitude\":36.326731,\"longitude\":137.831840},{\"latitude\":36.327327,\"longitude\":137.831556},{\"latitude\":36.327716,\"longitude\":137.831245}]}"
        val serviceIntent = Intent(context, MockLocationJsonService::class.java)
        serviceIntent.putExtra(MockLocationJsonService.EXTRA_JSON, jsonString)
        context.startService(serviceIntent)
    }

    private fun onReceiveStop() {
        Log.d("LocationReceiver", "onReceiveStop")
        EventBus.getDefault().post(MockLocationJsonStopEvent())
    }
}