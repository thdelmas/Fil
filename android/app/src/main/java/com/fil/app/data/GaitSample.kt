package com.fil.app.data

/**
 * A single raw accelerometer reading used for gait analysis.
 */
data class GaitSample(
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float,
)
