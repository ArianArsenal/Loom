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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.loom.data.local.entity.FoodItem
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.data.local.entity.SubTask
import com.example.loom.data.local.entity.Task
import com.example.loom.data.local.entity.TaskSection
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import com.example.loom.ui.home.components.*
import com.example.loom.ui.sandbox.BlockRenderer
import com.example.loom.ui.sandbox.cleanBlocksForDB
import com.example.loom.ui.sandbox.findPinnedCards
import com.example.loom.ui.sandbox.prepareBlocksForUI
import com.example.loom.ui.sandbox.removeBlockRecursively
import com.example.loom.ui.sandbox.replaceBlockRecursively
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

// --- PERFECT PRECISION FIX: Max 3 decimals, strip trailing zeros ---
private fun Float.formatWeight(): String {
    val formatted = String.format(Locale.US, "%.3f", this)
    return formatted.trimEnd('0').trimEnd('.')
}

// --- RTL MASTER FIX: Explicitly splits the number from the text to enforce visual order ---
@Composable
fun ServingUnitDisplay(servingUnit: String, textStyle: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) {
    val mResult = "\\d+(\\.\\d+)?".toRegex().find(servingUnit)
    val numStr = mResult?.value ?: ""
    val textStr = if (mResult != null) servingUnit.replaceFirst(numStr, "").trim() else servingUnit.trim()
    val isPersian = Regex("[\\u0600-\\u06FF]").containsMatchIn(textStr)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPersian) {
            // Visual LTR order for Persian: [Text] then [Number] (Reads RTL correctly)
            if (textStr.isNotBlank()) Text(textStr, style = textStyle.copy(textDirection = TextDirection.Rtl))
            if (numStr.isNotBlank()) Text(numStr, style = textStyle, fontWeight = FontWeight.Bold)
        } else {
            // Visual LTR order for English: [Number] then [Text]
            if (numStr.isNotBlank()) Text(numStr, style = textStyle, fontWeight = FontWeight.Bold)
            if (textStr.isNotBlank()) Text(textStr, style = textStyle.copy(textDirection = TextDirection.Ltr))
        }
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToSpace: (String) -> Unit
) {

    val userName by viewModel.userName.collectAsState()
    var showWelcomeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userName) {
        if (userName.isNotBlank()) {
            showWelcomeDialog = false
        } else {
            delay(150.milliseconds)
            if (userName.isBlank()) {
                showWelcomeDialog = true
            }
        }
    }

    val hiddenHomeCards by viewModel.hiddenHomeCards.collectAsState()
    val agendaText by viewModel.agendaText.collectAsState()

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
    val dailyGoal by viewModel.waterDailyGoal.collectAsState()

    var showNutritionDialog by remember { mutableStateOf(false) }
    var isManualEntry by remember { mutableStateOf(false) }
    var showNewFoodDialog by remember { mutableStateOf(false) }
    var showDashboardConfig by remember { mutableStateOf(false) }

    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var isKanbanView by remember { mutableStateOf(false) }
    var sectionToDelete by remember { mutableStateOf<TaskSection?>(null) }

    val shortcutAction by viewModel.shortcutAction.collectAsState()

    LaunchedEffect(shortcutAction) {
        if (shortcutAction == "com.example.loom.ACTION_QUICK_TASK") {
            showTaskDialog = true
            viewModel.clearShortcutAction()
        }
    }

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(currentHour, userName) {
        val nameDisplay = if (userName.isNotBlank()) ", $userName" else ""
        when (currentHour) {
            in 5..11 -> "Good Morning$nameDisplay"
            in 12..17 -> "Good Afternoon$nameDisplay"
            in 18..23 -> "Good Evening$nameDisplay"
            else -> "Good Night$nameDisplay"
        }
    }

    if (showWelcomeDialog) {
        var nameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { },
            title = { Text("Welcome to Loom 👋", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "What should we call you?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = if (nameInput.isNotBlank()) nameInput.trim() else "User"
                        viewModel.saveUserName(finalName)
                        showWelcomeDialog = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Get Started")
                }
            }
        )
    }

    if (showDashboardConfig) {
        ModalBottomSheet(
            onDismissRequest = { showDashboardConfig = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            LazyColumn(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                item {
                    Text(
                        "Customize Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Select which modules appear on your Home tab.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                val availableCards = listOf("Agenda", "Nutrition", "Hydration", "Skincare", "Tasks")
                items(availableCards) { card ->
                    val isVisible = !hiddenHomeCards.contains(card)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleHomeCardVisibility(card, !isVisible) }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(card, style = MaterialTheme.typography.bodyLarge)
                        Icon(
                            imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (isVisible) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }

                if (pinnedCards.isNotEmpty()) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text(
                            "Pinned Space Cards",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(pinnedCards, key = { "sheet_${it.second.id}" }) { (_, card) ->
                        val cardKey = "pinned_${card.id}"
                        val isVisible = !hiddenHomeCards.contains(cardKey)
                        val cardTitle = card.content.ifBlank { "Untitled Card" }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.toggleHomeCardVisibility(
                                        cardKey,
                                        !isVisible
                                    )
                                }
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Space Card: $cardTitle",
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (isVisible) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNutritionDialog) {
        var foodDropdownExpanded by remember { mutableStateOf(false) }
        var localSearchText by remember { mutableStateOf("") }
        var selectedFoodName by remember { mutableStateOf<String?>(null) }
        var selectedVariant by remember { mutableStateOf<FoodItem?>(null) }
        var servingsStr by remember { mutableStateOf("1") }

        val variants by viewModel.selectedFoodVariants.collectAsState()

        AlertDialog(
            onDismissRequest = { showNutritionDialog = false },
            title = { Text("Log Meal") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isManualEntry) "Manual Entry" else "From Pantry",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = {
                            isManualEntry = !isManualEntry
                        }) { Text(if (isManualEntry) "Use Pantry" else "Manual Entry") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isManualEntry) {
                        var inputCalories by remember { mutableStateOf("") }
                        var inputProtein by remember { mutableStateOf("") }
                        var inputCarbs by remember { mutableStateOf("") }
                        var inputFats by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = inputCalories,
                            onValueChange = { inputCalories = it },
                            label = { Text("Calories") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputProtein,
                            onValueChange = { inputProtein = it },
                            label = { Text("Protein (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputCarbs,
                                onValueChange = { inputCarbs = it },
                                label = { Text("Carbs (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = inputFats,
                                onValueChange = { inputFats = it },
                                label = { Text("Fats (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            viewModel.logManualMeal(
                                inputCalories.toIntOrNull() ?: 0,
                                inputProtein.toIntOrNull() ?: 0,
                                inputCarbs.toIntOrNull() ?: 0,
                                inputFats.toIntOrNull() ?: 0
                            )
                            showNutritionDialog = false
                        }, modifier = Modifier.fillMaxWidth()) { Text("Add Macros") }
                    } else {
                        val searchedFoods by viewModel.searchedFoodItems.collectAsState()

                        ExposedDropdownMenuBox(
                            expanded = foodDropdownExpanded,
                            onExpandedChange = { foodDropdownExpanded = it }) {
                            OutlinedTextField(
                                value = localSearchText,
                                onValueChange = {
                                    localSearchText = it
                                    viewModel.updateFoodSearchQuery(it)
                                    foodDropdownExpanded = true
                                    selectedFoodName = null
                                    selectedVariant = null
                                    viewModel.clearFoodVariants()
                                },
                                placeholder = { Text("Search Persian & global foods...") },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true)
                                    .fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = foodDropdownExpanded) }
                            )

                            if (searchedFoods.isNotEmpty() && foodDropdownExpanded) {
                                ExposedDropdownMenu(
                                    expanded = true,
                                    onDismissRequest = { foodDropdownExpanded = false },
                                    modifier = Modifier.heightIn(max = 280.dp)
                                ) {
                                    searchedFoods.forEachIndexed { index, food ->
                                        DropdownMenuItem(
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                            text = {
                                                val isNamePersian = Regex("[\\u0600-\\u06FF]").containsMatchIn(food.name)
                                                Text(
                                                    text = food.name,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        textDirection = if (isNamePersian) TextDirection.Rtl else TextDirection.Ltr
                                                    ),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            },
                                            onClick = {
                                                localSearchText = food.name
                                                selectedFoodName = food.name
                                                viewModel.fetchFoodVariants(food.name)
                                                foodDropdownExpanded = false
                                            }
                                        )
                                        if (index < searchedFoods.size - 1) HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedFoodName != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Select Serving Size",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(variants) { variant ->
                                    FilterChip(
                                        selected = selectedVariant == variant,
                                        onClick = { selectedVariant = variant },
                                        label = { ServingUnitDisplay(variant.servingUnit, MaterialTheme.typography.labelMedium) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }

                        if (selectedVariant != null) {
                            Spacer(modifier = Modifier.height(16.dp))

                            val currentMultiplier = servingsStr.toFloatOrNull() ?: 1f
                            val dynamicCals = selectedVariant!!.calories * currentMultiplier
                            val dynamicPro = selectedVariant!!.protein * currentMultiplier
                            val dynamicCarbs = selectedVariant!!.carbs * currentMultiplier
                            val dynamicFats = selectedVariant!!.fats * currentMultiplier

                            OutlinedTextField(
                                value = servingsStr,
                                onValueChange = { servingsStr = it },
                                label = { Text("Multiplier (e.g., 2 or 0.5)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = currentMultiplier.formatWeight(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "×", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(6.dp))

                                        // --- RTL FIX: Apply the flawless unit display ---
                                        ServingUnitDisplay(selectedVariant!!.servingUnit, MaterialTheme.typography.titleMedium)

                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = "=", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            text = "${dynamicCals.formatWeight()} kcal",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${dynamicPro.formatWeight()}g", fontWeight = FontWeight.Bold, color = Color(0xFFE57373))
                                            Text("Protein", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${dynamicCarbs.formatWeight()}g", fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
                                            Text("Carbs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${dynamicFats.formatWeight()}g", fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D))
                                            Text("Fats", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                viewModel.logFoodItem(
                                    selectedVariant!!,
                                    servingsStr.toFloatOrNull() ?: 1f
                                ); showNutritionDialog = false
                            }, modifier = Modifier.fillMaxWidth()) { Text("Log Food") }
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(
                                onClick = { showNewFoodDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("+ Add New Food to Pantry") }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showNutritionDialog = false
                }) { Text("Cancel") }
            }
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
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Food Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputServing,
                            onValueChange = { inputServing = it },
                            label = { Text("Serving Unit") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputCals,
                                onValueChange = { inputCals = it },
                                label = { Text("Kcal") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = inputPro,
                                onValueChange = { inputPro = it },
                                label = { Text("Pro (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputCarbs,
                                onValueChange = { inputCarbs = it },
                                label = { Text("Carb (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = inputFats,
                                onValueChange = { inputFats = it },
                                label = { Text("Fat (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (inputName.isNotBlank() && inputCals.isNotBlank()) {
                        viewModel.addFoodItemToPantry(
                            FoodItem(
                                name = inputName,
                                servingUnit = inputServing,
                                calories = inputCals.toFloatOrNull() ?: 0f,
                                protein = inputPro.toFloatOrNull() ?: 0f,
                                carbs = inputCarbs.toFloatOrNull() ?: 0f,
                                fats = inputFats.toFloatOrNull() ?: 0f
                            )
                        )
                        showNewFoodDialog = false
                    }
                }) { Text("Save Food") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewFoodDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    if (sectionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sectionToDelete = null }, title = { Text("Delete Section?") },
            text = { Text("Are you sure you want to delete '${sectionToDelete?.name}'? Tasks in this section will be orphaned.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTaskSection(sectionToDelete!!); sectionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { sectionToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showTaskDialog) {
        CreateTaskDialog(
            sections = sections, tags = tags, initialTask = taskToEdit,
            onDismiss = { showTaskDialog = false; taskToEdit = null },
            onSave = { newTask ->
                if (taskToEdit != null) viewModel.updateTask(newTask) else viewModel.addTask(
                    newTask
                ); showTaskDialog = false; taskToEdit = null
            },
            onDelete = { taskToDelete ->
                viewModel.deleteTask(taskToDelete); showTaskDialog = false; taskToEdit = null
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
                title = { Text(greeting, fontWeight = FontWeight.Bold) },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = { showDashboardConfig = true }) {
                            Icon(
                                Icons.Default.Tune,
                                "Dashboard Config",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Default.Settings,
                                "Settings",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
    ) { paddingValues ->

        val visiblePinnedCards = remember(pinnedCards, hiddenHomeCards) {
            pinnedCards.filter { (_, card) -> !hiddenHomeCards.contains("pinned_${card.id}") }
        }

        val isDashboardEmpty = visiblePinnedCards.isEmpty() &&
                hiddenHomeCards.contains("Agenda") &&
                hiddenHomeCards.contains("Nutrition") &&
                hiddenHomeCards.contains("Hydration") &&
                hiddenHomeCards.contains("Skincare") &&
                hiddenHomeCards.contains("Tasks")

        if (isDashboardEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your dashboard is clear",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap the tune icon above to customize",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                if (!hiddenHomeCards.contains("Agenda")) {
                    item {
                        AgendaCard(
                            agendaText = agendaText,
                            onUpdateText = { viewModel.updateAgendaText(it) })
                    }
                }

                if (visiblePinnedCards.isNotEmpty()) {
                    items(visiblePinnedCards, key = { it.second.id }) { (space, card) ->
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

                if (!hiddenHomeCards.contains("Nutrition")) {
                    item {
                        NutritionLogCard(
                            todayRecord = todayRecord,
                            targetCaloriesInt = targetCaloriesInt,
                            targetProteinInt = targetProteinInt,
                            targetCarbsInt = targetCarbsInt,
                            targetFatsInt = targetFatsInt,
                            consumedList = consumedList,
                            viewModel = viewModel,
                            onLogMealClick = { showNutritionDialog = true }
                        )
                    }
                }

                if (!hiddenHomeCards.contains("Hydration")) {
                    item {
                        HydrationCard(
                            todayRecord = todayRecord,
                            dailyGoal = dailyGoal,
                            defaultWaterIncrement = defaultWaterIncrement,
                            viewModel = viewModel
                        )
                    }
                }

                if (!hiddenHomeCards.contains("Skincare")) {
                    item {
                        SkincareRoutinesCard(
                            pinnedRoutines = pinnedRoutines,
                            completedRoutines = completedRoutines,
                            completedSteps = completedSteps,
                            viewModel = viewModel
                        )
                    }
                }

                if (!hiddenHomeCards.contains("Tasks")) {
                    item {
                        TasksAndDeadlinesCard(
                            sections = sections,
                            allTasks = allTasks,
                            tags = tags,
                            isKanbanView = isKanbanView,
                            viewModel = viewModel,
                            onToggleKanban = { isKanbanView = !isKanbanView },
                            onAddTask = { taskToEdit = null; showTaskDialog = true },
                            onEditTask = { taskToEdit = it; showTaskDialog = true },
                            onDeleteSection = { sectionToDelete = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaCard(agendaText: String, onUpdateText: (String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(agendaText) { mutableStateOf(agendaText) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daily Agenda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = {
                    if (isEditing) {
                        onUpdateText(editText)
                    }
                    isEditing = !isEditing
                }, modifier = Modifier.size(24.dp)) {
                    Icon(if (isEditing) Icons.Default.Check else Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("What's the plan for today?\n- Chapter 1 Notes\n- Gym @ 6PM") },
                    minLines = 3
                )
            } else {
                if (agendaText.isBlank()) {
                    Text("No agenda set for today. Tap the edit icon to plan your day!", color = Color.Gray, fontStyle = FontStyle.Italic)
                } else {
                    val lines = agendaText.split("\n").filter { it.isNotBlank() }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        lines.forEachIndexed { index, line ->
                            val isDone = line.trim().startsWith("✓")
                            val displayText = if (isDone) line.replaceFirst("✓", "").trim() else line.trim()

                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val newLines = lines.toMutableList()
                                    if (isDone) {
                                        newLines[index] = displayText
                                    } else {
                                        newLines[index] = "✓ $displayText"
                                    }
                                    onUpdateText(newLines.joinToString("\n"))
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isDone) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = if (isDone) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (isDone) Color.Gray else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
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

    val initialTags = try { Json.decodeFromString<List<String>>(initialTask?.tagsJson ?: "[]") } catch (_: Exception) { emptyList() }
    var selectedTags by remember { mutableStateOf(initialTags.toSet()) }

    val initialSubtasks = try { Json.decodeFromString<List<SubTask>>(initialTask?.subtasksJson ?: "[]") } catch (_: Exception) { emptyList() }
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
                        Text(deadlineStr.ifBlank { "No Deadline" })
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Section:", style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = { showNewSectionDialog = true }) { Text("+ Add") }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sections, key = { it.id }) { sec ->
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
                            items(tags, key = { it.name }) { tag ->
                                val tagColor = try { Color(tag.colorHex.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
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

@Composable
fun PinnedCardWidget(
    space: SandboxSpace,
    cardBlock: SandboxBlock,
    allSpaces: List<SandboxSpace>,
    tabName: String = "Home",
    onUpdateSpace: (SandboxSpace) -> Unit,
    onNavigateToSpace: (String) -> Unit
) {
    val focusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val recentTableColors = remember { mutableStateListOf<Color>() }
    var focusedBlockId by remember { mutableStateOf<String?>(null) }

    var localCard by remember {
        mutableStateOf(prepareBlocksForUI(listOf(cardBlock)).first())
    }

    LaunchedEffect(space.updatedAt) {
        val freshCardBlock = findPinnedCards(space.blocks, tabName).find { it.id == cardBlock.id }

        if (freshCardBlock != null) {
            if (focusedBlockId == null || localCard.isLocked != freshCardBlock.isLocked) {
                localCard = prepareBlocksForUI(listOf(freshCardBlock)).first()
                if (freshCardBlock.isLocked) {
                    focusedBlockId = null
                }
            }
        }
    }

    var debounceJob by remember { mutableStateOf<Job?>(null) }

    BlockRenderer(
        block = localCard,
        numberIndex = 1,
        allSpaces = allSpaces,
        currentSpaceId = space.id,
        recentTableColors = recentTableColors,
        focusRequesters = focusRequesters,
        focusTargetId = focusedBlockId,
        allBlocks = listOf(localCard),
        isWidgetMode = true,
        isReadOnly = localCard.isLocked,
        onScrollToBlock = {},
        onMove = {},
        onFocusRequest = { focusedBlockId = it },
        onScrollToTop = {},
        onNavigateToSpace = onNavigateToSpace,
        onCreateLinkedSpace = { UUID.randomUUID().toString() },
        onUpdate = { updatedCard ->
            localCard = updatedCard

            debounceJob?.cancel()
            @OptIn(DelicateCoroutinesApi::class)
            debounceJob = GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                withContext(NonCancellable) {
                    delay(500.milliseconds)
                    val cleanCard = cleanBlocksForDB(listOf(updatedCard)).first()
                    val newBlocks = replaceBlockRecursively(space.blocks, cleanCard)
                    onUpdateSpace(space.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()))
                }
            }
        },
        onSplitBlock = { _, _, _ -> },
        onDeleteBlock = { _, deleteEntirely ->
            if (deleteEntirely) {
                debounceJob?.cancel()
                val newBlocks = removeBlockRecursively(space.blocks, localCard.id)
                onUpdateSpace(space.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()))
            }
        }
    )
}