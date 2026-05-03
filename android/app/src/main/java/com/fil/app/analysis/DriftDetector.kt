package com.fil.app.analysis

import com.fil.app.data.GaitBaseline
import com.fil.app.data.GaitMetrics
import kotlin.math.abs

/**
 * Detects drift from personal baseline using z-score analysis.
 *
 * A z-score > 2.0 on any metric means that day's value is more than 2 standard
 * deviations from the person's 14-day baseline — a statistically notable shift.
 *
 * The composite drift score combines multiple metrics so that coordinated
 * small shifts (gait + cadence + asymmetry all drifting) are caught even
 * when no single metric exceeds the threshold alone.
 */
class DriftDetector {

    /**
     * Compute per-metric z-scores for today's gait vs personal baseline.
     */
    fun computeZScores(today: GaitMetrics, baseline: GaitBaseline): GaitDriftResult {
        val strideTimeZ = zScore(today.avgStrideTimeMs, baseline.avgStrideTimeMs, baseline.sdStrideTimeMs)
        val variabilityZ = zScore(today.strideTimeVariability, baseline.avgVariability, baseline.sdVariability)
        val asymmetryZ = zScore(today.asymmetryRatio, baseline.avgAsymmetry, baseline.sdAsymmetry)
        val cadenceZ = zScore(today.avgCadenceStepsPerMin, baseline.avgCadence, baseline.sdCadence)

        // Composite: root mean square of z-scores (all directions count)
        val composite = rms(listOf(strideTimeZ, variabilityZ, asymmetryZ, cadenceZ))

        val alerts = mutableListOf<String>()
        if (abs(strideTimeZ) > ALERT_THRESHOLD) alerts.add("Stride time shifted (z=${formatZ(strideTimeZ)})")
        if (abs(variabilityZ) > ALERT_THRESHOLD) alerts.add("Stride variability shifted (z=${formatZ(variabilityZ)})")
        if (abs(asymmetryZ) > ALERT_THRESHOLD) alerts.add("Gait asymmetry shifted (z=${formatZ(asymmetryZ)})")
        if (abs(cadenceZ) > ALERT_THRESHOLD) alerts.add("Walking cadence shifted (z=${formatZ(cadenceZ)})")
        if (composite > COMPOSITE_ALERT_THRESHOLD && alerts.isEmpty()) {
            alerts.add("Multiple gait metrics drifting together")
        }

        return GaitDriftResult(
            strideTimeZ = strideTimeZ,
            variabilityZ = variabilityZ,
            asymmetryZ = asymmetryZ,
            cadenceZ = cadenceZ,
            compositeScore = composite,
            alerts = alerts,
            driftDetected = alerts.isNotEmpty(),
        )
    }

    private fun zScore(value: Double, mean: Double, sd: Double): Double {
        if (sd == 0.0) return 0.0
        return (value - mean) / sd
    }

    private fun rms(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sumSquares = values.sumOf { it * it }
        return kotlin.math.sqrt(sumSquares / values.size)
    }

    private fun formatZ(z: Double): String = "%.1f".format(z)

    companion object {
        // Alert if any single metric exceeds 2 standard deviations
        const val ALERT_THRESHOLD = 2.0

        // Alert on composite if coordinated drift exceeds 1.5 (even if no single metric hits 2.0)
        const val COMPOSITE_ALERT_THRESHOLD = 1.5
    }
}

data class GaitDriftResult(
    val strideTimeZ: Double,
    val variabilityZ: Double,
    val asymmetryZ: Double,
    val cadenceZ: Double,
    val compositeScore: Double,
    val alerts: List<String>,
    val driftDetected: Boolean,
)
