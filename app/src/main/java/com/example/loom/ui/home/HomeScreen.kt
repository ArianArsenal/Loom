package com.example.loom.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
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
import androidx.compose.ui.unit.dp
import com.example.loom.data.local.entity.FoodItem
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.data.local.entity.SubTask
import com.example.loom.data.local.entity.Task
import com.example.loom.data.local.entity.TaskSection
import com.example.loom.ui.home.components.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToSpace: (String) -> Unit
) {

    val pinnedCards by viewModel.pinnedHomeCards.collectAsState()
    val allSpaces by viewModel.allSpaces.collectAsState()

    val todayRecord by viewModel.todayRecord.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val sections by viewModel.allTaskSections.collectAsState()
    val tags by viewModel.allNoteTags.collectAsState()

    val pinnedRoutines by viewModel.pinnedSkincareRoutines.collectAsState()
    val completedRoutines by viewModel.completedSkincareRoutines.collectAsState()
    val completedSteps by viewModel.completedSkincareSteps.collectAsState()
    val consumedList by viewModel.consumedFoods.collectAsState()

    val targetCaloriesInt by viewModel.targetCalories.collectAsState()
    val targetProteinInt by viewModel.targetProtein.collectAsState()
    val targetCarbsInt by viewModel.targetCarbs.collectAsState()
    val targetFatsInt by viewModel.targetFats.collectAsState()
    val defaultWaterIncrement by viewModel.defaultWaterIncrement.collectAsState()
    val dailyGoal by viewModel.waterDailyGoal.collectAsState() // NEW
    var showNutritionDialog by remember { mutableStateOf(false) }
    var isManualEntry by remember { mutableStateOf(false) }
    var showNewFoodDialog by remember { mutableStateOf(false) }

    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var isKanbanView by remember { mutableStateOf(false) }
    var sectionToDelete by remember { mutableStateOf<TaskSection?>(null) }

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "Good Morning, Arian"
            in 12..17 -> "Good Afternoon, Arian"
            in 18..23 -> "Good Evening, Arian"
            else -> "Good Night, Arian"
        }
    }

    if (showNutritionDialog) {
        var foodDropdownExpanded by remember { mutableStateOf(false) }
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
                            viewModel.logManualMeal(inputCalories.toIntOrNull() ?: 0, inputProtein.toIntOrNull() ?: 0, inputCarbs.toIntOrNull() ?: 0, inputFats.toIntOrNull() ?: 0)
                            showNutritionDialog = false
                        }, modifier = Modifier.fillMaxWidth()) { Text("Add Macros") }
                    } else {
                        val currentSearchQuery by viewModel.foodSearchQuery.collectAsState()
                        val searchedFoods by viewModel.searchedFoodItems.collectAsState()

                        if (currentSearchQuery.isBlank() && selectedFood == null && searchedFoods.isNotEmpty()) {
                            Text("Quick Picks", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(searchedFoods.take(8)) { food ->
                                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.clickable { selectedFood = food }) {
                                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(food.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Text("${food.calories} kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        ExposedDropdownMenuBox(expanded = foodDropdownExpanded, onExpandedChange = { foodDropdownExpanded = it }) {
                            OutlinedTextField(
                                value = selectedFood?.name ?: currentSearchQuery,
                                onValueChange = { viewModel.updateFoodSearchQuery(it); foodDropdownExpanded = true; selectedFood = null },
                                placeholder = { Text("Search Persian & global foods...") },
                                modifier = Modifier.menuAnchor().fillMaxWidth(), singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = foodDropdownExpanded) }
                            )

                            if (searchedFoods.isNotEmpty() && foodDropdownExpanded) {
                                ExposedDropdownMenu(expanded = foodDropdownExpanded, onDismissRequest = { foodDropdownExpanded = false }, modifier = Modifier.heightIn(max = 250.dp)) {
                                    searchedFoods.forEachIndexed { index, food ->
                                        DropdownMenuItem(
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                            text = {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Text(text = food.name, fontWeight = FontWeight.Bold)
                                                    Text(text = "P: ${food.protein}g • C: ${food.carbs}g • F: ${food.fats}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    // --- RTL Format Fix ---
                                                    val uText = food.servingUnit.trim()
                                                    val mResult = "\\d+(\\.\\d+)?".toRegex().find(uText)
                                                    val numStr = mResult?.value ?: ""
                                                    val textStr = if (mResult != null) uText.replaceFirst(numStr, "").trim() else uText

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Left side: Calories (Plain Text)
                                                        Text(
                                                            text = "${food.calories} kcal",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )

                                                        // Right side: Serving Unit (Colored Badge with RTL Row)
                                                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                if (textStr.isNotBlank()) {
                                                                    Text(text = textStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                }
                                                                if (numStr.isNotBlank()) {
                                                                    Text(text = numStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            onClick = { selectedFood = food; foodDropdownExpanded = false; viewModel.updateFoodSearchQuery("") }
                                        )
                                        if (index < searchedFoods.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                                    }
                                }
                            }
                        }

                        if (selectedFood != null) {
                            Spacer(modifier = Modifier.height(16.dp))

                            val currentMultiplier = servingsStr.toFloatOrNull() ?: 1f
                            val dynamicCals = selectedFood!!.calories * currentMultiplier
                            val unitText = selectedFood!!.servingUnit.trim()
                            val numberRegex = "\\d+(\\.\\d+)?".toRegex()
                            val matchResult = numberRegex.find(unitText)
                            val textPart: String
                            val formattedNum: String

                            if (matchResult != null) {
                                val baseNum = matchResult.value.toFloat()
                                val calculatedNum = baseNum * currentMultiplier
                                formattedNum = if (calculatedNum % 1f == 0f) calculatedNum.toInt().toString() else String.format(Locale.US, "%.1f", calculatedNum)
                                textPart = unitText.replaceFirst(matchResult.value, "").trim()
                            } else {
                                formattedNum = if (currentMultiplier % 1f == 0f) currentMultiplier.toInt().toString() else String.format(Locale.US, "%.1f", currentMultiplier)
                                textPart = unitText
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (textPart.isNotBlank()) {
                                    Text(text = textPart, style = MaterialTheme.typography.bodyLarge)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(text = formattedNum, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "=", style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.width(12.dp))

                                val calsStr = if (dynamicCals % 1f == 0f) dynamicCals.toInt().toString() else String.format(Locale.US, "%.1f", dynamicCals)
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text(
                                        text = "$calsStr kcal",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(value = servingsStr, onValueChange = { servingsStr = it }, label = { Text("Multiplier (e.g., 2 or 0.5)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.logFoodItem(selectedFood!!, servingsStr.toFloatOrNull() ?: 1f); showNutritionDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Log Food") }
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { showNewFoodDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Add New Food to Pantry") }
                        }
                    }
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = { showNutritionDialog = false }) { Text("Cancel") } }
        )
    }

    if (showNewFoodDialog) {
        var inputName by remember { mutableStateOf("") }
        var inputServing by remember { mutableStateOf("Plate") }
        var inputCals by remember { mutableStateOf("") }
        var inputPro by remember { mutableStateOf("") }
        var inputCarbs by remember { mutableStateOf("") }
        var inputFats by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewFoodDialog = false }, title = { Text("Add to Pantry") },
            text = {
                LazyColumn {
                    item {
                        OutlinedTextField(value = inputName, onValueChange = { inputName = it }, label = { Text("Food Name") }, modifier = Modifier.fillMaxWidth())
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
                        viewModel.addFoodItemToPantry(FoodItem(name = inputName, servingUnit = inputServing, calories = inputCals.toFloatOrNull() ?: 0f, protein = inputPro.toFloatOrNull() ?: 0f, carbs = inputCarbs.toFloatOrNull() ?: 0f, fats = inputFats.toFloatOrNull() ?: 0f))
                        showNewFoodDialog = false
                    }
                }) { Text("Save Food") }
            },
            dismissButton = { TextButton(onClick = { showNewFoodDialog = false }) { Text("Cancel") } }
        )
    }

    if (sectionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sectionToDelete = null }, title = { Text("Delete Section?") },
            text = { Text("Are you sure you want to delete '${sectionToDelete?.name}'? Tasks in this section will be orphaned.") },
            confirmButton = { Button(onClick = { viewModel.deleteTaskSection(sectionToDelete!!); sectionToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { sectionToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showTaskDialog) {
        CreateTaskDialog(
            sections = sections, tags = tags, initialTask = taskToEdit,
            onDismiss = { showTaskDialog = false; taskToEdit = null },
            onSave = { newTask -> if (taskToEdit != null) viewModel.updateTask(newTask) else viewModel.addTask(newTask); showTaskDialog = false; taskToEdit = null },
            onDelete = { taskToDelete -> viewModel.deleteTask(taskToDelete); showTaskDialog = false; taskToEdit = null },
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
                title = { Text(greeting, fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- NEW: RENDER PINNED CARDS AT THE VERY TOP ---
            if (pinnedCards.isNotEmpty()) {
                items(pinnedCards, key = { it.second.id }) { (space, card) ->
                    PinnedCardWidget(
                        space = space,
                        cardBlock = card,
                        allSpaces = allSpaces,
                        tabName = "Home",
                        onUpdateSpace = { viewModel.updateSpaceFromWidget(it) },
                        onNavigateToSpace = onNavigateToSpace
                    )
                }
            }

            item {
                NutritionLogCard(
                    todayRecord = todayRecord, targetCaloriesInt = targetCaloriesInt, targetProteinInt = targetProteinInt, targetCarbsInt = targetCarbsInt, targetFatsInt = targetFatsInt,
                    consumedList = consumedList, viewModel = viewModel, onLogMealClick = { showNutritionDialog = true }
                )
            }
            item {
                HydrationCard(todayRecord = todayRecord, dailyGoal = dailyGoal, defaultWaterIncrement = defaultWaterIncrement, viewModel = viewModel)
            }
            item {
                SkincareRoutinesCard(pinnedRoutines = pinnedRoutines, completedRoutines = completedRoutines, completedSteps = completedSteps, viewModel = viewModel)
            }
            item {
                TasksAndDeadlinesCard(
                    sections = sections, allTasks = allTasks, tags = tags, isKanbanView = isKanbanView, viewModel = viewModel,
                    onToggleKanban = { isKanbanView = !isKanbanView },
                    onAddTask = { taskToEdit = null; showTaskDialog = true },
                    onEditTask = { taskToEdit = it; showTaskDialog = true },
                    onDeleteSection = { sectionToDelete = it }
                )
            }
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
// --- NEW: STRICT DATABASE-MIRRORING WIDGET ---
@Composable
fun PinnedCardWidget(
    space: com.example.loom.data.models.SandboxSpace,
    cardBlock: com.example.loom.data.models.SandboxBlock,
    allSpaces: List<com.example.loom.data.models.SandboxSpace>,
    tabName: String = "Home",
    onUpdateSpace: (com.example.loom.data.models.SandboxSpace) -> Unit,
    onNavigateToSpace: (String) -> Unit
) {
    val focusRequesters = remember { androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.ui.focus.FocusRequester>() }
    val recentTableColors = remember { androidx.compose.runtime.mutableStateListOf<androidx.compose.ui.graphics.Color>() }
    var focusedBlockId by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    // 1. Initialize the state buffer
    var localCard by remember {
        androidx.compose.runtime.mutableStateOf(
            com.example.loom.ui.sandbox.prepareBlocksForUI(listOf(cardBlock)).first()
        )
    }

    // 2. THE ULTIMATE SYNC: Key explicitly to space.updatedAt!
    // Whenever the database saves, the timestamp changes. This guarantees the LaunchedEffect runs.
    androidx.compose.runtime.LaunchedEffect(space.updatedAt) {
        // Find the fresh block directly from the newly emitted space to ensure we have the latest data
        val freshCardBlock = com.example.loom.ui.sandbox.findPinnedCards(space.blocks, tabName).find { it.id == cardBlock.id }

        if (freshCardBlock != null) {
            if (focusedBlockId == null || localCard.isLocked != freshCardBlock.isLocked) {
                localCard = com.example.loom.ui.sandbox.prepareBlocksForUI(listOf(freshCardBlock)).first()
                if (freshCardBlock.isLocked) {
                    focusedBlockId = null // Forcefully hide the keyboard if locked from another tab!
                }
            }
        }
    }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var debounceJob by remember { androidx.compose.runtime.mutableStateOf<kotlinx.coroutines.Job?>(null) }

    com.example.loom.ui.sandbox.BlockRenderer(
        block = localCard,
        numberIndex = 1,
        allSpaces = allSpaces,
        currentSpaceId = space.id,
        recentTableColors = recentTableColors,
        focusRequesters = focusRequesters,
        focusTargetId = focusedBlockId,
        allBlocks = listOf(localCard),
        isWidgetMode = true,
        isReadOnly = localCard.isLocked, // Use localCard for immediate UI reflection
        onScrollToBlock = {},
        onMove = {},
        onFocusRequest = { focusedBlockId = it },
        onScrollToTop = {},
        onNavigateToSpace = onNavigateToSpace,
        onCreateLinkedSpace = { java.util.UUID.randomUUID().toString() },
        onUpdate = { updatedCard ->
            // Fast UI typing update
            localCard = updatedCard

            // Push to DB
            debounceJob?.cancel()
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            debounceJob = kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                kotlinx.coroutines.delay(500)
                val cleanCard = com.example.loom.ui.sandbox.cleanBlocksForDB(listOf(updatedCard)).first()
                val newBlocks = com.example.loom.ui.sandbox.replaceBlockRecursively(space.blocks, cleanCard)
                // Bump the timestamp here to trigger the LaunchedEffect for all other widgets!
                onUpdateSpace(space.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()))
            }
        },
        onSplitBlock = { _, _, _ -> },
        onDeleteBlock = { _, deleteEntirely ->
            if (deleteEntirely) {
                debounceJob?.cancel()
                val newBlocks = com.example.loom.ui.sandbox.removeBlockRecursively(space.blocks, localCard.id)
                onUpdateSpace(space.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()))
            }
        }
    )
}