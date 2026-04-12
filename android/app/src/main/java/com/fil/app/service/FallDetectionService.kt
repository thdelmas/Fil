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
import com.fil.app.ui.MainActivity
import com.fil.app.ui.emergency.EmergencyCountdownActivity
import kotlin.math.sqrt

/**
 * Always-on foreground service that monitors the accelerometer for fall events.
 *
 * Detection algorithm:
 * 1. Free-fall phase: acceleration magnitude drops below FREEFALL_THRESHOLD (~0.5g)
 * 2. Impact phase: within IMPACT_WINDOW_MS, magnitude spikes above IMPACT_THRESHOLD (~3g)
 * 3. Post-fall stillness: within STILLNESS_WINDOW_MS, magnitude stays near 1g (lying still)
 *
 * When all three phases are detected, launches the emergency countdown activity.
 */
class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var freefallDetectedAt: Long = 0
    private var impactDetectedAt: Long = 0
    private var lastPeakAccel: Float = 0f

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

        // Phase 1: Free-fall detection (magnitude drops close to 0g)
        if (magnitude < FREEFALL_THRESHOLD) {
            freefallDetectedAt = now
            return
        }

        // Phase 2: Impact detection (high-g spike after free-fall)
        if (freefallDetectedAt > 0 && magnitude > IMPACT_THRESHOLD) {
            val timeSinceFreefall = now - freefallDetectedAt
            if (timeSinceFreefall in 1..IMPACT_WINDOW_MS) {
                impactDetectedAt = now
                lastPeakAccel = magnitude
                return
            }
        }

        // Phase 3: Post-fall stillness (near 1g, person lying on ground)
        if (impactDetectedAt > 0) {
            val timeSinceImpact = now - impactDetectedAt
            if (timeSinceImpact > STILLNESS_DELAY_MS && timeSinceImpact < STILLNESS_WINDOW_MS) {
                val nearOneG = magnitude in STILLNESS_LOW..STILLNESS_HIGH
                if (nearOneG) {
                    onFallDetected()
                    return
                }
            }
            // Reset if stillness window expired without detection
            if (timeSinceImpact > STILLNESS_WINDOW_MS) {
                resetState()
            }
        }

        // Reset freefall if too much time passed without impact
        if (freefallDetectedAt > 0 && now - freefallDetectedAt > IMPACT_WINDOW_MS) {
            freefallDetectedAt = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun onFallDetected() {
        resetState()

        val intent = Intent(this, EmergencyCountdownActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_PEAK_ACCEL, lastPeakAccel)
        }
        startActivity(intent)
    }

    private fun resetState() {
        freefallDetectedAt = 0
        impactDetectedAt = 0
        lastPeakAccel = 0f
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

        // Thresholds tuned for MS patients (less aggressive movement than healthy adults)
        private const val FREEFALL_THRESHOLD = 4.9f   // ~0.5g in m/s^2
        private const val IMPACT_THRESHOLD = 29.4f     // ~3g in m/s^2
        private const val IMPACT_WINDOW_MS = 500L      // Max time between freefall and impact
        private const val STILLNESS_DELAY_MS = 500L    // Wait before checking stillness
        private const val STILLNESS_WINDOW_MS = 3000L  // Must be still within 3s of impact
        private const val STILLNESS_LOW = 8.8f         // ~0.9g
        private const val STILLNESS_HIGH = 10.8f       // ~1.1g
    }
}
