package com.flatcode.littleplayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import io.selimdawa.multicolors.MultiColorManager

@HiltAndroidApp
class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        MultiColorManager.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.apply {
                createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID_1,
                        "Channel(1)",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply { description = "Channel 1 Desc.." })
                createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID_2,
                        "Channel(2)",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply { description = "Channel 2 Desc.." })
            }
        }
    }

    companion object {
        const val CHANNEL_ID_1 = "channel1"
        const val CHANNEL_ID_2 = "channel2"
        const val ACTION_PREVIOUS = "actionprevious"
        const val ACTION_NEXT = "actionnext"
        const val ACTION_PLAY = "actionplay"
    }
}