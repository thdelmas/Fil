package com.fil.app.analysis

import com.fil.app.data.GaitBaseline
import com.fil.app.data.GaitMetrics
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DriftDetectorTest {

    private lateinit var detector: DriftDetector

    private val stableBaseline = GaitBaseline(
        days = 14,
        avgStrideTimeMs = 500.0,
        sdStrideTimeMs = 20.0,
        avgVariability = 30.0,
        sdVariability = 5.0,
        avgAsymmetry = 1.02,
        sdAsymmetry = 0.02,
        avgCadence = 120.0,
        sdCadence = 5.0,
    )

    @Before
    fun setUp() {
        detector = DriftDetector()
    }

    @Test
    fun `normal day produces no drift`() {
        val today = metricsForDay(
            strideTime = 505.0,
            variability = 32.0,
            asymmetry = 1.03,
            cadence = 118.0,
        )
        val result = detector.computeZScores(today, stableBaseline)

        assertFalse(result.driftDetected)
        assertTrue(result.alerts.isEmpty())
        assertTrue(result.compositeScore < DriftDetector.COMPOSITE_ALERT_THRESHOLD)
    }

    @Test
    fun `stride time drift triggers alert`() {
        // Stride time 3 SD above baseline (500 + 3*20 = 560)
        val today = metricsForDay(strideTime = 565.0)
        val result = detector.computeZScores(today, stableBaseline)

        assertTrue(result.driftDetected)
        assertTrue(result.alerts.any { "Stride time" in it })
        assertTrue(result.strideTimeZ > 2.0)
    }

    @Test
    fun `asymmetry drift triggers alert`() {
        // Asymmetry 3 SD above baseline (1.02 + 3*0.02 = 1.08)
        val today = metricsForDay(asymmetry = 1.10)
        val result = detector.computeZScores(today, stableBaseline)

        assertTrue(result.driftDetected)
        assertTrue(result.alerts.any { "asymmetry" in it.lowercase() })
    }

    @Test
    fun `cadence drop triggers alert`() {
        // Cadence 3 SD below baseline (120 - 3*5 = 105)
        val today = metricsForDay(cadence = 100.0)
        val result = detector.computeZScores(today, stableBaseline)

        assertTrue(result.driftDetected)
        assertTrue(result.alerts.any { "cadence" in it.lowercase() })
    }

    @Test
    fun `variability spike triggers alert`() {
        // Variability 3 SD above (30 + 3*5 = 45)
        val today = metricsForDay(variability = 48.0)
        val result = detector.computeZScores(today, stableBaseline)

        assertTrue(result.driftDetected)
        assertTrue(result.alerts.any { "variability" in it.lowercase() })
    }

    @Test
    fun `composite alert when multiple metrics drift moderately`() {
        // Each metric 1.2 SD off — no single one hits 2.0, but composite > 1.5
        val today = metricsForDay(
            strideTime = 500.0 + 1.2 * 20.0,  // z=1.2
            variability = 30.0 + 1.2 * 5.0,    // z=1.2
            asymmetry = 1.02 + 1.2 * 0.02,     // z=1.2
            cadence = 120.0 - 1.2 * 5.0,        // z=-1.2
        )
        val result = detector.computeZScores(today, stableBaseline)

        // Composite RMS of [1.2, 1.2, 1.2, 1.2] = 1.2, which is below 1.5
        // So this should NOT trigger — verify the math is correct
        assertEquals(1.2, result.compositeScore, 0.1)
        // 1.2 < 1.5 so no composite alert, and no single metric > 2.0
        assertFalse(result.driftDetected)
    }

    @Test
    fun `composite alert triggers when coordinated drift is stronger`() {
        // Each metric 1.6 SD off — composite RMS = 1.6 > 1.5
        val today = metricsForDay(
            strideTime = 500.0 + 1.6 * 20.0,
            variability = 30.0 + 1.6 * 5.0,
            asymmetry = 1.02 + 1.6 * 0.02,
            cadence = 120.0 - 1.6 * 5.0,
        )
        val result = detector.computeZScores(today, stableBaseline)

        assertTrue(result.compositeScore > DriftDetector.COMPOSITE_ALERT_THRESHOLD)
        assertTrue(result.driftDetected)
        assertTrue(result.alerts.any { "Multiple" in it || "together" in it })
    }

    @Test
    fun `zero standard deviation returns zero z-score`() {
        val flatBaseline = stableBaseline.copy(sdStrideTimeMs = 0.0)
        val today = metricsForDay(strideTime = 999.0)
        val result = detector.computeZScores(today, flatBaseline)

        assertEquals(0.0, result.strideTimeZ, 0.001)
    }

    @Test
    fun `multiple alerts can fire simultaneously`() {
        val today = metricsForDay(
            strideTime = 565.0,  // 3+ SD
            asymmetry = 1.10,    // 4 SD
            cadence = 100.0,     // 4 SD
        )
        val result = detector.computeZScores(today, stableBaseline)

        assertTrue(result.driftDetected)
        assertTrue(result.alerts.size >= 2, "Multiple metrics drifting should produce multiple alerts")
    }

    // --- Helpers ---

    private fun metricsForDay(
        strideTime: Double = 500.0,
        variability: Double = 30.0,
        asymmetry: Double = 1.02,
        cadence: Double = 120.0,
    ) = GaitMetrics(
        date = "2026-04-13",
        stepCount = 5000,
        avgStrideTimeMs = strideTime,
        strideTimeVariability = variability,
        asymmetryRatio = asymmetry,
        avgCadenceStepsPerMin = cadence,
        walkingSegments = 20,
    )
}
