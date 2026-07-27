package com.example.loom.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.local.entity.FoodItem
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.data.local.entity.SubTask
import com.example.loom.data.local.entity.Task
import com.example.loom.data.local.entity.TaskSection
import com.example.loom.util.DateUtils
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

// --- CUSTOM GAPLESS PROGRESS BAR ---
@Composable
fun GaplessProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = color.copy(alpha = 0.2f) // Matches track to main color, but faded
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(color)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit
) {
    val todayRecord by viewModel.todayRecord.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val sections by viewModel.allTaskSections.collectAsState()
    val tags by viewModel.allNoteTags.collectAsState()

    val pinnedRoutines by viewModel.pinnedSkincareRoutines.collectAsState()
    val completedRoutines = try { Json.decodeFromString<List<String>>(todayRecord?.completedSkincareRoutinesJson ?: "[]") } catch (e: Exception) { emptyList() }
    val completedSteps = try { Json.decodeFromString<List<String>>(todayRecord?.completedSkincareStepsJson ?: "[]") } catch (e: Exception) { emptyList() }

    val allFoodItems by viewModel.allFoodItems.collectAsState()

    // Targets
    val targetCaloriesInt by viewModel.targetCalories.collectAsState()
    val targetProteinInt by viewModel.targetProtein.collectAsState()
    val targetCarbsInt by viewModel.targetCarbs.collectAsState()
    val targetFatsInt by viewModel.targetFats.collectAsState()
    val defaultWaterIncrement by viewModel.defaultWaterIncrement.collectAsState()

    var showNutritionDialog by remember { mutableStateOf(false) }
    var isManualEntry by remember { mutableStateOf(false) }
    var showNewFoodDialog by remember { mutableStateOf(false) }

    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var isKanbanView by remember { mutableStateOf(false) }
    var sectionToDelete by remember { mutableStateOf<TaskSection?>(null) }

    if (showNutritionDialog) {
        var foodDropdownExpanded by remember { mutableStateOf(false) }
        var foodSearchText by remember { mutableStateOf("") }
        var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
        var servingsStr by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { showNutritionDialog = false },
            title = { Text("Log Meal") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isManualEntry) "Manual Entry" else "From Pantry", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { isManualEntry = !isManualEntry }) { Text(if (isManualEntry) "Use Pantry" else "Manual Entry") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isManualEntry) {
                        var inputCalories by remember { mutableStateOf("") }
                        var inputProtein by remember { mutableStateOf("") }
                        var inputCarbs by remember { mutableStateOf("") }
                        var inputFats by remember { mutableStateOf("") }

                        OutlinedTextField(value = inputCalories, onValueChange = { inputCalories = it }, label = { Text("Calories") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = inputProtein, onValueChange = { inputProtein = it }, label = { Text("Protein (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = inputCarbs, onValueChange = { inputCarbs = it }, label = { Text("Carbs (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = inputFats, onValueChange = { inputFats = it }, label = { Text("Fats (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            val c = inputCalories.toIntOrNull() ?: 0
                            val p = inputProtein.toIntOrNull() ?: 0
                            val cb = inputCarbs.toIntOrNull() ?: 0
                            val f = inputFats.toIntOrNull() ?: 0
                            viewModel.editNutrition( (todayRecord?.calories ?: 0) + c, (todayRecord?.protein ?: 0) + p, (todayRecord?.carbs ?: 0) + cb, (todayRecord?.fats ?: 0) + f )
                            showNutritionDialog = false
                        }, modifier = Modifier.fillMaxWidth()) { Text("Add Macros") }
                    } else {
                        ExposedDropdownMenuBox(expanded = foodDropdownExpanded, onExpandedChange = { foodDropdownExpanded = it }) {
                            OutlinedTextField(
                                value = selectedFood?.name ?: foodSearchText,
                                onValueChange = { foodSearchText = it; foodDropdownExpanded = true; selectedFood = null },
                                placeholder = { Text("Search pantry...") },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = foodDropdownExpanded) }
                            )
                            val filtered = allFoodItems.filter { it.name.contains(foodSearchText, true) }
                            if (filtered.isNotEmpty() && foodDropdownExpanded) {
                                ExposedDropdownMenu(expanded = foodDropdownExpanded, onDismissRequest = { foodDropdownExpanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                                    filtered.forEach { food ->
                                        DropdownMenuItem(
                                            text = { Text("${food.emoji} ${food.name}") },
                                            onClick = { selectedFood = food; foodDropdownExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedFood != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("1 ${selectedFood!!.servingUnit} = ${selectedFood!!.calories} kcal", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = servingsStr, onValueChange = { servingsStr = it },
                                label = { Text("Servings") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                viewModel.logFoodItem(selectedFood!!, servingsStr.toFloatOrNull() ?: 1f)
                                showNutritionDialog = false
                            }, modifier = Modifier.fillMaxWidth()) { Text("Log Food") }
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { showNewFoodDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Add New Food to Pantry") }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showNutritionDialog = false }) { Text("Cancel") } }
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
                        OutlinedTextField(value = inputServing, onValueChange = { inputServing = it }, label = { Text("Serving Unit") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = inputCals, onValueChange = { inputCals = it }, label = { Text("Kcal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = inputPro, onValueChange = { inputPro = it }, label = { Text("Pro (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = inputCarbs, onValueChange = { inputCarbs = it }, label = { Text("Carb (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = inputFats, onValueChange = { inputFats = it }, label = { Text("Fat (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (inputName.isNotBlank() && inputCals.isNotBlank()) {
                        viewModel.addFoodItemToPantry(FoodItem(emoji = inputEmoji, name = inputName, servingUnit = inputServing, calories = inputCals.toIntOrNull() ?: 0, protein = inputPro.toIntOrNull() ?: 0, carbs = inputCarbs.toIntOrNull() ?: 0, fats = inputFats.toIntOrNull() ?: 0))
                        showNewFoodDialog = false
                    }
                }) { Text("Save Food") }
            },
            dismissButton = { TextButton(onClick = { showNewFoodDialog = false }) { Text("Cancel") } }
        )
    }

    if (sectionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sectionToDelete = null },
            title = { Text("Delete Section?") },
            text = { Text("Are you sure you want to delete '${sectionToDelete?.name}'? Tasks in this section will be orphaned.") },
            confirmButton = { Button(onClick = { viewModel.deleteTaskSection(sectionToDelete!!); sectionToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { sectionToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showTaskDialog) {
        CreateTaskDialog(
            sections = sections, tags = tags, initialTask = taskToEdit,
            onDismiss = { showTaskDialog = false; taskToEdit = null },
            onSave = { newTask ->
                if (taskToEdit != null) viewModel.updateTask(newTask) else viewModel.addTask(newTask)
                showTaskDialog = false; taskToEdit = null
            },
            onDelete = { taskToDelete ->
                viewModel.deleteTask(taskToDelete)
                showTaskDialog = false; taskToEdit = null
            },
            onAddSection = { name -> viewModel.addTaskSection(name) },
            onDeleteSection = { section -> viewModel.deleteTaskSection(section) },
            onAddTag = { name, color -> viewModel.addNoteTag(name, color) },
            onDeleteTag = { tag -> viewModel.deleteNoteTag(tag) }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Your Daily Loom", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    // --- FORCES THE LIGHT GREEN TINT ---
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { taskToEdit = null; showTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Default.Add, "Add Task") }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- FULL NUTRITION CARD (UPDATED WITH GAPLESS BARS) ---
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Fuel", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { showNutritionDialog = true }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) { Text("+ Log Meal") }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        val currentCals = todayRecord?.calories ?: 0
                        val calProg = if (targetCaloriesInt > 0) (currentCals.toFloat() / targetCaloriesInt).coerceIn(0f, 1f) else 0f
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Calories", style = MaterialTheme.typography.bodyMedium)
                            Text("$currentCals / $targetCaloriesInt kcal", fontWeight = FontWeight.Bold)
                        }

                        GaplessProgressBar(
                            progress = calProg,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val currentPro = todayRecord?.protein ?: 0
                            val proProg = if (targetProteinInt > 0) (currentPro.toFloat() / targetProteinInt).coerceIn(0f, 1f) else 0f
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Protein", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${currentPro}g / ${targetProteinInt}g", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                GaplessProgressBar(
                                    progress = proProg,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp)
                                )
                            }
                            val currentCarbs = todayRecord?.carbs ?: 0
                            val carbProg = if (targetCarbsInt > 0) (currentCarbs.toFloat() / targetCarbsInt).coerceIn(0f, 1f) else 0f
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Carbs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${currentCarbs}g / ${targetCarbsInt}g", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                GaplessProgressBar(
                                    progress = carbProg,
                                    color = Color(0xFFFFA726),
                                    modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp)
                                )
                            }
                            val currentFats = todayRecord?.fats ?: 0
                            val fatProg = if (targetFatsInt > 0) (currentFats.toFloat() / targetFatsInt).coerceIn(0f, 1f) else 0f
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Fats", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${currentFats}g / ${targetFatsInt}g", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                GaplessProgressBar(
                                    progress = fatProg,
                                    color = Color(0xFFE53935),
                                    modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- HYDRATION CARD ---
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hydration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val waterLiters = (todayRecord?.waterIntakeMl ?: 0) / 1000f
                            val formattedLiters = String.format(Locale.US, "%.3f", waterLiters).trimEnd('0').trimEnd('.')
                            Text("$formattedLiters L", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.removeWater() }) { Icon(Icons.Default.Remove, "Remove Water") }
                            Button(onClick = { viewModel.addWater() }, shape = RoundedCornerShape(12.dp)) {
                                Text("+ ${defaultWaterIncrement}ml", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- SKINCARE ROUTINE ---
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Skincare Routine", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (pinnedRoutines.isEmpty()) {
                            Text("No routines pinned yet. Create them in the Health tab!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            pinnedRoutines.forEach { routine ->
                                val isDone = completedRoutines.contains(routine.name)
                                val products = try { Json.decodeFromString<List<String>>(routine.productsJson) } catch (e: Exception) { emptyList() }

                                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Checkbox(
                                        checked = isDone,
                                        onCheckedChange = { viewModel.toggleSkincareRoutine(routine.name) }
                                    )
                                    Column(modifier = Modifier.padding(top = 12.dp)) {
                                        Text(routine.name, fontWeight = FontWeight.Bold, textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None)

                                        if (products.isNotEmpty()) {
                                            Column(modifier = Modifier.padding(top = 4.dp, start = 4.dp)) {
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
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tasks & Projects", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { isKanbanView = !isKanbanView }) {
                        Icon(if (isKanbanView) Icons.Default.ViewKanban else Icons.Default.ViewList, "Toggle View")
                    }
                }
            }

            items(sections) { section ->
                val sectionTasks = allTasks.filter { it.sectionId == section.id }
                if (sectionTasks.isNotEmpty() || !isKanbanView) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF7F7F7))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { sectionToDelete = section }
                                    )
                            ) {
                                Text(text = section.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }

                            if (isKanbanView) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val statuses = listOf("TO DO", "IN PROGRESS", "DONE")
                                    statuses.forEach { status ->
                                        val colTasks = sectionTasks.filter { it.status == status }

                                        Column(
                                            modifier = Modifier
                                                .width(280.dp)
                                                .fillMaxHeight()
                                                .defaultMinSize(minHeight = 150.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                                .padding(12.dp)
                                        ) {
                                            Text(status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                                            colTasks.forEach { task ->
                                                TaskCard(task, tags, viewModel, isKanbanView = true, onEdit = { taskToEdit = it; showTaskDialog = true })
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (sectionTasks.isEmpty()) {
                                    Text("No tasks here yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                                } else {
                                    sectionTasks.forEach { task ->
                                        TaskCard(task, tags, viewModel, isKanbanView = false, onEdit = { taskToEdit = it; showTaskDialog = true })
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

// ... KEEP getRemainingDaysText, TaskCard, CreateTaskDialog EXACTLY THE SAME FROM PREVIOUS CODE ...
fun getRemainingDaysText(targetDate: String): String? {
    if (targetDate.isBlank()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val target = sdf.parse(targetDate) ?: return null
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(task: Task, allTags: List<NoteTag>, viewModel: HomeViewModel, isKanbanView: Boolean, onEdit: (Task) -> Unit) {
    val priorityColor = when (task.priority) { 1 -> Color(0xFF4CAF50); 2 -> Color(0xFF8BC34A); 3 -> Color(0xFFFFEB3B); 4 -> Color(0xFFFF9800); 5 -> Color(0xFFF44336); else -> Color.Gray }
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = { viewModel.toggleTaskCompletion(task) },
                onLongClick = { onEdit(task) }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(50)).background(priorityColor))
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None))
                    if (task.description.isNotBlank()) Text(task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                IconButton(onClick = { viewModel.toggleTaskCompletion(task) }, modifier = Modifier.size(32.dp)) {
                    Icon(if (task.isCompleted) Icons.Outlined.CheckCircle else Icons.Default.RadioButtonUnchecked, "Complete", tint = if (task.isCompleted) priorityColor else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            val subtasks = try { Json.decodeFromString<List<SubTask>>(task.subtasksJson) } catch (e: Exception) { emptyList() }
            if (subtasks.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 4.dp)) {
                    subtasks.forEach { subtask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleSubtaskCompletion(task, subtask.id) }
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (subtask.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = "Toggle Subtask",
                                modifier = Modifier.size(16.dp),
                                tint = if (subtask.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = subtask.title,
                                style = MaterialTheme.typography.bodySmall.copy(textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None),
                                color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {

                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    val taskTags = try { Json.decodeFromString<List<String>>(task.tagsJson) } catch (e: Exception) { emptyList() }
                    if (taskTags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState()).weight(1f, fill = false)
                        ) {
                            taskTags.forEach { tagName ->
                                val tagColor = allTags.find { it.name == tagName }?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary
                                Surface(color = tagColor.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) { Text(tagName, style = MaterialTheme.typography.labelSmall, color = tagColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
                            }
                        }
                    }

                    val remaining = getRemainingDaysText(task.targetDate)
                    if (remaining != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val isOverdue = remaining.contains("overdue")
                        Text(
                            text = remaining,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                    val statuses = listOf("TO DO", "IN PROGRESS", "DONE")
                    val currentIndex = statuses.indexOf(task.status)

                    if (isKanbanView && currentIndex > 0) {
                        IconButton(onClick = {
                            val prevStatus = statuses[currentIndex - 1]
                            viewModel.updateTaskStatus(task, prevStatus)
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ChevronLeft, "Move Left", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Surface(
                        modifier = Modifier.clickable {
                            val nextStatus = when (task.status) { "TO DO" -> "IN PROGRESS"; "IN PROGRESS" -> "DONE"; else -> "TO DO" }
                            viewModel.updateTaskStatus(task, nextStatus)
                        },
                        color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)
                    ) { Text(task.status, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }

                    if (isKanbanView && currentIndex < statuses.size - 1) {
                        IconButton(onClick = {
                            val nextStatus = statuses[currentIndex + 1]
                            viewModel.updateTaskStatus(task, nextStatus)
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ChevronRight, "Move Right", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Move to TO DO") }, onClick = { viewModel.updateTaskStatus(task, "TO DO"); showMenu = false })
            DropdownMenuItem(text = { Text("Move to IN PROGRESS") }, onClick = { viewModel.updateTaskStatus(task, "IN PROGRESS"); showMenu = false })
            DropdownMenuItem(text = { Text("Move to DONE") }, onClick = { viewModel.updateTaskStatus(task, "DONE"); showMenu = false })
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Edit Task") }, onClick = { onEdit(task); showMenu = false }, leadingIcon = { Icon(Icons.Default.Edit, null) })
            DropdownMenuItem(text = { Text("Delete Task", color = MaterialTheme.colorScheme.error) }, onClick = { viewModel.deleteTask(task); showMenu = false }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CreateTaskDialog(
    sections: List<TaskSection>, tags: List<NoteTag>, initialTask: Task?,
    onDismiss: () -> Unit, onSave: (Task) -> Unit, onDelete: (Task) -> Unit,
    onAddSection: (String) -> Unit, onDeleteSection: (TaskSection) -> Unit,
    onAddTag: (String, String) -> Unit, onDeleteTag: (NoteTag) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var priority by remember { mutableIntStateOf(initialTask?.priority ?: 1) }
    var selectedSectionId by remember { mutableIntStateOf(initialTask?.sectionId ?: sections.firstOrNull()?.id ?: 1) }
    var status by remember { mutableStateOf(initialTask?.status ?: "TO DO") }

    val initialTags = try { Json.decodeFromString<List<String>>(initialTask?.tagsJson ?: "[]") } catch (e: Exception) { emptyList() }
    var selectedTags by remember { mutableStateOf(initialTags.toSet()) }

    val initialSubtasks = try { Json.decodeFromString<List<SubTask>>(initialTask?.subtasksJson ?: "[]") } catch (e: Exception) { emptyList() }
    var subtasks by remember { mutableStateOf(initialSubtasks) }
    var newSubtaskText by remember { mutableStateOf("") }

    var isRecurring by remember { mutableStateOf(initialTask?.isRecurring ?: false) }
    val initialRecInterval = initialTask?.recurringInterval?.split(" ")?.getOrNull(1) ?: "DAILY"
    val initialRecCount = initialTask?.recurringInterval?.split(" ")?.getOrNull(0) ?: "1"

    var recurringType by remember { mutableStateOf(initialRecInterval) }
    var recurringCount by remember { mutableStateOf(initialRecCount) }

    var selectedDays by remember { mutableStateOf(setOf<Int>()) }

    var deadlineStr by remember { mutableStateOf(initialTask?.targetDate ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    var showNewSectionDialog by remember { mutableStateOf(false) }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<NoteTag?>(null) }
    var dialogSectionToDelete by remember { mutableStateOf<TaskSection?>(null) }

    if (showNewSectionDialog) {
        var newSecName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewSectionDialog = false }, title = { Text("New Section") },
            text = { OutlinedTextField(value = newSecName, onValueChange = { newSecName = it }, singleLine = true) },
            confirmButton = { Button(onClick = { if (newSecName.isNotBlank()) onAddSection(newSecName); showNewSectionDialog = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showNewSectionDialog = false }) { Text("Cancel") } }
        )
    }

    if (dialogSectionToDelete != null) {
        AlertDialog(
            onDismissRequest = { dialogSectionToDelete = null }, title = { Text("Delete Section?") },
            text = { Text("Are you sure you want to delete '${dialogSectionToDelete?.name}'?") },
            confirmButton = { Button(onClick = {
                onDeleteSection(dialogSectionToDelete!!)
                if (selectedSectionId == dialogSectionToDelete!!.id) {
                    selectedSectionId = sections.firstOrNull { it.id != selectedSectionId }?.id ?: 1
                }
                dialogSectionToDelete = null
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { dialogSectionToDelete = null }) { Text("Cancel") } }
        )
    }

    if (tagToDelete != null) {
        AlertDialog(
            onDismissRequest = { tagToDelete = null }, title = { Text("Delete Tag?") },
            text = { Text("Are you sure you want to delete '${tagToDelete?.name}'?") },
            confirmButton = { Button(onClick = { onDeleteTag(tagToDelete!!); selectedTags = selectedTags - tagToDelete!!.name; tagToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { tagToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showNewTagDialog) {
        var newTagName by remember { mutableStateOf("") }
        var hue by remember { mutableFloatStateOf(210f) }
        var saturation by remember { mutableFloatStateOf(0.9f) }
        var value by remember { mutableFloatStateOf(0.9f) }

        val dynamicColor = Color.hsv(hue, saturation, value)
        val hexString = String.format("#%02X%02X%02X", (dynamicColor.red * 255).toInt(), (dynamicColor.green * 255).toInt(), (dynamicColor.blue * 255).toInt())

        AlertDialog(
            onDismissRequest = { showNewTagDialog = false }, title = { Text("Create Custom Tag") },
            text = {
                Column {
                    OutlinedTextField(value = newTagName, onValueChange = { newTagName = it }, label = { Text("Tag Name") }, singleLine = true)
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.hsv(hue, 1f, 1f))
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                saturation = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                value = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                            }
                        }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(brush = Brush.horizontalGradient(colors = listOf(Color.White, Color.Transparent)))
                            drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black)))
                            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(saturation * size.width, (1f - value) * size.height), style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Canvas(modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(50))
                        .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); hue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f } }
                    ) {
                        drawRect(brush = Brush.horizontalGradient(colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                        drawCircle(color = Color.White, radius = 12.dp.toPx(), center = Offset((hue / 360f) * size.width, size.height / 2), style = Stroke(width = 3.dp.toPx()))
                    }
                }
            },
            confirmButton = { Button(onClick = { if (newTagName.isNotBlank()) onAddTag(newTagName, hexString); showNewTagDialog = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showNewTagDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDatePicker) {
        val currentUtcDateMillis = remember {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= currentUtcDateMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        deadlineStr = sdf.format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { deadlineStr = ""; showDatePicker = false }) { Text("Clear Date") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val greenChipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTask == null) "New Task" else "Edit Task") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Details (Optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 4) }

                item {
                    Text("Subtasks:", style = MaterialTheme.typography.labelMedium)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        subtasks.forEachIndexed { index, subtask ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(if (subtask.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(subtask.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { subtasks = subtasks.filterIndexed { i, _ -> i != index } }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, "Remove Subtask", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newSubtaskText,
                                onValueChange = { newSubtaskText = it },
                                placeholder = { Text("New subtask...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                if (newSubtaskText.isNotBlank()) {
                                    subtasks = subtasks + SubTask(id = UUID.randomUUID().toString(), title = newSubtaskText)
                                    newSubtaskText = ""
                                }
                            }) {
                                Icon(Icons.Default.Add, "Add Subtask", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                item {
                    Text("Deadline:", style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp))
                        Text(if (deadlineStr.isBlank()) "No Deadline" else deadlineStr)
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Section:", style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = { showNewSectionDialog = true }) { Text("+ Add") }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sections) { sec ->
                            Surface(
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = { selectedSectionId = sec.id },
                                        onLongClick = { dialogSectionToDelete = sec }
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (selectedSectionId == sec.id) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                color = if (selectedSectionId == sec.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ) {
                                Text(
                                    text = sec.name,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selectedSectionId == sec.id) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Priority (1-5):", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        (1..5).forEach { lvl ->
                            val pColor = when(lvl) { 1->Color(0xFF4CAF50); 2->Color(0xFF8BC34A); 3->Color(0xFFFFEB3B); 4->Color(0xFFFF9800); 5->Color(0xFFF44336); else->Color.Gray }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(pColor.copy(alpha = if (priority == lvl) 1f else 0.3f))
                                    .then(if (priority == lvl) Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier)
                                    .clickable { priority = lvl },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$lvl", color = if (priority == lvl) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Make Recurring")
                    }
                    if (isRecurring) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Repeat every ", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = recurringCount,
                                onValueChange = { recurringCount = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(60.dp).height(48.dp),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            listOf("DAYS", "WEEKS", "MONTHS").forEach { interval ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { recurringType = interval },
                                    color = if (recurringType == interval) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                ) {
                                    Text(
                                        text = interval,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (recurringType == interval) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (recurringType == "WEEKS") {
                            val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                daysOfWeek.forEachIndexed { index, day ->
                                    val dayId = index + 1
                                    val isSelected = selectedDays.contains(dayId)
                                    Box(
                                        modifier = Modifier.size(32.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable {
                                            selectedDays = if (isSelected) selectedDays - dayId else selectedDays + dayId
                                        },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(day, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Status:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("TO DO", "IN PROGRESS", "DONE").forEach { st ->
                            FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st) }, colors = greenChipColors)
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Tags:", style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = { showNewTagDialog = true }) { Text("+ Add") }
                    }
                    if (tags.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tags) { tag ->
                                val tagColor = try { Color(android.graphics.Color.parseColor(tag.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                                Surface(
                                    modifier = Modifier.combinedClickable(
                                        onClick = { selectedTags = if (selectedTags.contains(tag.name)) selectedTags - tag.name else selectedTags + tag.name },
                                        onLongClick = { tagToDelete = tag }
                                    ).clip(RoundedCornerShape(8.dp)).border(if (selectedTags.contains(tag.name)) 2.dp else 0.dp, tagColor, RoundedCornerShape(8.dp)),
                                    color = tagColor.copy(alpha = 0.3f),
                                ) {
                                    Text(tag.name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                if (initialTask != null) {
                    TextButton(onClick = { onDelete(initialTask) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Button(onClick = {
                    if (title.isNotBlank()) {
                        val finalIntervalString = if (isRecurring) "$recurringCount $recurringType" else "NONE"
                        val completionTime = if (status == "DONE") System.currentTimeMillis() else 0L

                        val task = Task(
                            id = initialTask?.id ?: 0, title = title, description = description, priority = priority,
                            sectionId = selectedSectionId, status = status, isCompleted = status == "DONE",
                            targetDate = deadlineStr,
                            tagsJson = Json.encodeToString(selectedTags.toList()),
                            subtasksJson = Json.encodeToString(subtasks),
                            isRecurring = isRecurring, recurringInterval = finalIntervalString,
                            completionDateMillis = completionTime
                        )
                        onSave(task)
                    }
                }) { Text("Save") }
            }
        }
    )
}