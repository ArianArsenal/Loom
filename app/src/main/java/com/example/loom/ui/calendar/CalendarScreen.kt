package com.example.loom.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// Helper function to draw the circular macro/calorie ring from Health screen
@Composable
fun SimpleMacroRing(calories: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = Color(0xFF4CAF50).copy(alpha = 0.3f), // Base color
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            // Fake a progress ring for aesthetics (assuming 2500 is a rough target)
            val fillPercentage = (calories.toFloat() / 2500f).coerceIn(0f, 1f)
            drawArc(
                color = Color(0xFF4CAF50),
                startAngle = -90f,
                sweepAngle = 360f * fillPercentage,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$calories", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val records by viewModel.monthlyRecords.collectAsState()

    // Time Machine Data
    val selectedDateId by viewModel.selectedDateId.collectAsState()
    val snapshot by viewModel.selectedDateSnapshot.collectAsState()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Sync initial click
    LaunchedEffect(Unit) {
        viewModel.selectDate(selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp) // Removed bottom padding for scroll
    ) {
        // --- CALENDAR HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previousMonth() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month") }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { viewModel.nextMonth() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- DAYS OF WEEK ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    text = day, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- GRID CALCULATION ---
        val firstDayOfMonth = currentMonth.atDay(1)
        val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7
        val daysInMonth = currentMonth.lengthOfMonth()

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(dayOfWeekOffset) { Spacer(modifier = Modifier.size(40.dp)) }

            items(daysInMonth) { dayIndex ->
                val dayNumber = dayIndex + 1
                val date = currentMonth.atDay(dayNumber)
                val dateIdStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                val hasData = records.containsKey(dateIdStr)
                val isSelected = date == selectedDate

                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                hasData -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                        .clickable {
                            selectedDate = date
                            viewModel.selectDate(dateIdStr)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayNumber.toString(),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected || hasData) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- THE TIME MACHINE DASHBOARD ---
        AnimatedVisibility(
            visible = selectedDateId != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).weight(1f, fill = false),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {

                    // Dashboard Header
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { viewModel.clearSelectedDate() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (snapshot?.record == null && snapshot?.tasks.isNullOrEmpty() && snapshot?.exercises.isNullOrEmpty()) {
                        Text("No logs for this day. It's a blank page!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {

                        // 1. Health Overview Row (Calories & Water)
                        val record = snapshot?.record
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                            // Nutrition Mini-Card
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SimpleMacroRing(calories = record?.calories ?: 0, modifier = Modifier.size(50.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Protein: ${record?.protein ?: 0}g", style = MaterialTheme.typography.labelSmall)
                                    Text("Carbs: ${record?.carbs ?: 0}g", style = MaterialTheme.typography.labelSmall)
                                    Text("Fats: ${record?.fats ?: 0}g", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            // Water Mini-Card
                            Column(horizontalAlignment = Alignment.End) {
                                Icon(Icons.Outlined.WaterDrop, contentDescription = "Water", tint = MaterialTheme.colorScheme.primary)
                                val waterLiters = (record?.waterIntakeMl ?: 0) / 1000f
                                Text("${String.format(Locale.US, "%.2f", waterLiters)} L", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Workouts
                        val exercises = snapshot?.exercises ?: emptyList()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Workout Logs", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (exercises.isEmpty()) {
                            Text("Rest day.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            // Group by preset
                            exercises.groupBy { it.presetName }.forEach { (preset, list) ->
                                Text(preset ?: "Single Exercises", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                                list.forEach { ex ->
                                    Text("• ${ex.exerciseName} (${ex.sets} sets)", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Tasks
                        val tasks = snapshot?.tasks ?: emptyList()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            val doneCount = tasks.count { it.isCompleted }
                            Text("Tasks ($doneCount / ${tasks.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (tasks.isEmpty()) {
                            Text("No tasks scheduled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            tasks.forEach { task ->
                                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (task.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                                        contentDescription = null, modifier = Modifier.size(14.dp),
                                        tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        task.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // 4. Skincare & Diary (Optional Add-ons at the bottom)
                        if (record != null) {
                            val completedRoutines = try { Json.decodeFromString<List<String>>(record.completedSkincareRoutinesJson) } catch (e: Exception) { emptyList() }
                            if (completedRoutines.isNotEmpty() || record.diaryEntry.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))

                                if (completedRoutines.isNotEmpty()) {
                                    Text("Skincare: ${completedRoutines.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (record.diaryEntry.isNotBlank()) {
                                    Text("Note: \"${record.diaryEntry}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}