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
import com.fil.app.analysis.DriftDetector
import com.fil.app.analysis.GaitDriftResult
import com.fil.app.analysis.GaitFeatureExtractor
import com.fil.app.data.GaitMetrics
import com.fil.app.data.GaitRepository
import com.fil.app.data.GaitSample
import com.fil.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Foreground service that continuously analyzes gait from the accelerometer.
 *
 * Runs alongside FallDetectionService — both register their own sensor listeners.
 * Uses SENSOR_DELAY_GAME (~20ms / 50Hz) which is sufficient for step detection.
 *
 * Periodically computes gait metrics and checks for drift from baseline.
 * Metrics are saved every COMPUTE_INTERVAL_MS (5 minutes) during walking.
 */
class GaitAnalysisService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var repository: GaitRepository
    private val extractor = GaitFeatureExtractor()
    private val driftDetector = DriftDetector()

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var lastComputeTime = 0L

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SensorManager::class.java)
        repository = GaitRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // Save final metrics before stopping
            scope.launch {
                saveCurrentMetrics()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME,
            )
        }

        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        scope.cancel()
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

        extractor.addSample(GaitSample(now, x, y, z, magnitude))

        // Compute and save metrics periodically
        if (now - lastComputeTime > COMPUTE_INTERVAL_MS) {
            lastComputeTime = now
            scope.launch { saveCurrentMetrics() }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private suspend fun saveCurrentMetrics() {
        val metrics = extractor.computeMetrics() ?: return
        repository.saveMetrics(metrics)

        // Check drift against baseline
        val baseline = repository.baseline.first() ?: return
        val drift = driftDetector.computeZScores(metrics, baseline)

        _latestDrift.value = drift
        _latestMetrics.value = metrics

        if (drift.driftDetected) {
            showDriftNotification(drift)
        }
    }

    private fun showDriftNotification(drift: GaitDriftResult) {
        val text = drift.alerts.firstOrNull() ?: "Gait pattern has shifted from your baseline"

        val notification = NotificationCompat.Builder(this, FilApp.CHANNEL_FALL_ALERT)
            .setContentTitle("Gait drift detected")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(DRIFT_NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, GaitAnalysisService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, FilApp.CHANNEL_FALL_DETECTION)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Gait analysis active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openApp)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 2
        const val DRIFT_NOTIFICATION_ID = 3
        const val ACTION_STOP = "com.fil.app.STOP_GAIT_ANALYSIS"

        // Compute metrics every 5 minutes during active monitoring
        private const val COMPUTE_INTERVAL_MS = 5 * 60 * 1000L

        // Observable state for the UI
        private val _latestMetrics = MutableStateFlow<GaitMetrics?>(null)
        val latestMetrics: StateFlow<GaitMetrics?> = _latestMetrics

        private val _latestDrift = MutableStateFlow<GaitDriftResult?>(null)
        val latestDrift: StateFlow<GaitDriftResult?> = _latestDrift
    }
}
