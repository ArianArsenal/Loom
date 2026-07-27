package com.example.loom.ui.health

import android.widget.NumberPicker
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.ExerciseTemplate
import com.example.loom.data.local.entity.FoodItem
import com.example.loom.data.local.entity.PresetExercise
import com.example.loom.data.local.entity.SkincareProduct
import com.example.loom.data.local.entity.SkincareRoutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.Locale

@Composable
fun ReorderableProductList(
    products: List<String>,
    onReorder: (List<String>) -> Unit,
    onRemove: ((String) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        products.forEachIndexed { index, product ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column {
                    IconButton(
                        onClick = {
                            if (index > 0) {
                                val newList = products.toMutableList()
                                Collections.swap(newList, index, index - 1)
                                onReorder(newList)
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up") }
                    IconButton(
                        onClick = {
                            if (index < products.size - 1) {
                                val newList = products.toMutableList()
                                Collections.swap(newList, index, index + 1)
                                onReorder(newList)
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down") }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(product, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)

                if (onRemove != null) {
                    IconButton(onClick = { onRemove(product) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HealthScreen(viewModel: HealthViewModel) {
    val todayRecord by viewModel.todayRecord.collectAsState()
    val todayExercises by viewModel.todayExercises.collectAsState()
    val allPresets by viewModel.allPresets.collectAsState()
    val globalTemplates by viewModel.exerciseTemplates.collectAsState()

    val skincareRoutines by viewModel.allSkincareRoutines.collectAsState()
    val skincareProducts by viewModel.allSkincareProducts.collectAsState()

    val allFoodItems by viewModel.allFoodItems.collectAsState()
    val defaultWaterIncrement by viewModel.defaultWaterIncrement.collectAsState()

    // Bio States
    val weight by viewModel.weight.collectAsState()
    val height by viewModel.height.collectAsState()
    val age by viewModel.age.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val activity by viewModel.activity.collectAsState()
    val goal by viewModel.goal.collectAsState()

    var showExerciseDialog by remember { mutableStateOf(false) }
    var templateToEdit by remember { mutableStateOf<ExerciseTemplate?>(null) }
    var showTemplateEditDialog by remember { mutableStateOf(false) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var activePresetId by remember { mutableIntStateOf(0) }
    var activePresetName by remember { mutableStateOf("") }
    var activeExercises by remember { mutableStateOf(listOf<PresetExercise>()) }

    var loggedPresetToManage by remember { mutableStateOf<String?>(null) }
    var showLoggedPresetDialog by remember { mutableStateOf(false) }
    var loggedExerciseToEdit by remember { mutableStateOf<Exercise?>(null) }

    var showSkincareDialog by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<SkincareRoutine?>(null) }
    var productToDelete by remember { mutableStateOf<SkincareProduct?>(null) }

    // Food Dialog States
    var showFoodDialog by remember { mutableStateOf(false) }
    var showFoodPantryDialog by remember { mutableStateOf(false) }
    var foodToLog by remember { mutableStateOf<FoodItem?>(null) }
    var showNewFoodDialog by remember { mutableStateOf(false) }
    var foodToDelete by remember { mutableStateOf<FoodItem?>(null) }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product?") },
            text = { Text("Are you sure you want to permanently delete '${productToDelete?.name}' from your global library?") },
            confirmButton = {
                Button(onClick = { viewModel.deleteSkincareProduct(productToDelete!!); productToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { productToDelete = null }) { Text("Cancel") } }
        )
    }

    if (foodToDelete != null) {
        AlertDialog(
            onDismissRequest = { foodToDelete = null },
            title = { Text("Delete Food?") },
            text = { Text("Permanently delete '${foodToDelete?.name}' from the pantry?") },
            confirmButton = {
                Button(onClick = { viewModel.deleteFoodItem(foodToDelete!!); foodToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { foodToDelete = null }) { Text("Cancel") } }
        )
    }

    if (foodToLog != null && showFoodDialog) {
        var servingsStr by remember { mutableStateOf("1") }
        AlertDialog(
            onDismissRequest = { showFoodDialog = false; foodToLog = null },
            title = { Text("Log ${foodToLog!!.name}") },
            text = {
                Column {
                    Text("1 ${foodToLog!!.servingUnit} = ${foodToLog!!.calories} kcal", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = servingsStr,
                        onValueChange = { servingsStr = it },
                        label = { Text("How many ${foodToLog!!.servingUnit}s?") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val multiplier = servingsStr.toFloatOrNull() ?: 1f
                    viewModel.logFoodItem(foodToLog!!, multiplier)
                    showFoodDialog = false
                    foodToLog = null
                }) { Text("Log Meal") }
            },
            dismissButton = { TextButton(onClick = { showFoodDialog = false; foodToLog = null }) { Text("Cancel") } }
        )
    }

    // --- PANTRY MANAGER DIALOG ---
    if (showFoodPantryDialog) {
        AlertDialog(
            onDismissRequest = { showFoodPantryDialog = false },
            title = { Text("Edit Pantry") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { TextButton(onClick = { showNewFoodDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Create New Food") } }
                    items(allFoodItems) { food ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { foodToLog = food; showFoodDialog = true; showFoodPantryDialog = false },
                                onLongClick = { foodToDelete = food }
                            )
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(food.emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(food.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("${food.calories} kcal / ${food.servingUnit}", style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { foodToDelete = food }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showFoodPantryDialog = false }) { Text("Close") } }
        )
    }

    if (showNewFoodDialog) {
        var inputEmoji by remember { mutableStateOf("🍽️") }
        var inputName by remember { mutableStateOf("") }
        var inputServing by remember { mutableStateOf("Plate") }
        var inputCals by remember { mutableStateOf("") }
        var inputPro by remember { mutableStateOf("") }
        var inputCarbs by remember { mutableStateOf("") }
        var inputFats by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewFoodDialog = false },
            title = { Text("Add to Pantry") },
            text = {
                LazyColumn {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = inputEmoji, onValueChange = { inputEmoji = it.take(2) }, label = { Text("Emoji") }, modifier = Modifier.weight(0.3f), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp))
                            OutlinedTextField(value = inputName, onValueChange = { inputName = it }, label = { Text("Food Name") }, modifier = Modifier.weight(0.7f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = inputServing, onValueChange = { inputServing = it }, label = { Text("Serving Unit (e.g. Plate, Scoop, 100g)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = inputCals, onValueChange = { inputCals = it }, label = { Text("Kcal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = inputPro, onValueChange = { inputPro = it }, label = { Text("Pro (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = inputCarbs, onValueChange = { inputCarbs = it }, label = { Text("Carb (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = inputFats, onValueChange = { inputFats = it }, label = { Text("Fat (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (inputName.isNotBlank() && inputCals.isNotBlank()) {
                        viewModel.addFoodItem(
                            FoodItem(
                                emoji = inputEmoji, name = inputName, servingUnit = inputServing,
                                calories = inputCals.toIntOrNull() ?: 0, protein = inputPro.toIntOrNull() ?: 0,
                                carbs = inputCarbs.toIntOrNull() ?: 0, fats = inputFats.toIntOrNull() ?: 0
                            )
                        )
                        showNewFoodDialog = false
                    }
                }) { Text("Save Food") }
            },
            dismissButton = { TextButton(onClick = { showNewFoodDialog = false }) { Text("Cancel") } }
        )
    }

    if (showSkincareDialog) {
        var inputRoutineName by remember(routineToEdit) { mutableStateOf(routineToEdit?.name ?: "") }
        var isPinnedToHome by remember(routineToEdit) { mutableStateOf(routineToEdit?.isPinnedToHome ?: true) }
        val initialProducts = try { Json.decodeFromString<List<String>>(routineToEdit?.productsJson ?: "[]") } catch(e: Exception){ emptyList() }
        var selectedProducts by remember(routineToEdit) { mutableStateOf(initialProducts) }

        var routineDropdownExpanded by remember { mutableStateOf(false) }
        var productSearchText by remember { mutableStateOf("") }
        var productDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSkincareDialog = false; routineToEdit = null },
            title = { Text(if (routineToEdit == null) "New Skincare Routine" else "Edit Routine") },
            text = {
                LazyColumn {
                    item {
                        ExposedDropdownMenuBox(expanded = routineDropdownExpanded, onExpandedChange = { routineDropdownExpanded = it }) {
                            OutlinedTextField(
                                value = inputRoutineName, onValueChange = { inputRoutineName = it; routineDropdownExpanded = true },
                                label = { Text("Routine Name (e.g., Morning Wash)") }, modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routineDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(), singleLine = true
                            )
                            val filteredRoutines = skincareRoutines.filter { it.name.contains(inputRoutineName, true) }
                            if (filteredRoutines.isNotEmpty() && routineDropdownExpanded) {
                                ExposedDropdownMenu(expanded = routineDropdownExpanded, onDismissRequest = { routineDropdownExpanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                                    filteredRoutines.forEach { routine ->
                                        DropdownMenuItem(text = { Text(routine.name) }, onClick = { routineToEdit = routine; routineDropdownExpanded = false })
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = isPinnedToHome, onCheckedChange = { isPinnedToHome = it }, modifier = Modifier.scale(0.8f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pin to Home Screen", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Products (Application Order):", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        ReorderableProductList(
                            products = selectedProducts,
                            onReorder = { newOrder -> selectedProducts = newOrder },
                            onRemove = { removedProduct -> selectedProducts = selectedProducts.filter { it != removedProduct } }
                        )

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExposedDropdownMenuBox(expanded = productDropdownExpanded, onExpandedChange = { productDropdownExpanded = it }, modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = productSearchText, onValueChange = { productSearchText = it; productDropdownExpanded = true },
                                    placeholder = { Text("Search or add...") }, modifier = Modifier.menuAnchor().fillMaxWidth(), singleLine = true
                                )
                                val filteredProducts = skincareProducts.filter { it.name.contains(productSearchText, true) && !selectedProducts.contains(it.name) }
                                if (filteredProducts.isNotEmpty() && productDropdownExpanded) {
                                    ExposedDropdownMenu(expanded = productDropdownExpanded, onDismissRequest = { productDropdownExpanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                                        filteredProducts.forEach { product ->
                                            Box(modifier = Modifier.fillMaxWidth().combinedClickable(
                                                onClick = { selectedProducts = selectedProducts + product.name; productSearchText = ""; productDropdownExpanded = false },
                                                onLongClick = { productToDelete = product }
                                            ).padding(16.dp)) { Text(product.name) }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                val trimmed = productSearchText.trim()
                                if (trimmed.isNotBlank() && !selectedProducts.contains(trimmed)) {
                                    viewModel.addSkincareProduct(trimmed)
                                    selectedProducts = selectedProducts + trimmed
                                    productSearchText = ""
                                    productDropdownExpanded = false
                                }
                            }) { Icon(Icons.Default.Add, "Add Product", tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (inputRoutineName.isNotBlank()) {
                        viewModel.saveSkincareRoutine(SkincareRoutine(id = routineToEdit?.id ?: 0, name = inputRoutineName, isPinnedToHome = isPinnedToHome, productsJson = Json.encodeToString(selectedProducts)))
                        showSkincareDialog = false; routineToEdit = null
                    }
                }) { Text("Save Routine") }
            },
            dismissButton = {
                Row {
                    if (routineToEdit != null) {
                        TextButton(onClick = { viewModel.deleteSkincareRoutine(routineToEdit!!); showSkincareDialog = false; routineToEdit = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                    }
                    TextButton(onClick = { showSkincareDialog = false; routineToEdit = null }) { Text("Cancel") }
                }
            }
        )
    }

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
                            update = { it.value = editWeight }, modifier = Modifier.fillMaxWidth().height(120.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) {
                        viewModel.deleteExercise(loggedExerciseToEdit!!)
                        viewModel.addExercise(name = editName, sets = editSets.toIntOrNull() ?: 0, reps = editReps.toIntOrNull() ?: 0, weight = editWeight, isTimeBased = editIsTimeBased, duration = editDuration.toIntOrNull() ?: 0, presetName = loggedExerciseToEdit!!.presetName)
                        loggedExerciseToEdit = null
                    }
                }) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { loggedExerciseToEdit = null }) { Text("Cancel") } }
        )
    }

    if (showExerciseDialog) {
        Dialog(onDismissRequest = { showExerciseDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
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
                            var inputWeight by remember { mutableStateOf(60) }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                                    OutlinedTextField(
                                        value = inputName, onValueChange = { inputName = it; expanded = true },
                                        label = { Text("Search Exercise") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    val filtered = globalTemplates.filter { it.name.contains(inputName, true) }
                                    if (filtered.isNotEmpty() && expanded) {
                                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 240.dp)) {
                                            filtered.forEach { option ->
                                                Box(modifier = Modifier.fillMaxWidth().combinedClickable(
                                                    onClick = { inputName = option.name; expanded = false },
                                                    onLongClick = { templateToEdit = option; showTemplateEditDialog = true }
                                                ).padding(16.dp)) { Text(option.name) }
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
                                        factory = { context ->
                                            NumberPicker(context).apply {
                                                minValue = 0;
                                                maxValue = 300;
                                                value = inputWeight;
                                                setOnValueChangedListener { _, _, newVal -> inputWeight = newVal } // <-- Fixed from editWeight to inputWeight
                                            }
                                        },
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
                                        IconButton(onClick = { activePresetId = 0; activePresetName = ""; activeExercises = emptyList() }) { Icon(Icons.Default.Add, contentDescription = "New") }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = activePresetName, onValueChange = { activePresetName = it }, label = { Text("Preset Title (e.g., Push Day)") }, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(12.dp))

                                LazyColumn(modifier = Modifier.heightIn(min = 50.dp, max = 150.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(8.dp)) {
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
                                                Box(modifier = Modifier.fillMaxWidth().combinedClickable(
                                                    onClick = { builderName = option.name; builderExpanded = false },
                                                    onLongClick = { templateToEdit = option; showTemplateEditDialog = true }
                                                ).padding(16.dp)) { Text(option.name) }
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

            // 1. WORKOUT CARD
            item {
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
                                if (presetName != null) {
                                    Text(text = presetName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { loggedPresetToManage = presetName; showLoggedPresetDialog = true }).padding(top = 16.dp, bottom = 4.dp))
                                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                                }
                                exercises.forEach { exercise ->
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
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = exercise.exerciseName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                                    if (exercise.isTimeBased) Text("${exercise.sets} Sets × ${exercise.durationInSeconds} Seconds", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                                    else Text("${exercise.sets} Sets × ${exercise.reps} Reps  |  ${exercise.weight} kg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
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

            // 2. FUEL CARD (DONUT CHART WITH EXACT STORED KCAL + TIMELINE WITH SWIPE-TO-DELETE)
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Nutrition Log", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { showFoodPantryDialog = true }) { Text("Edit Pantry") }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        val storedCals = todayRecord?.calories ?: 0
                        val proGrams = todayRecord?.protein ?: 0
                        val carbGrams = todayRecord?.carbs ?: 0
                        val fatGrams = todayRecord?.fats ?: 0
                        val totalMacroCals = (proGrams * 4f) + (carbGrams * 4f) + (fatGrams * 9f)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeW = 14.dp.toPx()
                                    if (totalMacroCals == 0f) {
                                        drawArc(Color.LightGray.copy(alpha=0.3f), 0f, 360f, false, style = Stroke(strokeW))
                                    } else {
                                        var startAngle = -90f
                                        val proAngle = ((proGrams * 4f) / totalMacroCals) * 360f
                                        val carbAngle = ((carbGrams * 4f) / totalMacroCals) * 360f
                                        val fatAngle = ((fatGrams * 9f) / totalMacroCals) * 360f

                                        drawArc(Color(0xFF4CAF50), startAngle, proAngle, false, style = Stroke(strokeW)); startAngle += proAngle
                                        drawArc(Color(0xFFFFA726), startAngle, carbAngle, false, style = Stroke(strokeW)); startAngle += carbAngle
                                        drawArc(Color(0xFFE53935), startAngle, fatAngle, false, style = Stroke(strokeW))
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$storedCals", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text("kcal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(24.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Circle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("${proGrams}g Protein", style = MaterialTheme.typography.bodyMedium) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Circle, null, tint = Color(0xFFFFA726), modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("${carbGrams}g Carbs", style = MaterialTheme.typography.bodyMedium) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Circle, null, tint = Color(0xFFE53935), modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("${fatGrams}g Fats", style = MaterialTheme.typography.bodyMedium) }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Consumed Today", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                        val consumedList = try { Json.decodeFromString<List<com.example.loom.data.local.entity.ConsumedFoodLog>>(todayRecord?.consumedFoodsJson ?: "[]") } catch(e: Exception) { emptyList() }

                        if (consumedList.isEmpty()) {
                            Text("No meals logged yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            consumedList.sortedByDescending { it.timestamp }.forEach { log ->
                                key(log.id) {
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { dismissValue ->
                                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                                viewModel.removeConsumedFoodLog(log)
                                                true
                                            } else false
                                        }
                                    )

                                    SwipeToDismissBox(
                                        state = dismissState,
                                        enableDismissFromStartToEnd = false,
                                        backgroundContent = {
                                            val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else Color.Transparent
                                            Box(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp).background(color, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterEnd) {
                                                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onError)
                                            }
                                        }
                                    ) {
                                        val timeStr = java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(java.util.Date(log.timestamp))
                                        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(log.emoji, fontSize = 24.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${log.foodName} (${log.servings.toString().removeSuffix(".0")})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("${log.calories} kcal • P:${log.protein} C:${log.carbs} F:${log.fats}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. SKINCARE CARD
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Skincare Routines", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { routineToEdit = null; showSkincareDialog = true }) { Text("+ Create") }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val completedRoutines = try { Json.decodeFromString<List<String>>(todayRecord?.completedSkincareRoutinesJson ?: "[]") } catch (e: Exception) { emptyList() }
                        val completedSteps = try { Json.decodeFromString<List<String>>(todayRecord?.completedSkincareStepsJson ?: "[]") } catch (e: Exception) { emptyList() }

                        if (skincareRoutines.isEmpty()) {
                            Text("No skincare routines created.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            skincareRoutines.forEach { routine ->
                                val isRoutineDone = completedRoutines.contains(routine.name)
                                val products = try { Json.decodeFromString<List<String>>(routine.productsJson) } catch (e: Exception) { emptyList() }

                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(checked = isRoutineDone, onCheckedChange = { viewModel.toggleSkincareRoutine(routine.name) })
                                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                            Text(routine.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, textDecoration = if (isRoutineDone) TextDecoration.LineThrough else TextDecoration.None)
                                            if (routine.isPinnedToHome) {
                                                Text("Pinned to Home Screen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                                            }
                                        }
                                        IconButton(onClick = { routineToEdit = routine; showSkincareDialog = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Routine", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    if (products.isNotEmpty()) {
                                        Column(modifier = Modifier.padding(top = 4.dp, start = 48.dp)) {
                                            products.forEach { product ->
                                                val stepId = "${routine.name}_${product}"
                                                val isStepDone = completedSteps.contains(stepId)

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { viewModel.toggleSkincareStep(routine.name, product) }
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    Icon(if (isStepDone) Icons.Outlined.CheckCircle else Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(product, style = MaterialTheme.typography.bodySmall.copy(textDecoration = if (isStepDone) TextDecoration.LineThrough else TextDecoration.None), color = if(isStepDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
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

            // 4. HYDRATION SETTINGS CARD (NOW WITH EXACT 3-DECIMAL LITER DISPLAY)
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Hydration Quick-Add", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            val waterLiters = (todayRecord?.waterIntakeMl ?: 0) / 1000f
                            val formattedLiters = String.format(Locale.US, "%.3f", waterLiters).trimEnd('0').trimEnd('.')
                            Text("$formattedLiters L today", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val options = listOf(250 to "Glass", 600 to "Shaker", 1500 to "Bottle")
                            options.forEach { (ml, label) ->
                                val isSelected = defaultWaterIncrement == ml
                                Surface(
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).clickable { viewModel.saveDefaultWaterIncrement(ml) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${ml}ml", fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha=0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. BIO ENGINE CARD
            item {
                var isEditingBio by remember { mutableStateOf(false) }

                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Bio Engine & Macros", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(onClick = { isEditingBio = !isEditingBio }) { Icon(if (isEditingBio) Icons.Default.Close else Icons.Default.Edit, contentDescription = "Edit Bio") }
                        }

                        if (isEditingBio) {
                            var editWeight by remember { mutableStateOf(weight.toString()) }
                            var editHeight by remember { mutableStateOf(height.toString()) }
                            var editAge by remember { mutableStateOf(age.toString()) }
                            var editGender by remember { mutableStateOf(gender) }
                            var editActivity by remember { mutableStateOf(activity) }
                            var editGoal by remember { mutableStateOf(goal) }

                            var genderExpanded by remember { mutableStateOf(false) }
                            var activityExpanded by remember { mutableStateOf(false) }
                            var goalExpanded by remember { mutableStateOf(false) }

                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = editWeight, onValueChange = { editWeight = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = editHeight, onValueChange = { editHeight = it }, label = { Text("Height (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = editAge, onValueChange = { editAge = it }, label = { Text("Age") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = it }) {
                                    OutlinedTextField(value = editGender, onValueChange = {}, readOnly = true, label = { Text("Gender") }, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) })
                                    ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                                        listOf("Male", "Female").forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { editGender = opt; genderExpanded = false }) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                ExposedDropdownMenuBox(expanded = activityExpanded, onExpandedChange = { activityExpanded = it }) {
                                    OutlinedTextField(value = editActivity, onValueChange = {}, readOnly = true, label = { Text("Activity Level") }, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) })
                                    ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }) {
                                        listOf("Sedentary", "Light", "Moderate", "Active", "Very Active").forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { editActivity = opt; activityExpanded = false }) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                ExposedDropdownMenuBox(expanded = goalExpanded, onExpandedChange = { goalExpanded = it }) {
                                    OutlinedTextField(value = editGoal, onValueChange = {}, readOnly = true, label = { Text("Fitness Goal") }, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalExpanded) })
                                    ExposedDropdownMenu(expanded = goalExpanded, onDismissRequest = { goalExpanded = false }) {
                                        listOf("Lose Weight", "Maintain", "Build Muscle").forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { editGoal = opt; goalExpanded = false }) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = {
                                    viewModel.saveBioMetrics(editWeight.toFloatOrNull() ?: 70f, editHeight.toIntOrNull() ?: 175, editAge.toIntOrNull() ?: 21, editGender, editActivity, editGoal)
                                    isEditingBio = false
                                }, modifier = Modifier.fillMaxWidth()) { Text("Calculate & Save Targets") }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("$weight kg | $height cm | $age yrs", style = MaterialTheme.typography.bodyLarge)
                                Text(goal, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}