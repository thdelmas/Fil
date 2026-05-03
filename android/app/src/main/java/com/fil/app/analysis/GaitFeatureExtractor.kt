package com.fil.app.analysis

import com.fil.app.data.GaitMetrics
import com.fil.app.data.GaitSample
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Extracts gait features from raw accelerometer data.
 *
 * Algorithm:
 * 1. Detect walking segments (sustained rhythmic acceleration patterns)
 * 2. Find steps via peak detection on the vertical acceleration axis
 * 3. Compute stride times, asymmetry, cadence from step intervals
 */
class GaitFeatureExtractor {

    private val samples = mutableListOf<GaitSample>()
    private val stepTimestamps = mutableListOf<Long>()
    private var walkingSegmentCount = 0

    // Buffered window for peak detection
    private val windowBuffer = ArrayDeque<GaitSample>(WINDOW_SIZE)
    private var lastStepTimestamp = 0L

    fun addSample(sample: GaitSample) {
        samples.add(sample)
        windowBuffer.addLast(sample)
        if (windowBuffer.size > WINDOW_SIZE) {
            windowBuffer.removeFirst()
        }

        if (windowBuffer.size == WINDOW_SIZE) {
            detectStep()
        }

        // Trim old samples (keep last 10 seconds for context)
        val cutoff = sample.timestamp - 10_000
        while (samples.isNotEmpty() && samples.first().timestamp < cutoff) {
            samples.removeAt(0)
        }
    }

    /**
     * Peak detection: the middle sample in the window is a step if:
     * - Its magnitude is higher than all neighbors (local maximum)
     * - It exceeds the step threshold
     * - Enough time has passed since the last step (debounce)
     */
    private fun detectStep() {
        val mid = WINDOW_SIZE / 2
        val list = windowBuffer.toList()
        val candidate = list[mid]

        // Must exceed minimum step acceleration
        if (candidate.magnitude < STEP_THRESHOLD) return

        // Must be a local maximum
        val isPeak = (0 until WINDOW_SIZE).all { i ->
            i == mid || list[i].magnitude <= candidate.magnitude
        }
        if (!isPeak) return

        // Debounce: minimum time between steps (~200ms = 300 steps/min max)
        if (lastStepTimestamp > 0 && candidate.timestamp - lastStepTimestamp < MIN_STEP_INTERVAL_MS) {
            return
        }

        // Valid walking step interval: 300ms to 2000ms (~30 to 200 steps/min)
        if (lastStepTimestamp > 0) {
            val interval = candidate.timestamp - lastStepTimestamp
            if (interval > MAX_STEP_INTERVAL_MS) {
                // Gap too large — new walking segment
                walkingSegmentCount++
            }
        }

        lastStepTimestamp = candidate.timestamp
        stepTimestamps.add(candidate.timestamp)
    }

    /**
     * Compute daily gait metrics from accumulated step data.
     * Call at the end of the day or when enough data is collected.
     */
    fun computeMetrics(): GaitMetrics? {
        if (stepTimestamps.size < MIN_STEPS_FOR_METRICS) return null

        val intervals = stepTimestamps.zipWithNext { a, b -> (b - a).toDouble() }
            .filter { it in MIN_STEP_INTERVAL_MS.toDouble()..MAX_STEP_INTERVAL_MS.toDouble() }

        if (intervals.size < MIN_STEPS_FOR_METRICS - 1) return null

        val avgStrideTime = intervals.average()
        val variability = standardDeviation(intervals)
        val asymmetry = computeAsymmetry(intervals)
        val cadence = if (avgStrideTime > 0) 60_000.0 / avgStrideTime else 0.0

        return GaitMetrics(
            date = LocalDate.now().toString(),
            stepCount = stepTimestamps.size,
            avgStrideTimeMs = avgStrideTime,
            strideTimeVariability = variability,
            asymmetryRatio = asymmetry,
            avgCadenceStepsPerMin = cadence,
            walkingSegments = walkingSegmentCount.coerceAtLeast(1),
        )
    }

    /**
     * Asymmetry: compare alternating step intervals (left vs right foot).
     * Ratio of 1.0 = symmetric gait. >1.05 or <0.95 = notable asymmetry.
     */
    private fun computeAsymmetry(intervals: List<Double>): Double {
        if (intervals.size < 4) return 1.0

        val even = intervals.filterIndexed { i, _ -> i % 2 == 0 }
        val odd = intervals.filterIndexed { i, _ -> i % 2 == 1 }

        val evenAvg = even.average()
        val oddAvg = odd.average()

        if (evenAvg == 0.0 || oddAvg == 0.0) return 1.0

        return if (evenAvg >= oddAvg) evenAvg / oddAvg else oddAvg / evenAvg
    }

    private fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        return sqrt(variance)
    }

    fun reset() {
        samples.clear()
        stepTimestamps.clear()
        windowBuffer.clear()
        walkingSegmentCount = 0
        lastStepTimestamp = 0
    }

    companion object {
        // Peak detection window: 5 samples at ~50Hz = 100ms window
        private const val WINDOW_SIZE = 5

        // Step must exceed ~1.2g to count (filters out arm swing, drift)
        private const val STEP_THRESHOLD = 11.8f // m/s^2 (~1.2g)

        // Debounce: no two steps closer than 200ms
        private const val MIN_STEP_INTERVAL_MS = 200L

        // Max interval to still count as same walking bout
        private const val MAX_STEP_INTERVAL_MS = 2000L

        // Need at least 20 steps to compute meaningful metrics
        private const val MIN_STEPS_FOR_METRICS = 20
    }
}
