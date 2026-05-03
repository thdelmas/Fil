package com.fil.app.analysis

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FallDetectionAlgorithmTest {

    private lateinit var algo: FallDetectionAlgorithm

    @Before
    fun setUp() {
        algo = FallDetectionAlgorithm()
    }

    @Test
    fun `full fall sequence triggers detection`() {
        var time = 1000L // avoid timestamp 0 (sentinel for "unset")

        // Normal walking (~1g)
        repeat(10) {
            assertFalse(algo.processSample(9.8f, time))
            time += 20
        }

        // Phase 1: Free-fall (~0.3g)
        assertFalse(algo.processSample(3.0f, time))
        time += 100

        // Phase 2: Impact (~4g)
        assertFalse(algo.processSample(39.2f, time))
        val impactTime = time
        time += 20

        // Wait for stillness delay (500ms)
        repeat(25) {
            assertFalse(algo.processSample(15.0f, time)) // transition, not still
            time += 20
        }

        // Phase 3: Stillness (~1g)
        time = impactTime + FallDetectionAlgorithm.STILLNESS_DELAY_MS + 100
        assertTrue(algo.processSample(9.8f, time))
    }

    @Test
    fun `no detection without freefall phase`() {
        var time = 1000L

        // Skip freefall, go straight to impact
        assertFalse(algo.processSample(39.2f, time))
        time += 600

        // Stillness
        assertFalse(algo.processSample(9.8f, time))
    }

    @Test
    fun `no detection without impact phase`() {
        var time = 1000L

        // Freefall
        assertFalse(algo.processSample(3.0f, time))
        time += 100

        // No impact, just back to normal
        assertFalse(algo.processSample(9.8f, time))
        time += 1000

        // Stillness window passes with no impact
        assertFalse(algo.processSample(9.8f, time))
    }

    @Test
    fun `no detection without stillness phase`() {
        var time = 1000L

        // Freefall
        assertFalse(algo.processSample(3.0f, time))
        time += 100

        // Impact
        assertFalse(algo.processSample(39.2f, time))
        val impactTime = time
        time += 20

        // Active movement instead of stillness (>1.1g) until window expires
        while (time - impactTime < FallDetectionAlgorithm.STILLNESS_WINDOW_MS + 100) {
            assertFalse(algo.processSample(15.0f, time))
            time += 20
        }
    }

    @Test
    fun `impact too late after freefall is ignored`() {
        var time = 1000L

        // Freefall
        assertFalse(algo.processSample(3.0f, time))

        // Wait beyond impact window (>500ms)
        time += FallDetectionAlgorithm.IMPACT_WINDOW_MS + 100

        // Impact comes too late
        assertFalse(algo.processSample(39.2f, time))
        time += 600

        // Stillness — should not trigger because impact was not linked to freefall
        assertFalse(algo.processSample(9.8f, time))
    }

    @Test
    fun `sub-threshold impact after freefall is ignored`() {
        var time = 1000L

        // Freefall
        assertFalse(algo.processSample(3.0f, time))
        time += 100

        // Sub-threshold impact (only 2g, below 3g threshold)
        assertFalse(algo.processSample(19.6f, time))
        time += 600

        // Stillness — should not trigger
        assertFalse(algo.processSample(9.8f, time))
    }

    @Test
    fun `reset clears all state`() {
        var time = 1000L

        // Freefall + impact
        algo.processSample(3.0f, time)
        time += 100
        algo.processSample(39.2f, time)

        algo.reset()

        // Stillness should not trigger after reset
        time += 600
        assertFalse(algo.processSample(9.8f, time))
    }

    @Test
    fun `multiple falls can be detected sequentially`() {
        // First fall
        assertTrue(simulateFall(0L), "First fall should be detected")

        // After detection, algorithm auto-resets. Verify state is clean.
        assertEquals(0L, algo.freefallDetectedAt, "freefallDetectedAt should be 0 after reset")
        assertEquals(0L, algo.impactDetectedAt, "impactDetectedAt should be 0 after reset")

        // Second fall after some time
        assertTrue(simulateFall(10_000L), "Second fall should be detected")
    }

    @Test
    fun `normal walking does not trigger`() {
        var time = 1000L
        // Simulate 10 seconds of walking (oscillating between 0.8g and 1.5g)
        repeat(500) { i ->
            val mag = if (i % 25 < 5) 14.7f else 9.0f // step impacts at 1.5g
            assertFalse(algo.processSample(mag, time))
            time += 20
        }
    }

    @Test
    fun `phone drop does not trigger — no freefall long enough`() {
        var time = 1000L

        // Brief low-g from phone toss (above freefall threshold)
        assertFalse(algo.processSample(5.5f, time)) // above 4.9 threshold
        time += 50

        // Impact on table
        assertFalse(algo.processSample(35.0f, time))
        time += 600

        // Phone sitting still
        assertFalse(algo.processSample(9.8f, time))
    }

    // --- Helpers ---

    private fun simulateFall(startTime: Long): Boolean {
        // Use startTime + 1000 as base to avoid timestamp 0 (which is the "unset" sentinel)
        var time = startTime + 1000

        // Freefall
        algo.processSample(3.0f, time)
        time += 100

        // Impact
        algo.processSample(39.2f, time)
        val impactTime = time
        time += 20

        // Wait past stillness delay
        time = impactTime + FallDetectionAlgorithm.STILLNESS_DELAY_MS + 100

        // Stillness
        return algo.processSample(9.8f, time)
    }
}
