package com.fil.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private val Context.gaitDataStore: DataStore<Preferences> by preferencesDataStore(name = "gait")

/**
 * Stores daily gait metrics on-device using DataStore.
 * Maintains a 14-day rolling baseline for drift detection.
 */
class GaitRepository(private val context: Context) {

    private val metricsKey = stringPreferencesKey("daily_metrics_json")

    val dailyMetrics: Flow<List<GaitMetrics>> = context.gaitDataStore.data.map { prefs ->
        parseMetrics(prefs[metricsKey] ?: "[]")
    }

    val baseline: Flow<GaitBaseline?> = dailyMetrics.map { metrics ->
        computeBaseline(metrics)
    }

    suspend fun saveMetrics(metrics: GaitMetrics) {
        context.gaitDataStore.edit { prefs ->
            val existing = parseMetrics(prefs[metricsKey] ?: "[]").toMutableList()

            // Replace today's entry if it exists, otherwise add
            existing.removeAll { it.date == metrics.date }
            existing.add(metrics)

            // Keep only the last 30 days (14 for baseline + buffer for trends)
            val cutoff = LocalDate.now().minusDays(30)
            existing.removeAll { LocalDate.parse(it.date).isBefore(cutoff) }

            prefs[metricsKey] = serializeMetrics(existing)
        }
    }

    private fun computeBaseline(metrics: List<GaitMetrics>): GaitBaseline? {
        val cutoff = LocalDate.now().minusDays(BASELINE_DAYS.toLong())
        val baselineMetrics = metrics.filter {
            !LocalDate.parse(it.date).isBefore(cutoff)
        }

        if (baselineMetrics.size < MIN_DAYS_FOR_BASELINE) return null

        return GaitBaseline(
            days = baselineMetrics.size,
            avgStrideTimeMs = baselineMetrics.map { it.avgStrideTimeMs }.average(),
            sdStrideTimeMs = sd(baselineMetrics.map { it.avgStrideTimeMs }),
            avgVariability = baselineMetrics.map { it.strideTimeVariability }.average(),
            sdVariability = sd(baselineMetrics.map { it.strideTimeVariability }),
            avgAsymmetry = baselineMetrics.map { it.asymmetryRatio }.average(),
            sdAsymmetry = sd(baselineMetrics.map { it.asymmetryRatio }),
            avgCadence = baselineMetrics.map { it.avgCadenceStepsPerMin }.average(),
            sdCadence = sd(baselineMetrics.map { it.avgCadenceStepsPerMin }),
        )
    }

    private fun sd(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        return kotlin.math.sqrt(variance)
    }

    private fun parseMetrics(json: String): List<GaitMetrics> {
        val list = mutableListOf<GaitMetrics>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                GaitMetrics(
                    date = o.getString("date"),
                    stepCount = o.getInt("stepCount"),
                    avgStrideTimeMs = o.getDouble("avgStrideTimeMs"),
                    strideTimeVariability = o.getDouble("strideTimeVariability"),
                    asymmetryRatio = o.getDouble("asymmetryRatio"),
                    avgCadenceStepsPerMin = o.getDouble("avgCadenceStepsPerMin"),
                    walkingSegments = o.getInt("walkingSegments"),
                    timestamp = o.optLong("timestamp", 0),
                )
            )
        }
        return list
    }

    private fun serializeMetrics(metrics: List<GaitMetrics>): String {
        val array = JSONArray()
        for (m in metrics) {
            array.put(JSONObject().apply {
                put("date", m.date)
                put("stepCount", m.stepCount)
                put("avgStrideTimeMs", m.avgStrideTimeMs)
                put("strideTimeVariability", m.strideTimeVariability)
                put("asymmetryRatio", m.asymmetryRatio)
                put("avgCadenceStepsPerMin", m.avgCadenceStepsPerMin)
                put("walkingSegments", m.walkingSegments)
                put("timestamp", m.timestamp)
            })
        }
        return array.toString()
    }

    companion object {
        const val BASELINE_DAYS = 14
        const val MIN_DAYS_FOR_BASELINE = 5
    }
}

/**
 * Personal baseline computed from the 14-day rolling window.
 * Each metric has a mean and standard deviation for z-score calculation.
 */
data class GaitBaseline(
    val days: Int,
    val avgStrideTimeMs: Double,
    val sdStrideTimeMs: Double,
    val avgVariability: Double,
    val sdVariability: Double,
    val avgAsymmetry: Double,
    val sdAsymmetry: Double,
    val avgCadence: Double,
    val sdCadence: Double,
)
