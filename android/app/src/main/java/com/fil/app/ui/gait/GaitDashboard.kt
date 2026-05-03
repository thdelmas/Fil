package com.fil.app.ui.gait

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fil.app.analysis.GaitDriftResult
import com.fil.app.data.GaitBaseline
import com.fil.app.data.GaitMetrics
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun GaitDashboard(
    metrics: GaitMetrics?,
    baseline: GaitBaseline?,
    drift: GaitDriftResult?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (drift?.driftDetected == true) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Gait Analysis",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (metrics == null) {
                Text(
                    text = "Collecting data\u2026 Walk normally with your phone in your pocket.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                MetricRow("Steps", "${metrics.stepCount}")
                MetricRow("Cadence", "${metrics.avgCadenceStepsPerMin.roundToInt()} steps/min")
                MetricRow("Stride time", "${metrics.avgStrideTimeMs.roundToInt()} ms")
                MetricRow("Asymmetry", formatAsymmetry(metrics.asymmetryRatio))

                if (baseline != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Baseline: ${baseline.days} days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (drift != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DriftSummary(drift)
                } else if (baseline == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Building your baseline\u2026 need ${com.fil.app.data.GaitRepository.MIN_DAYS_FOR_BASELINE} days minimum.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DriftSummary(drift: GaitDriftResult) {
    if (drift.driftDetected) {
        for (alert in drift.alerts) {
            Text(
                text = alert,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    } else {
        Text(
            text = "Your gait is within your normal range.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatAsymmetry(ratio: Double): String {
    val percent = ((ratio - 1.0) * 100).roundToInt()
    return if (percent <= 0) "Symmetric" else "+${percent}%"
}
