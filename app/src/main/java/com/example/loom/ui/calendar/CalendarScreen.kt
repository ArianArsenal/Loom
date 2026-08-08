package com.example.loom.ui.calendar

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.PresetExercise
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timelineDateFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

private fun Float.formatWeight(): String {
    return if (this % 1f == 0f) this.toInt().toString() else this.toString()
}

@Composable
fun SimpleMacroRing(calories: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = Color(0xFF4CAF50).copy(alpha = 0.3f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val context = LocalContext.current
    val currentMonth by viewModel.currentMonth.collectAsState()
    val activeDays by viewModel.activeDays.collectAsState()

    val selectedDateId by viewModel.selectedDateId.collectAsState()
    val snapshot by viewModel.selectedDateSnapshot.collectAsState()

    val allPresets by viewModel.allPresets.collectAsState()
    val globalTemplates by viewModel.exerciseTemplates.collectAsState()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val isFuture = selectedDate.isAfter(LocalDate.now())

    var showMacroEditDialog by remember { mutableStateOf(false) }
    var showDetailedExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.selectDate(selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    }

    if (showMacroEditDialog) {
        val record = snapshot?.record
        var editCals by remember(record) { mutableStateOf(record?.calories?.toString() ?: "0") }
        var editPro by remember(record) { mutableStateOf(record?.protein?.toString() ?: "0") }
        var editCarbs by remember(record) { mutableStateOf(record?.carbs?.toString() ?: "0") }
        var editFats by remember(record) { mutableStateOf(record?.fats?.toString() ?: "0") }

        AlertDialog(
            onDismissRequest = { showMacroEditDialog = false },
            title = { Text("Edit Macros for ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editCals, onValueChange = { editCals = it }, label = { Text("Calories") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editPro, onValueChange = { editPro = it }, label = { Text("Protein (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editCarbs, onValueChange = { editCarbs = it }, label = { Text("Carbs (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editFats, onValueChange = { editFats = it }, label = { Text("Fats (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updatePastMacros(
                        editCals.toIntOrNull() ?: 0,
                        editPro.toIntOrNull() ?: 0,
                        editCarbs.toIntOrNull() ?: 0,
                        editFats.toIntOrNull() ?: 0
                    )
                    showMacroEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showMacroEditDialog = false }) { Text("Cancel") } }
        )
    }

    if (exerciseToDelete != null) {
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Delete Exercise") },
            text = { Text("Remove '${exerciseToDelete?.exerciseName}' from this day?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deletePastExercise(exerciseToDelete!!)
                    exerciseToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { exerciseToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showDetailedExerciseDialog) {
        Dialog(
            onDismissRequest = { showDetailedExerciseDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f).imePadding().padding(vertical = 24.dp).heightIn(max = 750.dp),
                shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Single Log") })
                        Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Presets") })
                    }

                    Box(modifier = Modifier.padding(16.dp).weight(1f)) {
                        if (selectedTabIndex == 0) {
                            var inputName by remember { mutableStateOf("") }
                            var expanded by remember { mutableStateOf(false) }
                            var isTimeBased by remember { mutableStateOf(false) }
                            var inputSets by remember { mutableStateOf("3") }
                            var inputReps by remember { mutableStateOf("10") }
                            var inputDuration by remember { mutableStateOf("60") }
                            var varyWeight by remember { mutableStateOf(false) }
                            var inputWeight by remember { mutableStateOf("60") }
                            var advancedWeights by remember { mutableStateOf(mapOf<Int, String>()) }

                            Column(modifier = Modifier.fillMaxSize()) {
                                Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                                        OutlinedTextField(
                                            value = inputName, onValueChange = { inputName = it; expanded = true },
                                            label = { Text("Search Exercise") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        val filtered = globalTemplates.filter { it.name.contains(inputName, true) }
                                        val exactMatch = globalTemplates.any { it.name.equals(inputName, ignoreCase = true) }

                                        if ((filtered.isNotEmpty() || (inputName.isNotBlank() && !exactMatch)) && expanded) {
                                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                                                filtered.forEach { option ->
                                                    Box(modifier = Modifier.fillMaxWidth().clickable { inputName = option.name; expanded = false }.padding(16.dp)) { Text(option.name) }
                                                }
                                                if (inputName.isNotBlank() && !exactMatch) {
                                                    Box(modifier = Modifier.fillMaxWidth().clickable {
                                                        viewModel.createExerciseTemplate(inputName)
                                                        expanded = false
                                                    }.padding(16.dp)) {
                                                        Text("Create new exercise: \"$inputName\"", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(checked = isTimeBased, onCheckedChange = { isTimeBased = it }, modifier = Modifier.scale(0.8f))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Time-based (e.g., Plank)", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = inputSets, onValueChange = { inputSets = it }, label = { Text("Sets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                        if (isTimeBased) {
                                            OutlinedTextField(value = inputDuration, onValueChange = { inputDuration = it }, label = { Text("Seconds") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                        } else {
                                            OutlinedTextField(value = inputReps, onValueChange = { inputReps = it }, label = { Text("Reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                        }
                                    }

                                    if (!isTimeBased) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Switch(checked = varyWeight, onCheckedChange = { varyWeight = it }, modifier = Modifier.scale(0.8f))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Different weight per set", style = MaterialTheme.typography.bodySmall)
                                        }

                                        if (varyWeight) {
                                            val setCount = inputSets.toIntOrNull() ?: 0
                                            if (setCount > 0) {
                                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    for (i in 0 until setCount) {
                                                        OutlinedTextField(
                                                            value = advancedWeights[i] ?: "",
                                                            onValueChange = { newVal -> advancedWeights = advancedWeights.toMutableMap().apply { put(i, newVal) } },
                                                            label = { Text("Set ${i + 1} (kg)") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            modifier = Modifier.fillMaxWidth().height(56.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = inputWeight, onValueChange = { inputWeight = it },
                                                label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { showDetailedExerciseDialog = false }) { Text("Cancel") }
                                    Button(onClick = {
                                        if (inputName.isNotBlank()) {
                                            val wList = if (varyWeight) {
                                                val setCount = inputSets.toIntOrNull() ?: 0
                                                (0 until setCount).map { advancedWeights[it]?.toFloatOrNull() ?: 0f }
                                            } else emptyList()

                                            viewModel.addPastExercise(
                                                name = inputName, sets = inputSets.toIntOrNull() ?: 0, reps = inputReps.toIntOrNull() ?: 0,
                                                weight = inputWeight.toFloatOrNull() ?: 0f, weightListJson = Json.encodeToString(wList),
                                                isTimeBased = isTimeBased, duration = inputDuration.toIntOrNull() ?: 0
                                            )
                                        }
                                        showDetailedExerciseDialog = false
                                    }) { Text("Log Past Exercise") }
                                }
                            }
                        } else {
                            var presetDropdownExpanded by remember { mutableStateOf(false) }
                            var activePresetName by remember { mutableStateOf("") }
                            var activeExercises by remember { mutableStateOf(listOf<PresetExercise>()) }

                            Column(modifier = Modifier.fillMaxSize()) {
                                Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                                    ExposedDropdownMenuBox(expanded = presetDropdownExpanded, onExpandedChange = { presetDropdownExpanded = it }, modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = if (activePresetName.isEmpty()) "Select a Preset..." else activePresetName,
                                            onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetDropdownExpanded) },
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                        )
                                        ExposedDropdownMenu(expanded = presetDropdownExpanded, onDismissRequest = { presetDropdownExpanded = false }) {
                                            allPresets.forEach { preset ->
                                                DropdownMenuItem(text = { Text(preset.presetName) }, onClick = {
                                                    activePresetName = preset.presetName; activeExercises = preset.exercises
                                                    presetDropdownExpanded = false
                                                })
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(8.dp)) {
                                        if (activeExercises.isEmpty()) {
                                            Text("No exercises selected.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                                        }
                                        activeExercises.forEach { ex ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(ex.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                    if (ex.isTimeBased) {
                                                        Text("${ex.sets} Sets × ${ex.durationInSeconds} Secs", style = MaterialTheme.typography.bodySmall)
                                                    } else {
                                                        val wList = try { Json.decodeFromString<List<Float>>(ex.weightListJson) } catch(e:Exception){ emptyList() }
                                                        if (wList.isNotEmpty()) {
                                                            Text("${ex.sets} Sets × ${ex.reps} Reps @ ${wList.joinToString(", ") { it.formatWeight() }}kg", style = MaterialTheme.typography.bodySmall)
                                                        } else {
                                                            Text("${ex.sets} Sets × ${ex.reps} Reps @ ${ex.weight.formatWeight()}kg", style = MaterialTheme.typography.bodySmall)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { showDetailedExerciseDialog = false }) { Text("Cancel") }
                                    Button(onClick = {
                                        if (activePresetName.isNotBlank() && activeExercises.isNotEmpty()) {
                                            viewModel.logPresetExercisesToPast(activePresetName, activeExercises)
                                            Toast.makeText(context, "Preset Logged!", Toast.LENGTH_SHORT).show()
                                            showDetailedExerciseDialog = false
                                        }
                                    }) { Text("Log Entire Preset") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    text = day, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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

                val hasData = activeDays.contains(dateIdStr)
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

                    val record = snapshot?.record
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SimpleMacroRing(calories = record?.calories ?: 0, modifier = Modifier.size(50.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Fuel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    if (!isFuture) {
                                        IconButton(onClick = { showMacroEditDialog = true }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Macros", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                Text("P: ${record?.protein ?: 0}g", style = MaterialTheme.typography.labelSmall)
                                Text("C: ${record?.carbs ?: 0}g", style = MaterialTheme.typography.labelSmall)
                                Text("F: ${record?.fats ?: 0}g", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isFuture) {
                                    IconButton(onClick = { viewModel.removePastWater(250) }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Remove, "Less Water", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Icon(Icons.Outlined.WaterDrop, contentDescription = "Water", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp))
                                if (!isFuture) {
                                    IconButton(onClick = { viewModel.addPastWater(250) }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Add, "More Water", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            val waterLiters = (record?.waterIntakeMl ?: 0) / 1000f
                            Text("${String.format(Locale.US, "%.2f", waterLiters)} L", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        }
                    }

                    // --- NEW WEIGHT DISPLAY ---
                    val loggedWeight = record?.weightLogKg ?: 0f
                    if (loggedWeight > 0f) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = "Weight", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Logged Weight", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                                Text("${loggedWeight.formatWeight()} kg", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    val exercises = snapshot?.exercises ?: emptyList()
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Workout Logs", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        if (!isFuture) {
                            IconButton(onClick = { showDetailedExerciseDialog = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Log Past Exercise", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (exercises.isEmpty()) {
                        Text("Rest day.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        exercises.groupBy { it.presetName }.forEach { (preset, list) ->
                            Text(preset ?: "Single Exercises", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                            list.forEach { ex ->
                                val wList = try { Json.decodeFromString<List<Float>>(ex.weightListJson) } catch(e:Exception){ emptyList() }
                                val weightStr = if (wList.isNotEmpty()) wList.joinToString(", ") { it.formatWeight() } else ex.weight.formatWeight()
                                val details = if (ex.isTimeBased) "${ex.sets} x ${ex.durationInSeconds}s" else "${ex.sets} x ${ex.reps} | $weightStr"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .combinedClickable(
                                            onClick = { },
                                            onLongClick = { exerciseToDelete = ex }
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (ex.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (ex.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${ex.exerciseName}  ·  $details",
                                        style = MaterialTheme.typography.bodySmall,
                                        textDecoration = if (ex.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (ex.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

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