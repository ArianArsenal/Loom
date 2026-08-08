package com.example.loom.ui.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

@Composable
fun FastingTimerCard(viewModel: HealthViewModel) {
    val isFasting by viewModel.isFasting.collectAsState()
    val startTime by viewModel.fastingStartTime.collectAsState()
    val goalHours by viewModel.fastingGoalHours.collectAsState()

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedGoal by remember { mutableIntStateOf(goalHours) }

    // Keep the local selection in sync if the global state changes
    LaunchedEffect(goalHours) { selectedGoal = goalHours }

    LaunchedEffect(isFasting) {
        while (isFasting) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Intermittent Fasting", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Dynamically updates based on whether we are fasting or just setting up
                val displayGoal = if (isFasting) goalHours else selectedGoal
                Text("${displayGoal}h Goal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            val activeGoal = if (isFasting) goalHours else selectedGoal
            val elapsedMillis = if (isFasting) currentTime - startTime else 0L
            val goalMillis = activeGoal * 60 * 60 * 1000L
            val progress = (elapsedMillis.toFloat() / goalMillis.toFloat()).coerceIn(0f, 1f)

            val elapsedHours = (elapsedMillis / (1000 * 60 * 60)).toInt()
            val elapsedMinutes = ((elapsedMillis / (1000 * 60)) % 60).toInt()
            val elapsedSeconds = ((elapsedMillis / 1000) % 60).toInt()

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(170.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(color = Color.Gray.copy(alpha = 0.2f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(color = Color(0xFF4CAF50), startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isFasting) "Fasting" else "Not Fasting", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format(Locale.US, "%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                    // NEW: Time Left / Overtime Display
                    if (isFasting) {
                        val remainingMillis = goalMillis - elapsedMillis
                        val isOvertime = remainingMillis < 0
                        val absRemaining = kotlin.math.abs(remainingMillis)
                        val remHours = (absRemaining / (1000 * 60 * 60)).toInt()
                        val remMinutes = ((absRemaining / (1000 * 60)) % 60).toInt()

                        Spacer(modifier = Modifier.height(4.dp))
                        if (isOvertime) {
                            Text("+$remHours" + "h " + "$remMinutes" + "m overtime", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        } else {
                            Text("$remHours" + "h " + "$remMinutes" + "m left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isFasting) {
                Button(onClick = { viewModel.toggleFasting(false, goalHours) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("End Fast")
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { selectedGoal = if (selectedGoal == 16) 18 else 16 }, modifier = Modifier.weight(1f)) {
                        Text(if (selectedGoal == 16) "Switch to 18:6" else "Switch to 16:8")
                    }
                    Button(onClick = { viewModel.toggleFasting(true, selectedGoal) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Fast")
                    }
                }
            }
        }
    }
}

@Composable
fun SleepRecoveryCard(viewModel: HealthViewModel) {
    val todayRecord by viewModel.todayRecord.collectAsState()
    val loggedHours = todayRecord?.sleepHours ?: 0f
    val loggedRating = todayRecord?.sleepRating ?: 0
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        var sliderHours by remember { mutableFloatStateOf(if (loggedHours > 0f) loggedHours else 7.5f) }
        var rating by remember { mutableIntStateOf(if (loggedRating > 0) loggedRating else 3) }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = (sliderHours * 2).toInt() - 4)

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Log Sleep & Recovery", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("${String.format(Locale.US, "%.1f", sliderHours)} Hours", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    LazyRow(state = listState, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 60.dp)) {
                        items(24) { index ->
                            val itemHours = 2f + (index * 0.5f)
                            val isSelected = itemHours == sliderHours
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { sliderHours = itemHours }.padding(8.dp)) {
                                Text(itemHours.toString(), style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Recovery Rating", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rate $star",
                                tint = if (star <= rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp).clickable { rating = star }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.logSleep(sliderHours, rating); showDialog = false }) { Text("Save Sleep") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sleep & Recovery", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (loggedHours > 0f) {
                // NEW: Sleek Night-Sky Gradient UI for Sleep
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
                ) {
                    Box(
                        modifier = Modifier
                            .background(Brush.linearGradient(listOf(Color(0xFF383A59), Color(0xFF26273B))))
                            .padding(20.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("${String.format(Locale.US, "%.1f", loggedHours)} Hours", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    (1..5).forEach { star ->
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (star <= loggedRating) Color(0xFFFFD700) else Color.White.copy(alpha=0.2f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Modern, subtle glass-morphic delete button
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.size(36.dp),
                                onClick = { viewModel.logSleep(0f, 0) }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete Sleep Log", tint = Color.White, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            } else {
                OutlinedButton(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Log Last Night's Sleep")
                }
            }
        }
    }
}

@Composable
fun BodyMeasurementsCard(viewModel: HealthViewModel) {
    val todayRecord by viewModel.todayRecord.collectAsState()
    val rawJson = todayRecord?.measurementsJson ?: "{}"
    val measurements = try { Json.decodeFromString<Map<String, Float>>(if (rawJson.isBlank()) "{}" else rawJson) } catch (e: Exception) { emptyMap() }

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        var waist by remember { mutableStateOf(measurements["Waist"]?.toString() ?: "") }
        var chest by remember { mutableStateOf(measurements["Chest"]?.toString() ?: "") }
        var arms by remember { mutableStateOf(measurements["Arms"]?.toString() ?: "") }
        var thighs by remember { mutableStateOf(measurements["Thighs"]?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Log Body Measurements", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = waist, onValueChange = { waist = it }, label = { Text("Waist (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = chest, onValueChange = { chest = it }, label = { Text("Chest (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = arms, onValueChange = { arms = it }, label = { Text("Arms (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = thighs, onValueChange = { thighs = it }, label = { Text("Thighs (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val map = mutableMapOf<String, Float>()
                    waist.toFloatOrNull()?.let { map["Waist"] = it }
                    chest.toFloatOrNull()?.let { map["Chest"] = it }
                    arms.toFloatOrNull()?.let { map["Arms"] = it }
                    thighs.toFloatOrNull()?.let { map["Thighs"] = it }
                    viewModel.logMeasurements(map)
                    showDialog = false
                }) { Text("Save Measurements") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Body Measurements", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { showDialog = true }) { Text("Edit") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (measurements.isEmpty()) {
                Text("No measurements logged today.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    measurements.forEach { (part, value) ->
                        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(part, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${value}cm", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}