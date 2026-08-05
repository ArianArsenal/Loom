package com.example.loom.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun GaplessProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = color.copy(alpha = 0.2f)
    Box(modifier = modifier.clip(RoundedCornerShape(50)).background(trackColor)) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).background(color))
    }
}

val globalDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

fun getRemainingDaysText(targetDate: String): String? {
    if (targetDate.isBlank()) return null
    return try {
        val target = globalDateFormatter.parse(targetDate) ?: return null
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
        val diff = target.time - today.time
        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
        when {
            days < 0 -> "${-days}d overdue"
            days == 0L -> "Today"
            else -> "${days}d left"
        }
    } catch (e: Exception) { null }
}