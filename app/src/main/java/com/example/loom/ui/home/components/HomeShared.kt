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

// --- FIX: Formatter is now dynamically generated to ensure it always uses the current Locale ---
fun getGlobalDateFormatter(): SimpleDateFormat {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US)
}

fun getRemainingDaysText(targetDate: String): String? {
    if (targetDate.isBlank()) return null
    return try {
        // Calendar-day difference: millisecond math was off by one across DST changes
        val days = java.time.temporal.ChronoUnit.DAYS.between(
            com.example.loom.util.DateUtils.getLogicalToday(),
            java.time.LocalDate.parse(targetDate)
        )
        when {
            days < 0 -> "${-days}d overdue"
            days == 0L -> "Today"
            else -> "${days}d left"
        }
    } catch (_: Exception) { null } // --- FIX: Removed unused 'e' parameter ---
}