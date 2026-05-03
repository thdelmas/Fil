package com.fil.app.data

/**
 * Daily gait metrics computed from walking segments.
 *
 * @param date ISO date string (yyyy-MM-dd)
 * @param stepCount total steps detected during the day
 * @param avgStrideTimeMs average time between consecutive steps (ms)
 * @param strideTimeVariability standard deviation of stride times (ms)
 * @param asymmetryRatio left/right stride time ratio (1.0 = perfect symmetry)
 * @param avgCadenceStepsPerMin steps per minute during walking segments
 * @param walkingSegments number of detected walking bouts
 */
data class GaitMetrics(
    val date: String,
    val stepCount: Int,
    val avgStrideTimeMs: Double,
    val strideTimeVariability: Double,
    val asymmetryRatio: Double,
    val avgCadenceStepsPerMin: Double,
    val walkingSegments: Int,
    val timestamp: Long = System.currentTimeMillis(),
)
