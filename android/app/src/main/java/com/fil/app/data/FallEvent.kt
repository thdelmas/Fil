package com.fil.app.data

data class FallEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val peakAcceleration: Float,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val cancelled: Boolean = false,
)
