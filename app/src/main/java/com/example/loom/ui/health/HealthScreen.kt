package com.example.loom.ui.health

import android.widget.NumberPicker
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.ExerciseTemplate
import com.example.loom.data.local.entity.PresetExercise
import com.example.loom.ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HealthScreen(viewModel: HomeViewModel) {
    val todayExercises by viewModel.todayExercises.collectAsState()
    val allPresets by viewModel.allPresets.collectAsState()
    val globalTemplates by viewModel.exerciseTemplates.collectAsState()

    var showExerciseDialog by remember { mutableStateOf(false) }
    var templateToEdit by remember { mutableStateOf<ExerciseTemplate?>(null) }
    var showTemplateEditDialog by remember { mutableStateOf(false) }

    // Lifted Preset Builder States (So we can pre-load them from long-press)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var activePresetId by remember { mutableIntStateOf(0) }
    var activePresetName by remember { mutableStateOf("") }
    var activeExercises by remember { mutableStateOf(listOf<PresetExercise>()) }

    // State for managing Logged Presets via Long Press
    var loggedPresetToManage by remember { mutableStateOf<String?>(null) }
    var showLoggedPresetDialog by remember { mutableStateOf(false) }

    // State for Editing a Logged Exercise (Left-to-Right Swipe)
    var loggedExerciseToEdit by remember { mutableStateOf<Exercise?>(null) }

    // --- TEMPLATE EDIT DIALOG ---
    if (showTemplateEditDialog && templateToEdit != null) {
        var editName by remember { mutableStateOf(templateToEdit!!.name) }
        AlertDialog(
            onDismissRequest = { showTemplateEditDialog = false },
            title = { Text("Edit Global Exercise") },
            text = {
                OutlinedTextField(
                    value = editName, onValueChange = { editName = it }, label = { Text("Exercise Name") }, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) viewModel.updateTemplate(templateToEdit!!.copy(name = editName))
                    showTemplateEditDialog = false; templateToEdit = null
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { viewModel.deleteTemplate(templateToEdit!!); showTemplateEditDialog = false; templateToEdit = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                    TextButton(onClick = { showTemplateEditDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    // --- LOGGED PRESET MANAGEMENT DIALOG (Now with Edit Option) ---
    if (showLoggedPresetDialog && loggedPresetToManage != null) {
        AlertDialog(
            onDismissRequest = { showLoggedPresetDialog = false },
            title = { Text("Manage '${loggedPresetToManage}'") },
            text = { Text("Would you like to edit the preset template or remove today's logs?") },
            confirmButton = {
                Button(
                    onClick = {
                        val targetPreset = allPresets.find { it.presetName == loggedPresetToManage }
                        if (targetPreset != null) {
                            activePresetId = targetPreset.id
                            activePresetName = targetPreset.presetName
                            activeExercises = targetPreset.exercises
                        } else {
                            activePresetId = 0
                            activePresetName = loggedPresetToManage ?: ""
                            activeExercises = emptyList()
                        }
                        selectedTabIndex = 1
                        showExerciseDialog = true
                        showLoggedPresetDialog = false
                        loggedPresetToManage = null
                    }
                ) { Text("Edit Preset Template") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            todayExercises.filter { it.presetName == loggedPresetToManage }.forEach { viewModel.deleteExercise(it) }
                            showLoggedPresetDialog = false
                            loggedPresetToManage = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete Logs") }
                    TextButton(onClick = { showLoggedPresetDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    // --- EDIT LOGGED EXERCISE DIALOG ---
    if (loggedExerciseToEdit != null) {
        var editName by remember { mutableStateOf(loggedExerciseToEdit!!.exerciseName) }
        var editIsTimeBased by remember { mutableStateOf(loggedExerciseToEdit!!.isTimeBased) }
        var editSets by remember { mutableStateOf(loggedExerciseToEdit!!.sets.toString()) }
        var editReps by remember { mutableStateOf(loggedExerciseToEdit!!.reps.toString()) }
        var editDuration by remember { mutableStateOf(loggedExerciseToEdit!!.durationInSeconds.toString()) }
        var editWeight by remember { mutableStateOf(loggedExerciseToEdit!!.weight) }

        AlertDialog(
            onDismissRequest = { loggedExerciseToEdit = null },
            title = { Text("Edit Logged Exercise") },
            text = {
                Column {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = editIsTimeBased, onCheckedChange = { editIsTimeBased = it }, modifier = Modifier.scale(0.8f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Time-based", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = editSets, onValueChange = { editSets = it }, label = { Text("Sets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        if (editIsTimeBased) {
                            OutlinedTextField(value = editDuration, onValueChange = { editDuration = it }, label = { Text("Secs") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        } else {
                            OutlinedTextField(value = editReps, onValueChange = { editReps = it }, label = { Text("Reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                    }
                    if (!editIsTimeBased) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Weight (kg)", style = MaterialTheme.typography.labelLarge)
                        AndroidView(
                            factory = { context -> NumberPicker(context).apply { minValue = 0; maxValue = 300; value = editWeight; setOnValueChangedListener { _, _, newVal -> editWeight = newVal } } },
                            update = { it.value = editWeight },
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) {
                        viewModel.deleteExercise(loggedExerciseToEdit!!)
                        viewModel.addExercise(
                            name = editName, sets = editSets.toIntOrNull() ?: 0, reps = editReps.toIntOrNull() ?: 0,
                            weight = editWeight, isTimeBased = editIsTimeBased, duration = editDuration.toIntOrNull() ?: 0,
                            presetName = loggedExerciseToEdit!!.presetName
                        )
                        loggedExerciseToEdit = null
                    }
                }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { loggedExerciseToEdit = null }) { Text("Cancel") }
            }
        )
    }

    // --- MAIN TABBED DIALOG ---
    if (showExerciseDialog) {
        Dialog(
            onDismissRequest = { showExerciseDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Single Log") })
                        Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Presets") })
                    }

                    Box(modifier = Modifier.padding(16.dp)) {
                        if (selectedTabIndex == 0) {
                            // ==========================================
                            // TAB 1: ORDINARY LOG
                            // ==========================================
                            var inputName by remember { mutableStateOf("") }
                            var expanded by remember { mutableStateOf(false) }
                            var isTimeBased by remember { mutableStateOf(false) }
                            var inputSets by remember { mutableStateOf("3") }
                            var inputReps by remember { mutableStateOf("10") }
                            var inputDuration by remember { mutableStateOf("60") }
                            var inputWeight by remember { mutableStateOf(60) }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                                    OutlinedTextField(
                                        value = inputName, onValueChange = { inputName = it; expanded = true },
                                        label = { Text("Search Exercise") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    val filtered = globalTemplates.filter { it.name.contains(inputName, true) }
                                    if (filtered.isNotEmpty() && expanded) {
                                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 240.dp)) {
                                            filtered.forEach { option ->
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().combinedClickable(
                                                        onClick = { inputName = option.name; expanded = false },
                                                        onLongClick = { templateToEdit = option; showTemplateEditDialog = true }
                                                    ).padding(16.dp)
                                                ) { Text(option.name) }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(checked = isTimeBased, onCheckedChange = { isTimeBased = it })
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
                                    Text("Weight (kg)", style = MaterialTheme.typography.labelLarge)
                                    AndroidView(
                                        factory = { context -> NumberPicker(context).apply { minValue = 0; maxValue = 300; value = inputWeight; setOnValueChangedListener { _, _, newVal -> inputWeight = newVal } } },
                                        update = { it.value = inputWeight },
                                        modifier = Modifier.fillMaxWidth().height(120.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { showExerciseDialog = false }) { Text("Cancel") }
                                    Button(onClick = {
                                        if (inputName.isNotBlank()) {
                                            viewModel.addExercise(name = inputName, sets = inputSets.toIntOrNull() ?: 0, reps = inputReps.toIntOrNull() ?: 0, weight = inputWeight, isTimeBased = isTimeBased, duration = inputDuration.toIntOrNull() ?: 0)
                                        }
                                        showExerciseDialog = false
                                    }) { Text("Log Exercise") }
                                }
                            }
                        } else {
                            // ==========================================
                            // TAB 2: PRESET MANAGER
                            // ==========================================
                            var presetDropdownExpanded by remember { mutableStateOf(false) }
                            var builderName by remember { mutableStateOf("") }
                            var builderExpanded by remember { mutableStateOf(false) }
                            var builderIsTimeBased by remember { mutableStateOf(false) }
                            var builderSets by remember { mutableStateOf("3") }
                            var builderReps by remember { mutableStateOf("10") }
                            var builderDuration by remember { mutableStateOf("60") }
                            var builderWeight by remember { mutableStateOf("60") }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    ExposedDropdownMenuBox(expanded = presetDropdownExpanded, onExpandedChange = { presetDropdownExpanded = it }, modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = if (activePresetId == 0) "Create New Preset..." else "Editing: $activePresetName",
                                            onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetDropdownExpanded) },
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                        )
                                        ExposedDropdownMenu(expanded = presetDropdownExpanded, onDismissRequest = { presetDropdownExpanded = false }) {
                                            allPresets.forEach { preset ->
                                                DropdownMenuItem(text = { Text(preset.presetName) }, onClick = {
                                                    activePresetId = preset.id; activePresetName = preset.presetName; activeExercises = preset.exercises; presetDropdownExpanded = false
                                                })
                                            }
                                        }
                                    }
                                    if (activePresetId != 0 || activeExercises.isNotEmpty()) {
                                        IconButton(onClick = { activePresetId = 0; activePresetName = ""; activeExercises = emptyList() }) {
                                            Icon(Icons.Default.Add, contentDescription = "New")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = activePresetName, onValueChange = { activePresetName = it }, label = { Text("Preset Title (e.g., Push Day)") }, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(12.dp))

                                LazyColumn(
                                    modifier = Modifier.heightIn(min = 50.dp, max = 150.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(8.dp)
                                ) {
                                    if (activeExercises.isEmpty()) item { Text("No exercises in this preset.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp)) }
                                    items(activeExercises) { ex ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(ex.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                if (ex.isTimeBased) Text("${ex.sets} Sets × ${ex.durationInSeconds} Secs", style = MaterialTheme.typography.bodySmall)
                                                else Text("${ex.sets} Sets × ${ex.reps} Reps @ ${ex.weight}kg", style = MaterialTheme.typography.bodySmall)
                                            }
                                            IconButton(onClick = { activeExercises = activeExercises.filter { it != ex } }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error) }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Add Exercise to Preset:", style = MaterialTheme.typography.labelMedium)

                                ExposedDropdownMenuBox(expanded = builderExpanded, onExpandedChange = { builderExpanded = it }) {
                                    OutlinedTextField(
                                        value = builderName, onValueChange = { builderName = it; builderExpanded = true }, label = { Text("Search Exercise") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                        modifier = Modifier.menuAnchor().fillMaxWidth().height(56.dp)
                                    )
                                    val filtered = globalTemplates.filter { it.name.contains(builderName, true) }
                                    if (filtered.isNotEmpty() && builderExpanded) {
                                        ExposedDropdownMenu(expanded = builderExpanded, onDismissRequest = { builderExpanded = false }, modifier = Modifier.heightIn(max = 240.dp)) {
                                            filtered.forEach { option ->
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().combinedClickable(
                                                        onClick = { builderName = option.name; builderExpanded = false },
                                                        onLongClick = { templateToEdit = option; showTemplateEditDialog = true }
                                                    ).padding(16.dp)
                                                ) { Text(option.name) }
                                            }
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Switch(checked = builderIsTimeBased, onCheckedChange = { builderIsTimeBased = it }, modifier = Modifier.scale(0.8f))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Time-based", style = MaterialTheme.typography.bodySmall)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = builderSets, onValueChange = { builderSets = it }, label = { Text("Sets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).height(56.dp))
                                    if (builderIsTimeBased) {
                                        OutlinedTextField(value = builderDuration, onValueChange = { builderDuration = it }, label = { Text("Secs") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).height(56.dp))
                                    } else {
                                        OutlinedTextField(value = builderReps, onValueChange = { builderReps = it }, label = { Text("Reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).height(56.dp))
                                        OutlinedTextField(value = builderWeight, onValueChange = { builderWeight = it }, label = { Text("Kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).height(56.dp))
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (builderName.isNotBlank()) {
                                            activeExercises = activeExercises + PresetExercise(
                                                name = builderName, sets = builderSets.toIntOrNull() ?: 0, reps = builderReps.toIntOrNull() ?: 0,
                                                weight = builderWeight.toIntOrNull() ?: 0, isTimeBased = builderIsTimeBased, durationInSeconds = builderDuration.toIntOrNull() ?: 0
                                            )
                                            builderName = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) { Text("+ Add to List") }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    if (activePresetId != 0) {
                                        TextButton(
                                            onClick = {
                                                allPresets.find { it.id == activePresetId }?.let { viewModel.deletePreset(it) }
                                                activePresetId = 0; activePresetName = ""; activeExercises = emptyList()
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) { Text("Delete") }
                                    } else Spacer(modifier = Modifier.width(8.dp))

                                    Row {
                                        OutlinedButton(onClick = {
                                            if (activeExercises.isNotEmpty()) viewModel.logPresetExercises(activePresetName.ifBlank { "Custom Preset" }, activeExercises)
                                            showExerciseDialog = false
                                        }) { Text("Log") }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(onClick = {
                                            if (activePresetName.isNotBlank() && activeExercises.isNotEmpty()) {
                                                viewModel.savePreset(activePresetId, activePresetName, activeExercises)
                                                showExerciseDialog = false
                                            }
                                        }) { Text("Save") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Health & Fitness", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Workout & Gym", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = {
                                selectedTabIndex = 0
                                showExerciseDialog = true
                            }) { Text("+ Log") }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (todayExercises.isEmpty()) {
                            Text("No exercises logged today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            val groupedExercises = todayExercises.groupBy { it.presetName }

                            groupedExercises.forEach { (presetName, exercises) ->
                                if (presetName != null) {
                                    Text(
                                        text = presetName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {},
                                                onLongClick = {
                                                    loggedPresetToManage = presetName
                                                    showLoggedPresetDialog = true
                                                }
                                            )
                                            .padding(top = 16.dp, bottom = 4.dp)
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                                }

                                exercises.forEach { exercise ->
                                    // FIX: The key block prevents state from jumping to the wrong item when deleted
                                    key(exercise.id) {
                                        val dismissState = rememberSwipeToDismissBoxState(
                                            confirmValueChange = { dismissValue ->
                                                when (dismissValue) {
                                                    SwipeToDismissBoxValue.EndToStart -> {
                                                        viewModel.deleteExercise(exercise)
                                                        true // Confirm delete
                                                    }
                                                    SwipeToDismissBoxValue.StartToEnd -> {
                                                        loggedExerciseToEdit = exercise
                                                        false // Bounce back, we show dialog instead
                                                    }
                                                    else -> false
                                                }
                                            }
                                        )

                                        SwipeToDismissBox(
                                            state = dismissState,
                                            enableDismissFromStartToEnd = true, // Enable Left-to-Right swipe for Edit
                                            enableDismissFromEndToStart = true, // Enable Right-to-Left swipe for Delete
                                            backgroundContent = {
                                                val direction = dismissState.dismissDirection
                                                val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                                    MaterialTheme.colorScheme.error
                                                } else if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color.Transparent
                                                }

                                                val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                                val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Edit else Icons.Default.Delete

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(vertical = 4.dp)
                                                        .background(color, RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 16.dp),
                                                    contentAlignment = alignment
                                                ) {
                                                    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
                                                    }
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = exercise.exerciseName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                                    if (exercise.isTimeBased) {
                                                        Text("${exercise.sets} Sets × ${exercise.durationInSeconds} Seconds", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                                    } else {
                                                        Text("${exercise.sets} Sets × ${exercise.reps} Reps  |  ${exercise.weight} kg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}