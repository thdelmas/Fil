package com.fil.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fil.app.FilApp
import com.fil.app.R
import com.fil.app.analysis.FallDetectionAlgorithm
import com.fil.app.ui.MainActivity
import com.fil.app.ui.emergency.EmergencyCountdownActivity
import kotlin.math.sqrt

/**
 * Always-on foreground service that monitors the accelerometer for fall events.
 *
 * Detection is delegated to [FallDetectionAlgorithm] which is independently testable.
 * When a fall is detected, launches the emergency countdown activity.
 */
class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val algorithm = FallDetectionAlgorithm()

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SensorManager::class.java)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME, // ~20ms, good for fall detection
            )
        }

        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        if (algorithm.processSample(magnitude, now)) {
            onFallDetected()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun onFallDetected() {
        val intent = Intent(this, EmergencyCountdownActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_PEAK_ACCEL, algorithm.lastPeakAccel)
        }
        startActivity(intent)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FallDetectionService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, FilApp.CHANNEL_FALL_DETECTION)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.fall_detection_running))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openApp)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.fil.app.STOP_FALL_DETECTION"
        const val EXTRA_PEAK_ACCEL = "peak_accel"
    }
}
