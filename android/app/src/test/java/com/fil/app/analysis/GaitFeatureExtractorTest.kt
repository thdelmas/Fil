package com.fil.app.analysis

import com.fil.app.data.GaitSample
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GaitFeatureExtractorTest {

    private lateinit var extractor: GaitFeatureExtractor

    @Before
    fun setUp() {
        extractor = GaitFeatureExtractor()
    }

    @Test
    fun `no metrics with insufficient steps`() {
        // Feed only a few samples — not enough for meaningful metrics
        repeat(5) { i ->
            extractor.addSample(sample(i * 20L, 9.8f))
        }
        assertNull(extractor.computeMetrics())
    }

    @Test
    fun `detects steps from simulated walking pattern`() {
        // Simulate walking: alternating high (step impact) and low (swing) acceleration
        // at ~2 steps/second (500ms per step), which is normal walking cadence
        val stepIntervalMs = 500L
        val totalSteps = 40
        var time = 0L

        for (step in 0 until totalSteps) {
            // Swing phase: low acceleration (~1g) for most of the interval
            val swingSamples = 20 // 20 samples at 20ms = 400ms swing
            for (s in 0 until swingSamples) {
                extractor.addSample(sample(time, 9.8f))
                time += 20
            }
            // Impact phase: high acceleration spike (~1.5g)
            val impactSamples = 5 // 5 samples at 20ms = 100ms impact
            for (s in 0 until impactSamples) {
                val mag = if (s == 2) 14.7f else 11.0f // Peak in the middle
                extractor.addSample(sample(time, mag))
                time += 20
            }
        }

        val metrics = extractor.computeMetrics()
        assertNotNull(metrics)
        assertTrue(metrics.stepCount >= 20, "Expected at least 20 steps, got ${metrics.stepCount}")
        assertTrue(metrics.avgCadenceStepsPerMin > 60, "Cadence should be > 60 steps/min")
        assertTrue(metrics.avgCadenceStepsPerMin < 180, "Cadence should be < 180 steps/min")
    }

    @Test
    fun `symmetric gait produces asymmetry ratio near 1`() {
        // Simulate perfectly regular steps at 500ms intervals
        val metrics = simulateRegularWalking(stepIntervalMs = 500, steps = 40)
        assertNotNull(metrics)
        assertTrue(
            metrics.asymmetryRatio in 0.95..1.05,
            "Symmetric gait should have asymmetry ~1.0, got ${metrics.asymmetryRatio}"
        )
    }

    @Test
    fun `asymmetric gait detected with alternating intervals`() {
        // Simulate limping: alternating 400ms and 600ms step intervals
        var time = 0L
        val steps = 40

        for (step in 0 until steps) {
            val interval = if (step % 2 == 0) 400L else 600L
            val swingDuration = interval - 100 // Impact takes ~100ms
            val swingSamples = (swingDuration / 20).toInt()

            for (s in 0 until swingSamples) {
                extractor.addSample(sample(time, 9.8f))
                time += 20
            }
            // Impact peak
            for (s in 0 until 5) {
                val mag = if (s == 2) 14.7f else 11.0f
                extractor.addSample(sample(time, mag))
                time += 20
            }
        }

        val metrics = extractor.computeMetrics()
        assertNotNull(metrics)
        assertTrue(
            metrics.asymmetryRatio > 1.05,
            "Asymmetric gait should have ratio > 1.05, got ${metrics.asymmetryRatio}"
        )
    }

    @Test
    fun `reset clears all state`() {
        simulateRegularWalking(stepIntervalMs = 500, steps = 30)
        assertNotNull(extractor.computeMetrics())

        extractor.reset()
        assertNull(extractor.computeMetrics())
    }

    @Test
    fun `stride variability is low for regular gait`() {
        val metrics = simulateRegularWalking(stepIntervalMs = 500, steps = 40)
        assertNotNull(metrics)
        // Regular walking should have low stride time variability
        assertTrue(
            metrics.strideTimeVariability < 100.0,
            "Regular gait variability should be low, got ${metrics.strideTimeVariability}"
        )
    }

    @Test
    fun `debounce rejects steps closer than 200ms`() {
        var time = 0L
        // Rapid impacts at 100ms — should be debounced
        repeat(50) {
            extractor.addSample(sample(time, 9.8f))
            time += 50
            extractor.addSample(sample(time, 14.7f))
            time += 50
        }
        val metrics = extractor.computeMetrics()
        // Should either be null (too few valid steps) or have reduced count
        if (metrics != null) {
            assertTrue(metrics.stepCount < 50, "Debounce should reject rapid steps")
        }
    }

    // --- Helpers ---

    private fun simulateRegularWalking(stepIntervalMs: Long, steps: Int): com.fil.app.data.GaitMetrics? {
        var time = 0L
        for (step in 0 until steps) {
            val swingDuration = stepIntervalMs - 100
            val swingSamples = (swingDuration / 20).toInt()

            for (s in 0 until swingSamples) {
                extractor.addSample(sample(time, 9.8f))
                time += 20
            }
            for (s in 0 until 5) {
                val mag = if (s == 2) 14.7f else 11.0f
                extractor.addSample(sample(time, mag))
                time += 20
            }
        }
        return extractor.computeMetrics()
    }

    private fun sample(timestamp: Long, magnitude: Float): GaitSample {
        // Distribute magnitude across axes (mostly vertical)
        val y = magnitude * 0.95f
        val x = magnitude * 0.1f
        val z = magnitude * 0.1f
        return GaitSample(
            timestamp = timestamp,
            x = x,
            y = y,
            z = z,
            magnitude = magnitude,
        )
    }
}
