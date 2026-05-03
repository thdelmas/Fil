package com.fil.app.data

import org.junit.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for baseline computation logic.
 * Since GaitRepository.computeBaseline is private and needs Android context,
 * we test the same math independently here to verify the baseline contract.
 */
class GaitBaselineTest {

    @Test
    fun `baseline from stable 14 days`() {
        val metrics = (1..14).map { day ->
            GaitMetrics(
                date = "2026-04-${day.toString().padStart(2, '0')}",
                stepCount = 5000,
                avgStrideTimeMs = 500.0 + (day % 3) * 2.0, // small natural variation
                strideTimeVariability = 30.0,
                asymmetryRatio = 1.02,
                avgCadenceStepsPerMin = 120.0,
                walkingSegments = 20,
            )
        }

        val baseline = computeBaseline(metrics)
        assertNotNull(baseline)
        assertEquals(14, baseline.days)
        assertTrue(baseline.avgStrideTimeMs in 499.0..505.0)
        assertTrue(baseline.sdStrideTimeMs > 0, "SD should be > 0 with varying data")
        assertTrue(baseline.sdStrideTimeMs < 5.0, "SD should be small for stable gait")
    }

    @Test
    fun `baseline requires minimum 5 days`() {
        val tooFew = (1..4).map { day ->
            GaitMetrics(
                date = "2026-04-${day.toString().padStart(2, '0')}",
                stepCount = 5000,
                avgStrideTimeMs = 500.0,
                strideTimeVariability = 30.0,
                asymmetryRatio = 1.02,
                avgCadenceStepsPerMin = 120.0,
                walkingSegments = 20,
            )
        }
        assertNull(computeBaseline(tooFew))

        val justEnough = tooFew + GaitMetrics(
            date = "2026-04-05",
            stepCount = 5000,
            avgStrideTimeMs = 500.0,
            strideTimeVariability = 30.0,
            asymmetryRatio = 1.02,
            avgCadenceStepsPerMin = 120.0,
            walkingSegments = 20,
        )
        assertNotNull(computeBaseline(justEnough))
    }

    @Test
    fun `baseline detects increasing stride time trend`() {
        // Simulate gradual worsening: stride time increases over 14 days
        val worsening = (1..14).map { day ->
            GaitMetrics(
                date = "2026-04-${day.toString().padStart(2, '0')}",
                stepCount = 5000,
                avgStrideTimeMs = 480.0 + day * 5.0, // 485 to 550
                strideTimeVariability = 30.0 + day * 1.0,
                asymmetryRatio = 1.02 + day * 0.005,
                avgCadenceStepsPerMin = 125.0 - day * 1.5,
                walkingSegments = 20,
            )
        }

        val baseline = computeBaseline(worsening)
        assertNotNull(baseline)
        // High SD indicates the baseline period itself was unstable
        assertTrue(baseline.sdStrideTimeMs > 10.0, "Worsening trend should show high SD")
    }

    @Test
    fun `baseline with identical data has zero SD`() {
        val identical = (1..7).map { day ->
            GaitMetrics(
                date = "2026-04-${day.toString().padStart(2, '0')}",
                stepCount = 5000,
                avgStrideTimeMs = 500.0,
                strideTimeVariability = 30.0,
                asymmetryRatio = 1.0,
                avgCadenceStepsPerMin = 120.0,
                walkingSegments = 20,
            )
        }

        val baseline = computeBaseline(identical)
        assertNotNull(baseline)
        assertEquals(0.0, baseline.sdStrideTimeMs, 0.001)
        assertEquals(0.0, baseline.sdAsymmetry, 0.001)
    }

    // --- Replicate the baseline computation logic from GaitRepository ---

    private fun computeBaseline(metrics: List<GaitMetrics>): GaitBaseline? {
        if (metrics.size < 5) return null

        return GaitBaseline(
            days = metrics.size,
            avgStrideTimeMs = metrics.map { it.avgStrideTimeMs }.average(),
            sdStrideTimeMs = sd(metrics.map { it.avgStrideTimeMs }),
            avgVariability = metrics.map { it.strideTimeVariability }.average(),
            sdVariability = sd(metrics.map { it.strideTimeVariability }),
            avgAsymmetry = metrics.map { it.asymmetryRatio }.average(),
            sdAsymmetry = sd(metrics.map { it.asymmetryRatio }),
            avgCadence = metrics.map { it.avgCadenceStepsPerMin }.average(),
            sdCadence = sd(metrics.map { it.avgCadenceStepsPerMin }),
        )
    }

    private fun sd(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        return sqrt(variance)
    }
}
