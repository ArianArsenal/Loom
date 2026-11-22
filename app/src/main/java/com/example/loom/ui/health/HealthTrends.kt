package com.example.loom.ui.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Health card: streaks plus 7/30/90-day charts of weight, sleep, calories and water. */
@Composable
fun TrendsCard(viewModel: HealthViewModel) {
    val trends by viewModel.trends.collectAsState()
    val streaks by viewModel.streaks.collectAsState()
    val days by viewModel.trendDays.collectAsState()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Trends & Streaks", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StreakChip("Workout", streaks.workout, Modifier.weight(1f))
                StreakChip("Water", streaks.water, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StreakChip("Supplements", streaks.supplements, Modifier.weight(1f))
                StreakChip("Skincare", streaks.skincare, Modifier.weight(1f))
            }
            Text(
                "${streaks.workoutsLast7Days} workout day${if (streaks.workoutsLast7Days == 1) "" else "s"} in the last 7 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(16.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(7, 30, 90).forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = days == option,
                        onClick = { viewModel.setTrendDays(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, 3)
                    ) { Text("$option days") }
                }
            }

            Spacer(Modifier.height(16.dp))
            TrendChart("Weight", trends.weight, "kg", Color(0xFF7E9F8A), showChange = true)
            TrendChart("Sleep", trends.sleep, "h", Color(0xFF6C8EBF))
            TrendChart("Calories", trends.calories, "kcal", Color(0xFFD89B5B))
            TrendChart("Protein", trends.protein, "g", Color(0xFFB5677D))
            TrendChart("Water", trends.water, "ml", Color(0xFF4FA3C7))
        }
    }
}

@Composable
private fun StreakChip(label: String, streak: Streak, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${if (streak.current > 0) "🔥 " else ""}${streak.current}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(" day${if (streak.current == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text("Best ${streak.best}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TrendChart(title: String, series: TrendSeries, unit: String, color: Color, showChange: Boolean = false) {
    val logged = series.logged
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(title, fontWeight = FontWeight.Bold)
            val summary = when {
                logged.isEmpty() -> "No data"
                showChange && logged.size > 1 -> {
                    val change = (series.latest ?: 0f) - (series.first ?: 0f)
                    "${fmt(series.latest)} $unit (${if (change >= 0) "+" else ""}${fmt(change)})"
                }
                else -> "avg ${fmt(series.average)} $unit" + (series.target?.let { " · target ${fmt(it)}" } ?: "")
            }
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        if (logged.size < 2) {
            Text("Log at least two days to see a chart.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        } else {
            val gridColor = MaterialTheme.colorScheme.outlineVariant
            Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                val values = logged + listOfNotNull(series.target)
                val min = values.min()
                val max = values.max()
                val span = (max - min).takeIf { it > 0f } ?: 1f
                val n = series.points.size
                fun x(i: Int) = if (n <= 1) 0f else size.width * i / (n - 1)
                fun y(v: Float) = size.height - (v - min) / span * size.height * 0.9f - size.height * 0.05f

                series.target?.let { t ->
                    drawLine(gridColor, Offset(0f, y(t)), Offset(size.width, y(t)), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                }
                // Connect logged days; gaps (unlogged days) are bridged so the trend stays readable
                val path = Path()
                var started = false
                series.points.forEachIndexed { i, p ->
                    val v = p.value ?: return@forEachIndexed
                    if (!started) { path.moveTo(x(i), y(v)); started = true } else path.lineTo(x(i), y(v))
                }
                drawPath(path, color, style = Stroke(width = 5f))
                series.points.forEachIndexed { i, p ->
                    p.value?.let { drawCircle(color, radius = if (n > 40) 3f else 6f, center = Offset(x(i), y(it))) }
                }
            }
        }
    }
}

private fun fmt(v: Float?): String = when {
    v == null -> "–"
    v >= 100f -> v.toInt().toString()
    else -> "%.1f".format(v)
}

/** Compact line chart for a short series (e.g. an exercise's top weight per session). */
@Composable
fun MiniLineChart(values: List<Float>, color: Color, modifier: Modifier = Modifier.fillMaxWidth().height(90.dp)) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val span = (values.max() - min).takeIf { it > 0f } ?: 1f
        fun x(i: Int) = size.width * i / (values.size - 1)
        fun y(v: Float) = size.height - (v - min) / span * size.height * 0.9f - size.height * 0.05f
        val path = Path().apply {
            moveTo(x(0), y(values[0]))
            for (i in 1 until values.size) lineTo(x(i), y(values[i]))
        }
        drawPath(path, color, style = Stroke(width = 5f))
        values.forEachIndexed { i, v -> drawCircle(color, radius = 6f, center = Offset(x(i), y(v))) }
    }
}
