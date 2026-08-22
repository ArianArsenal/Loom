package com.example.loom.ui.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.ui.home.HomeViewModel
import java.util.Locale

@Composable
fun AnimatedWaterGlassIcon(intakeMl: Int, goalMl: Int, modifier: Modifier = Modifier) {
    val safeGoal = if (goalMl > 0) goalMl else 3000
    val targetProgress = (intakeMl.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "WaterFillAnimation"
    )

    val glassColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val liquidColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(width = 20.dp, height = 24.dp)
            .drawBehind {
                val stroke = 2.dp.toPx()
                val corner = 6.dp.toPx()
                val path = Path().apply {
                    moveTo(stroke / 2f, 0f)
                    lineTo(stroke / 2f, size.height - corner)
                    // --- FIX: Updated deprecated quadraticBezierTo to quadraticTo ---
                    quadraticTo(stroke / 2f, size.height - stroke / 2f, corner, size.height - stroke / 2f)
                    lineTo(size.width - corner, size.height - stroke / 2f)
                    quadraticTo(size.width - stroke / 2f, size.height - stroke / 2f, size.width - stroke / 2f, size.height - corner)
                    lineTo(size.width - stroke / 2f, 0f)
                }
                drawPath(path = path, color = glassColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
            .padding(start = 3.dp, end = 3.dp, bottom = 3.dp, top = 2.dp)
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedProgress)
                .background(liquidColor, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
        )
    }
}

@Composable
fun HydrationCard(todayRecord: DailyRecord?, dailyGoal: Int, defaultWaterIncrement: Int, viewModel: HomeViewModel) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedWaterGlassIcon(
                        intakeMl = todayRecord?.waterIntakeMl ?: 0,
                        goalMl = dailyGoal,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Hydration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                val waterLiters = (todayRecord?.waterIntakeMl ?: 0) / 1000f
                val formattedLiters = String.format(Locale.US, "%.3f", waterLiters).trimEnd('0').trimEnd('.')
                Text("$formattedLiters L", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.removeWater() }) {
                    Icon(Icons.Default.Remove, "Remove Water")
                }
                Button(onClick = { viewModel.addWater() }, shape = RoundedCornerShape(12.dp)) {
                    Text("+ ${defaultWaterIncrement}ml", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}