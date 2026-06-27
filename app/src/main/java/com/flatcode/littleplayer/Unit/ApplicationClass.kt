package com.flatcode.littleplayer.Unit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class ApplicationClass : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // Modern version checks look much cleaner in Kotlin
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel1 = NotificationChannel(
                CHANNEL_ID_1,
                "Channel(1)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel 1 Desc.."
            }

            val channel2 = NotificationChannel(
                CHANNEL_ID_2,
                "Channel(2)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel 2 Desc.."
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel1)
            notificationManager?.createNotificationChannel(channel2)
        }
    }

    // Companion object holds your static constants
    companion object {
        const val CHANNEL_ID_1 = "channel1"
        const val CHANNEL_ID_2 = "channel2"
        const val ACTION_PREVIOUS = "actionprevious"
        const val ACTION_NEXT = "actionnext"
        const val ACTION_PLAY = "actionplay"
    }
}
