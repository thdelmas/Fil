package com.fil.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class FilApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val fallChannel = NotificationChannel(
            CHANNEL_FALL_DETECTION,
            getString(R.string.fall_detection_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent notification while fall detection is active"
        }

        val alertChannel = NotificationChannel(
            CHANNEL_FALL_ALERT,
            "Fall Alert",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Emergency alert when a fall is detected"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
        }

        manager.createNotificationChannel(fallChannel)
        manager.createNotificationChannel(alertChannel)
    }

    companion object {
        const val CHANNEL_FALL_DETECTION = "fall_detection"
        const val CHANNEL_FALL_ALERT = "fall_alert"
    }
}
