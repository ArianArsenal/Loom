package com.example.loom.ui.health

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.ExerciseTemplate
import com.example.loom.data.local.entity.PresetExercise
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Collections

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HealthWorkout(viewModel: HealthViewModel) {
    val context = LocalContext.current
    val todayExercises by viewModel.todayExercises.collectAsState()
    val allPresets by viewModel.allPresets.collectAsState()
    val globalTemplates by viewModel.exerciseTemplates.collectAsState()

    var showExerciseDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var templateToEdit by remember { mutableStateOf<ExerciseTemplate?>(null) }
    var showTemplateEditDialog by remember { mutableStateOf(false) }

    var activePresetId by remember { mutableIntStateOf(0) }
    var activePresetName by remember { mutableStateOf("") }
    var activeExercises by remember { mutableStateOf(listOf<PresetExercise>()) }
    var isCurrentPresetSaved by remember { mutableStateOf(true) }
    var showUnsavedPrompt by remember { mutableStateOf(false) }

    var presetExerciseToEdit by remember { mutableStateOf<PresetExercise?>(null) }
    var presetExerciseToEditIndex by remember { mutableIntStateOf(-1) }

    var loggedPresetToManage by remember { mutableStateOf<String?>(null) }
    var showLoggedPresetDialog by remember { mutableStateOf(false) }
    var loggedExerciseToEdit by remember { mutableStateOf<Exercise?>(null) }

    if (showTemplateEditDialog && templateToEdit != null) {
        var editName by remember { mutableStateOf(templateToEdit!!.name) }
        AlertDialog(
            onDismissRequest = { showTemplateEditDialog = false },
            title = { Text("Edit Global Exercise") },
            text = { OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Exercise Name") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) viewModel.updateTemplate(templateToEdit!!.copy(name = editName))
                    showTemplateEditDialog = false; templateToEdit = null
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.deleteTemplate(templateToEdit!!); showTemplateEditDialog = false; templateToEdit = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                    TextButton(onClick = { showTemplateEditDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    if (showLoggedPresetDialog && loggedPresetToManage != null) {
        AlertDialog(
            onDismissRequest = { showLoggedPresetDialog = false },
            title = { Text("Manage '${loggedPresetToManage}'") },
            text = { Text("Would you like to edit the preset template or remove today's logs?") },
            confirmButton = {
                Button(onClick = {
                    val targetPreset = allPresets.find { it.presetName == loggedPresetToManage }
                    if (targetPreset != null) { activePresetId = targetPreset.id; activePresetName = targetPreset.presetName; activeExercises = targetPreset.exercises }
                    else { activePresetId = 0; activePresetName = loggedPresetToManage ?: ""; activeExercises = emptyList() }
                    selectedTabIndex = 1; showExerciseDialog = true; showLoggedPresetDialog = false; loggedPresetToManage = null
                }) { Text("Edit Preset Template") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        todayExercises.filter { it.presetName == loggedPresetToManage }.forEach { viewModel.deleteExercise(it) }
                        showLoggedPresetDialog = false; loggedPresetToManage = null
                    }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete Logs") }
                    TextButton(onClick = { showLoggedPresetDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    if (loggedExerciseToEdit != null) {
        var editName by remember { mutableStateOf(loggedExerciseToEdit!!.exerciseName) }
        var editIsTimeBased by remember { mutableStateOf(loggedExerciseToEdit!!.isTimeBased) }
        var editSets by remember { mutableStateOf(loggedExerciseToEdit!!.sets.toString()) }
        var editReps by remember { mutableStateOf(loggedExerciseToEdit!!.reps.toString()) }
        var editDuration by remember { mutableStateOf(loggedExerciseToEdit!!.durationInSeconds.toString()) }
        var editVaryWeight by remember { mutableStateOf(loggedExerciseToEdit!!.weightListJson != "[]") }
        var editWeight by remember { mutableStateOf(loggedExerciseToEdit!!.weight.formatWeight()) }

        // --- FIX: Restored explicit <Map<Int, String>> type definition ---
        var editAdvancedWeights by remember {
            mutableStateOf(
                try {
                    val list = Json.decodeFromString<List<Float>>(loggedExerciseToEdit!!.weightListJson)
                    list.mapIndexed { index, w -> index to w.formatWeight() }.toMap()
                } catch(_: Exception) { emptyMap<Int, String>() } // <-- Explicit type moved here!
            )
        }

        AlertDialog(
            onDismissRequest = { loggedExerciseToEdit = null },
            title = { Text("Edit Logged Exercise") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = editVaryWeight, onCheckedChange = { editVaryWeight = it }, modifier = Modifier.scale(0.8f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Different weight per set", style = MaterialTheme.typography.bodySmall)
                        }

                        if (editVaryWeight) {
                            val setCount = editSets.toIntOrNull() ?: 0
                            if (setCount > 0) {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (i in 0 until setCount) {
                                        OutlinedTextField(
                                            value = editAdvancedWeights[i] ?: "",
                                            onValueChange = { newVal -> editAdvancedWeights = editAdvancedWeights.toMutableMap().apply { put(i, newVal) } },
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
                                value = editWeight,
                                onValueChange = { editWeight = it },
                                label = { Text("Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) {
                        val wList = if (editVaryWeight) {
                            val setCount = editSets.toIntOrNull() ?: 0
                            (0 until setCount).map { editAdvancedWeights[it]?.toFloatOrNull() ?: 0f }
                        } else emptyList()

                        val updatedEx = loggedExerciseToEdit!!.copy(
                            exerciseName = editName,
                            sets = editSets.toIntOrNull() ?: 0,
                            reps = editReps.toIntOrNull() ?: 0,
                            weight = editWeight.toFloatOrNull() ?: 0f,
                            weightListJson = Json.encodeToString(wList),
                            isTimeBased = editIsTimeBased,
                            durationInSeconds = editDuration.toIntOrNull() ?: 0
                        )
                        viewModel.updateExercise(updatedEx)
                        loggedExerciseToEdit = null
                    }
                }) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { loggedExerciseToEdit = null }) { Text("Cancel") } }
        )
    }

    if (showUnsavedPrompt) {
        AlertDialog(
            onDismissRequest = { showUnsavedPrompt = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You made changes to this preset. Would you like to save it to your list before logging?") },
            confirmButton = {
                Button(onClick = {
                    if (activePresetName.isNotBlank()) {
                        viewModel.savePreset(activePresetId, activePresetName, activeExercises)
                        isCurrentPresetSaved = true
                    }
                    viewModel.logPresetExercises(activePresetName.ifBlank { "Custom Preset" }, activeExercises)
                    showUnsavedPrompt = false
                    showExerciseDialog = false
                }) { Text("Save & Log") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.logPresetExercises(activePresetName.ifBlank { "Custom Preset" }, activeExercises)
                    showUnsavedPrompt = false
                    showExerciseDialog = false
                }) { Text("Log Only") }
            }
        )
    }

    if (presetExerciseToEdit != null) {
        var editIsTimeBased by remember { mutableStateOf(presetExerciseToEdit!!.isTimeBased) }
        var editSets by remember { mutableStateOf(presetExerciseToEdit!!.sets.toString()) }
        var editReps by remember { mutableStateOf(presetExerciseToEdit!!.reps.toString()) }
        var editDuration by remember { mutableStateOf(presetExerciseToEdit!!.durationInSeconds.toString()) }
        var editVaryWeight by remember { mutableStateOf(presetExerciseToEdit!!.weightListJson != "[]") }
        var editWeight by remember { mutableStateOf(presetExerciseToEdit!!.weight.formatWeight()) }

        // --- FIX: Restored explicit <Map<Int, String>> type definition ---
        var editAdvancedWeights by remember {
            mutableStateOf(
                try {
                    val list = Json.decodeFromString<List<Float>>(loggedExerciseToEdit!!.weightListJson)
                    list.mapIndexed { index, w -> index to w.formatWeight() }.toMap()
                } catch(_: Exception) { emptyMap<Int, String>() } // <-- Explicit type moved here!
            )
        }

        AlertDialog(
            onDismissRequest = { presetExerciseToEdit = null },
            title = { Text("Edit ${presetExerciseToEdit!!.name}") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = editVaryWeight, onCheckedChange = { editVaryWeight = it }, modifier = Modifier.scale(0.8f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Different weight per set", style = MaterialTheme.typography.bodySmall)
                        }

                        if (editVaryWeight) {
                            val setCount = editSets.toIntOrNull() ?: 0
                            if (setCount > 0) {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (i in 0 until setCount) {
                                        OutlinedTextField(
                                            value = editAdvancedWeights[i] ?: "",
                                            onValueChange = { newVal -> editAdvancedWeights = editAdvancedWeights.toMutableMap().apply { put(i, newVal) } },
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
                                value = editWeight,
                                onValueChange = { editWeight = it },
                                label = { Text("Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val wList = if (editVaryWeight) {
                        val setCount = editSets.toIntOrNull() ?: 0
                        (0 until setCount).map { editAdvancedWeights[it]?.toFloatOrNull() ?: 0f }
                    } else emptyList()

                    val updatedEx = PresetExercise(
                        name = presetExerciseToEdit!!.name,
                        sets = editSets.toIntOrNull() ?: 0, reps = editReps.toIntOrNull() ?: 0,
                        weight = editWeight.toFloatOrNull() ?: 0f, weightListJson = Json.encodeToString(wList),
                        isTimeBased = editIsTimeBased, durationInSeconds = editDuration.toIntOrNull() ?: 0
                    )
                    val newList = activeExercises.toMutableList()
                    newList[presetExerciseToEditIndex] = updatedEx
                    activeExercises = newList
                    isCurrentPresetSaved = false
                    presetExerciseToEdit = null
                }) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { presetExerciseToEdit = null }) { Text("Cancel") } }
        )
    }

    if (showExerciseDialog) {
        Dialog(
            onDismissRequest = { showExerciseDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .imePadding()
                    .padding(vertical = 24.dp)
                    .heightIn(max = 750.dp)
                    .wrapContentHeight()
                    .animateContentSize(),
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
                            var inputName by remember { mutableStateOf("") }
                            var expanded by remember { mutableStateOf(false) }
                            var isTimeBased by remember { mutableStateOf(false) }
                            var inputSets by remember { mutableStateOf("3") }
                            var inputReps by remember { mutableStateOf("10") }
                            var inputDuration by remember { mutableStateOf("60") }
                            var varyWeight by remember { mutableStateOf(false) }
                            var inputWeight by remember { mutableStateOf("60") }
                            var advancedWeights by remember { mutableStateOf(mapOf<Int, String>()) }

                            Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                                Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                                        OutlinedTextField(
                                            value = inputName, onValueChange = { inputName = it; expanded = true },
                                            label = { Text("Search Exercise") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                                        )
                                        val filtered = globalTemplates.filter { it.name.contains(inputName, true) }
                                        val exactMatch = globalTemplates.any { it.name.equals(inputName, ignoreCase = true) }

                                        if ((filtered.isNotEmpty() || (inputName.isNotBlank() && !exactMatch)) && expanded) {
                                            ExposedDropdownMenu(expanded = true, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                                                filtered.forEach { option ->
                                                    Box(modifier = Modifier.fillMaxWidth().combinedClickable(
                                                        onClick = { inputName = option.name; expanded = false },
                                                        onLongClick = { templateToEdit = option; showTemplateEditDialog = true }
                                                    ).padding(16.dp)) { Text(option.name) }
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
                                                value = inputWeight,
                                                onValueChange = { inputWeight = it },
                                                label = { Text("Weight (kg)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { showExerciseDialog = false }) { Text("Cancel") }
                                    Button(onClick = {
                                        if (inputName.isNotBlank()) {
                                            val wList = if (varyWeight) {
                                                val setCount = inputSets.toIntOrNull() ?: 0
                                                (0 until setCount).map { advancedWeights[it]?.toFloatOrNull() ?: 0f }
                                            } else emptyList()

                                            viewModel.addExercise(
                                                name = inputName, sets = inputSets.toIntOrNull() ?: 0, reps = inputReps.toIntOrNull() ?: 0,
                                                weight = inputWeight.toFloatOrNull() ?: 0f, weightListJson = Json.encodeToString(wList),
                                                isTimeBased = isTimeBased, duration = inputDuration.toIntOrNull() ?: 0
                                            )
                                        }
                                        showExerciseDialog = false
                                    }) { Text("Log Exercise") }
                                }
                            }
                        } else {
                            var presetDropdownExpanded by remember { mutableStateOf(false) }
                            var builderName by remember { mutableStateOf("") }
                            var builderExpanded by remember { mutableStateOf(false) }
                            var builderIsTimeBased by remember { mutableStateOf(false) }
                            var builderSets by remember { mutableStateOf("3") }
                            var builderReps by remember { mutableStateOf("10") }
                            var builderDuration by remember { mutableStateOf("60") }
                            var builderVaryWeight by remember { mutableStateOf(false) }
                            var builderWeight by remember { mutableStateOf("60") }
                            var builderAdvancedWeights by remember { mutableStateOf(mapOf<Int, String>()) }

                            Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                                Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        ExposedDropdownMenuBox(expanded = presetDropdownExpanded, onExpandedChange = { presetDropdownExpanded = it }, modifier = Modifier.weight(1f)) {
                                            OutlinedTextField(
                                                value = if (activePresetId == 0) "Create New Preset..." else "Editing: $activePresetName",
                                                onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetDropdownExpanded) },
                                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                            )
                                            ExposedDropdownMenu(expanded = presetDropdownExpanded, onDismissRequest = { presetDropdownExpanded = false }) {
                                                allPresets.forEach { preset ->
                                                    DropdownMenuItem(text = { Text(preset.presetName) }, onClick = {
                                                        activePresetId = preset.id; activePresetName = preset.presetName; activeExercises = preset.exercises
                                                        isCurrentPresetSaved = true
                                                        presetDropdownExpanded = false
                                                    })
                                                }
                                            }
                                        }
                                        if (activePresetId != 0 || activeExercises.isNotEmpty()) {
                                            IconButton(onClick = { activePresetId = 0; activePresetName = ""; activeExercises = emptyList(); isCurrentPresetSaved = true }) { Icon(Icons.Default.Add, contentDescription = "New") }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(value = activePresetName, onValueChange = { activePresetName = it; isCurrentPresetSaved = false }, label = { Text("Preset Title (e.g., Push Day)") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(8.dp)) {
                                        if (activeExercises.isEmpty()) {
                                            Text("No exercises in this preset.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                                        }
                                        activeExercises.forEachIndexed { idx, ex ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).combinedClickable(
                                                    onClick = {},
                                                    onLongClick = { presetExerciseToEditIndex = idx; presetExerciseToEdit = ex }
                                                ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    IconButton(
                                                        onClick = {
                                                            if (idx > 0) {
                                                                val newList = activeExercises.toMutableList()
                                                                Collections.swap(newList, idx, idx - 1)
                                                                activeExercises = newList
                                                                isCurrentPresetSaved = false
                                                            }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up") }
                                                    IconButton(
                                                        onClick = {
                                                            if (idx < activeExercises.size - 1) {
                                                                val newList = activeExercises.toMutableList()
                                                                Collections.swap(newList, idx, idx + 1)
                                                                activeExercises = newList
                                                                isCurrentPresetSaved = false
                                                            }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down") }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(ex.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                    if (ex.isTimeBased) {
                                                        Text("${ex.sets} Sets × ${ex.durationInSeconds} Secs", style = MaterialTheme.typography.bodySmall)
                                                    } else {
                                                        val wList = try { Json.decodeFromString<List<Float>>(ex.weightListJson) } catch(_:Exception){ emptyList() }
                                                        if (wList.isNotEmpty()) {
                                                            Text("${ex.sets} Sets × ${ex.reps} Reps @ ${wList.joinToString(", ") { it.formatWeight() }}kg", style = MaterialTheme.typography.bodySmall)
                                                        } else {
                                                            Text("${ex.sets} Sets × ${ex.reps} Reps @ ${ex.weight.formatWeight()}kg", style = MaterialTheme.typography.bodySmall)
                                                        }
                                                    }
                                                }
                                                IconButton(onClick = { activeExercises = activeExercises.filter { it != ex }; isCurrentPresetSaved = false }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error) }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Add Exercise to Preset:", style = MaterialTheme.typography.labelMedium)

                                    ExposedDropdownMenuBox(expanded = builderExpanded, onExpandedChange = { builderExpanded = it }) {
                                        OutlinedTextField(
                                            value = builderName, onValueChange = { builderName = it; builderExpanded = true }, label = { Text("Search Exercise") },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth().height(56.dp)
                                        )
                                        val filtered = globalTemplates.filter { it.name.contains(builderName, true) }
                                        val exactMatch = globalTemplates.any { it.name.equals(builderName, ignoreCase = true) }

                                        if ((filtered.isNotEmpty() || (builderName.isNotBlank() && !exactMatch)) && builderExpanded) {
                                            ExposedDropdownMenu(expanded = true, onDismissRequest = { builderExpanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                                                filtered.forEach { option ->
                                                    Box(modifier = Modifier.fillMaxWidth().combinedClickable(
                                                        onClick = { builderName = option.name; builderExpanded = false },
                                                        onLongClick = { templateToEdit = option; showTemplateEditDialog = true }
                                                    ).padding(16.dp)) { Text(option.name) }
                                                }
                                                if (builderName.isNotBlank() && !exactMatch) {
                                                    Box(modifier = Modifier.fillMaxWidth().clickable {
                                                        viewModel.createExerciseTemplate(builderName)
                                                        builderExpanded = false
                                                    }.padding(16.dp)) {
                                                        Text("Create new exercise: \"$builderName\"", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                    }
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
                                        }
                                    }

                                    if (!builderIsTimeBased) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Switch(checked = builderVaryWeight, onCheckedChange = { builderVaryWeight = it }, modifier = Modifier.scale(0.8f))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Different weight per set", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (builderVaryWeight) {
                                            val setCount = builderSets.toIntOrNull() ?: 0
                                            if (setCount > 0) {
                                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    for (i in 0 until setCount) {
                                                        OutlinedTextField(
                                                            value = builderAdvancedWeights[i] ?: "",
                                                            onValueChange = { newVal -> builderAdvancedWeights = builderAdvancedWeights.toMutableMap().apply { put(i, newVal) } },
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
                                                value = builderWeight,
                                                onValueChange = { builderWeight = it },
                                                label = { Text("Weight (kg)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (builderName.isNotBlank()) {
                                                val wList = if (builderVaryWeight) {
                                                    val setCount = builderSets.toIntOrNull() ?: 0
                                                    (0 until setCount).map { builderAdvancedWeights[it]?.toFloatOrNull() ?: 0f }
                                                } else emptyList()

                                                activeExercises = activeExercises + PresetExercise(
                                                    name = builderName, sets = builderSets.toIntOrNull() ?: 0, reps = builderReps.toIntOrNull() ?: 0,
                                                    weight = builderWeight.toFloatOrNull() ?: 0f, weightListJson = Json.encodeToString(wList),
                                                    isTimeBased = builderIsTimeBased, durationInSeconds = builderDuration.toIntOrNull() ?: 0
                                                )
                                                builderName = ""
                                                isCurrentPresetSaved = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) { Text("+ Add to List") }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
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
                                            if (activeExercises.isNotEmpty()) {
                                                if (!isCurrentPresetSaved) {
                                                    showUnsavedPrompt = true
                                                } else {
                                                    viewModel.logPresetExercises(activePresetName.ifBlank { "Custom Preset" }, activeExercises)
                                                    showExerciseDialog = false
                                                }
                                            }
                                        }) { Text("Log") }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(onClick = {
                                            if (activePresetName.isNotBlank() && activeExercises.isNotEmpty()) {
                                                viewModel.savePreset(activePresetId, activePresetName, activeExercises)
                                                isCurrentPresetSaved = true
                                                Toast.makeText(context, "Preset Saved!", Toast.LENGTH_SHORT).show()
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

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Workout & Gym", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { selectedTabIndex = 0; showExerciseDialog = true }) { Text("+ Log") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (todayExercises.isEmpty()) {
                Text("No exercises logged today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                val groupedExercises = todayExercises.groupBy { it.presetName }
                groupedExercises.forEach { (presetName, exercises) ->
                    val displayTitle = if (presetName.isNullOrBlank()) "Single Exercises" else presetName
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().combinedClickable(
                            onClick = {},
                            onLongClick = {
                                if (!presetName.isNullOrBlank()) {
                                    loggedPresetToManage = presetName
                                    showLoggedPresetDialog = true
                                }
                            }
                        ).padding(top = 16.dp, bottom = 4.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                    exercises.forEach { exercise ->
                        val wList = remember(exercise.weightListJson) {
                            try { Json.decodeFromString<List<Float>>(exercise.weightListJson) } catch(_:Exception){ emptyList() }
                        }

                        key(exercise.id) {
                            val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { dismissValue -> when (dismissValue) { SwipeToDismissBoxValue.EndToStart -> { viewModel.deleteExercise(exercise); true }; SwipeToDismissBoxValue.StartToEnd -> { loggedExerciseToEdit = exercise; false }; else -> false } })
                            SwipeToDismissBox(
                                state = dismissState, enableDismissFromStartToEnd = true, enableDismissFromEndToStart = true,
                                backgroundContent = {
                                    val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) MaterialTheme.colorScheme.primary else Color.Transparent
                                    val align = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                    Box(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp).background(color, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp), contentAlignment = align) {
                                        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) Icon(if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Edit else Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onError)
                                    }
                                }
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {

                                    val isChecked = exercise.isCompleted

                                    IconButton(onClick = { viewModel.toggleExerciseCompletion(exercise) }) {
                                        Icon(
                                            imageVector = if (isChecked) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                                            contentDescription = "Complete",
                                            tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = exercise.exerciseName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None)
                                        if (exercise.isTimeBased) {
                                            Text("${exercise.sets} Sets × ${exercise.durationInSeconds} Seconds", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                        } else {
                                            if (wList.isNotEmpty()) {
                                                Text("${exercise.sets} Sets × ${exercise.reps} Reps  |  ${wList.joinToString(", ") { it.formatWeight() }} kg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                            } else {
                                                Text("${exercise.sets} Sets × ${exercise.reps} Reps  |  ${exercise.weight.formatWeight()} kg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
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