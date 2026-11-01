package com.example.loom.ui.calendar

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.loom.data.local.entity.ConsumedFoodLog
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.FoodItem
import com.example.loom.data.local.entity.PresetExercise
import com.example.loom.data.local.entity.SkincareProduct
import com.example.loom.data.local.entity.SkincareRoutine
import com.example.loom.ui.health.ProductProfileDialog
import com.example.loom.ui.health.ReorderableProductList
import com.example.loom.ui.health.SegmentedControl
import com.example.loom.ui.health.SupplementItem
import com.example.loom.ui.health.supplementTemplates
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private val timelineDateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat {
        return SimpleDateFormat("h:mm a", Locale.getDefault())
    }
}
private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

private fun Float.formatWeight(): String {
    return if (this % 1f == 0f) this.toInt().toString() else this.toString()
}

@Composable
fun SimpleMacroRing(calories: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 5.dp.toPx()
            drawArc(
                color = Color(0xFF4CAF50).copy(alpha = 0.3f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            val fillPercentage = (calories.toFloat() / 2500f).coerceIn(0f, 1f)
            drawArc(
                color = Color(0xFF4CAF50),
                startAngle = -90f,
                sweepAngle = 360f * fillPercentage,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$calories", fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
            Text("kcal", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 8.sp)
        }
    }
}

@Composable
fun AnalyticsBox(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val currentMonth by viewModel.currentMonth.collectAsState()
    val activeDays by viewModel.activeDays.collectAsState()

    val selectedDateId by viewModel.selectedDateId.collectAsState()
    val snapshot by viewModel.selectedDateSnapshot.collectAsState()
    val record = snapshot?.record

    val analytics by viewModel.monthlyAnalytics.collectAsState()
    var isMonthView by remember { mutableStateOf(true) }

    var showFoodDialog by remember { mutableStateOf(false) }
    var showFoodPantryDialog by remember { mutableStateOf(false) }
    var foodToLog by remember { mutableStateOf<FoodItem?>(null) }
    var showNewFoodDialog by remember { mutableStateOf(false) }
    var foodToDelete by remember { mutableStateOf<FoodItem?>(null) }
    var logDetailsToShow by remember { mutableStateOf<ConsumedFoodLog?>(null) }

    val allPresets by viewModel.allPresets.collectAsState()
    val globalTemplates by viewModel.exerciseTemplates.collectAsState()
    var showMacroEditDialog by remember { mutableStateOf(false) }
    var showDetailedExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val skincareRoutines by viewModel.allSkincareRoutines.collectAsState()
    val skincareProducts by viewModel.allSkincareProducts.collectAsState()
    var showSkincareDialog by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<SkincareRoutine?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var editingProductContext by remember { mutableStateOf<Pair<SkincareRoutine, SkincareProduct>?>(null) }
    var skincareRoutineToRemove by remember { mutableStateOf<String?>(null) }

    val baseWeight by viewModel.weightFlow.collectAsState()
    var showWeightLogDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    val supplementsRoster by viewModel.supplementsRoster.collectAsState()
    val todayScheduled by viewModel.selectedDateScheduledSupplements.collectAsState()
    val categories by viewModel.supplementCategories.collectAsState()
    var showSupplementManageDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingSupplementItem by remember { mutableStateOf<SupplementItem?>(null) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(Unit) {
        viewModel.selectDate(selectedDate.format(dayMonthYearFormatter))
    }

    if (showWeightLogDialog) {
        val currentLoggedWeight = record?.weightLogKg ?: 0f
        var sliderWeight by remember { mutableFloatStateOf(if (currentLoggedWeight > 0f) currentLoggedWeight else baseWeight) }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = (sliderWeight.toInt() - 20).coerceAtLeast(0))

        AlertDialog(
            onDismissRequest = { showWeightLogDialog = false },
            title = { Text("Log Weight", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", sliderWeight)} kg",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyRow(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 60.dp)
                    ) {
                        val minWeight = 20
                        val maxWeight = 250
                        items(maxWeight - minWeight + 1, key = { it }) { index ->
                            val itemWeight = minWeight + index
                            val isSelected = itemWeight == sliderWeight.toInt()
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val decimal = sliderWeight - sliderWeight.toInt()
                                        sliderWeight = itemWeight.toFloat() + decimal
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = itemWeight.toString(),
                                    style = if (isSelected) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { sliderWeight = (sliderWeight - 1f).coerceAtLeast(20f) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp)) { Text("-1") }
                        OutlinedButton(onClick = { sliderWeight = (sliderWeight - 0.1f).coerceAtLeast(20f) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp)) { Text("-.1") }
                        OutlinedButton(onClick = { sliderWeight += 0.1f }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp)) { Text("+.1") }
                        OutlinedButton(onClick = { sliderWeight += 1f }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp)) { Text("+1") }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val finalWeight = String.format(Locale.US, "%.1f", sliderWeight).toFloat()
                    viewModel.logDailyWeight(finalWeight)
                    showWeightLogDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentLoggedWeight > 0f) {
                        TextButton(onClick = { viewModel.deleteDailyWeight(); showWeightLogDialog = false }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = { showWeightLogDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    if (showSleepDialog) {
        val currentSleep = record?.sleepHours ?: 0f
        val currentRating = record?.sleepRating ?: 0
        var sliderHours by remember { mutableFloatStateOf(if (currentSleep > 0f) currentSleep else 7.5f) }
        var rating by remember { mutableIntStateOf(if (currentRating > 0) currentRating else 3) }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = (sliderHours * 2).toInt() - 4)

        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("Log Sleep & Recovery", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("${String.format(Locale.US, "%.1f", sliderHours)} Hours", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    LazyRow(state = listState, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 60.dp)) {
                        items(24, key = { it }) { index ->
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
                Button(onClick = { viewModel.logSleep(sliderHours, rating); showSleepDialog = false }) { Text("Save") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentSleep > 0f) {
                        TextButton(onClick = { viewModel.deleteSleep(); showSleepDialog = false }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = { showSleepDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    if (skincareRoutineToRemove != null) {
        AlertDialog(
            onDismissRequest = { skincareRoutineToRemove = null },
            title = { Text("Remove Routine") },
            text = { Text("Remove '$skincareRoutineToRemove' from this day's logs?") },
            confirmButton = {
                Button(onClick = {
                    val routineObj = skincareRoutines.find { it.name == skincareRoutineToRemove }
                    val products = routineObj?.let {
                        try { Json.decodeFromString<List<SkincareProduct>>(it.productsJson).map { p -> p.name } } catch(_: Exception) { emptyList() }
                    } ?: emptyList()
                    viewModel.removeLoggedSkincareRoutine(skincareRoutineToRemove!!, products)
                    skincareRoutineToRemove = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { skincareRoutineToRemove = null }) { Text("Cancel") } }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete '$categoryToDelete'?\n(Items in this category will still exist but should be updated)") },
            confirmButton = {
                Button(onClick = { viewModel.deleteCategory(categoryToDelete!!); categoryToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Manage Categories", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Hold a category to delete it.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(categories, key = { it }) { cat ->
                            Row(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { categoryToDelete = cat }).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(cat, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = { Text("New Category") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { Button(onClick = { if (newCategoryName.isNotBlank()) { viewModel.saveCategory(newCategoryName.trim()); newCategoryName = "" } }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Close") } }
        )
    }

    if (editingSupplementItem != null) {
        var editName by remember { mutableStateOf(editingSupplementItem!!.name) }
        var editDosage by remember { mutableStateOf(editingSupplementItem!!.dosage) }
        var editCategory by remember { mutableStateOf(editingSupplementItem!!.category.ifBlank { categories.firstOrNull() ?: "Supplement" }) }
        var editTime by remember { mutableStateOf(editingSupplementItem!!.timeOfUse.ifBlank { "Anytime" }) }
        var editRecurrenceType by remember { mutableStateOf(editingSupplementItem!!.recurrenceType.ifBlank { "DAILY" }) }
        var editRecurrenceVal by remember { mutableStateOf(editingSupplementItem!!.recurrenceValue) }
        var editInstruction by remember { mutableStateOf(editingSupplementItem!!.instruction.ifBlank { "None" }) }
        var showSuggestions by remember { mutableStateOf(false) }
        var catExpanded by remember { mutableStateOf(false) }
        var timeExpanded by remember { mutableStateOf(false) }
        var instExpanded by remember { mutableStateOf(false) }

        val allOptions = (supplementTemplates + supplementsRoster).distinctBy { it.name.lowercase(Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { editingSupplementItem = null },
            title = { Text(if (editingSupplementItem!!.id.isEmpty()) "Add Item" else "Edit Item") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it; showSuggestions = true }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) showSuggestions = true })
                    val filtered = if (editName.isBlank()) allOptions else allOptions.filter { it.name.contains(editName, ignoreCase = true) }
                    if (filtered.isNotEmpty() && showSuggestions) {
                        ElevatedCard(modifier = Modifier.fillMaxWidth().height(160.dp), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filtered, key = { it.name }) { template ->
                                    Row(modifier = Modifier.fillMaxWidth().clickable { editName = template.name; editDosage = template.dosage; editCategory = template.category; editTime = template.timeOfUse; editInstruction = template.instruction; showSuggestions = false; focusManager.clearFocus() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(template.name, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                    OutlinedTextField(value = editDosage, onValueChange = { editDosage = it }, label = { Text("Dosage (e.g. 500mg)") }, modifier = Modifier.fillMaxWidth())
                    ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                        OutlinedTextField(value = editCategory, onValueChange = {}, readOnly = true, label = { Text("Category") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) })
                        ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                            categories.forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { editCategory = opt; catExpanded = false }) }
                        }
                    }
                    ExposedDropdownMenuBox(expanded = timeExpanded, onExpandedChange = { timeExpanded = it }) {
                        OutlinedTextField(value = editTime, onValueChange = {}, readOnly = true, label = { Text("Time of Use") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded) })
                        ExposedDropdownMenu(expanded = timeExpanded, onDismissRequest = { timeExpanded = false }) {
                            listOf("Morning", "Afternoon", "Evening", "Before Bed", "Anytime").forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { editTime = opt; timeExpanded = false }) }
                        }
                    }
                    ExposedDropdownMenuBox(expanded = instExpanded, onExpandedChange = { instExpanded = it }) {
                        OutlinedTextField(value = editInstruction, onValueChange = {}, readOnly = true, label = { Text("Instructions") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = instExpanded) })
                        ExposedDropdownMenu(expanded = instExpanded, onDismissRequest = { instExpanded = false }) {
                            listOf("None", "With Food", "Empty Stomach", "With Water").forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { editInstruction = opt; instExpanded = false }) }
                        }
                    }
                    Text("Recurrence", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SegmentedControl(options = listOf("DAILY", "DAYS", "INTERVAL"), selectedOption = if(editRecurrenceType == "SPECIFIC_DAYS") "DAYS" else editRecurrenceType, onOptionSelected = { editRecurrenceType = if(it == "DAYS") "SPECIFIC_DAYS" else it })
                    if (editRecurrenceType == "SPECIFIC_DAYS") {
                        val daysOfWeek = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                        val selectedDays = editRecurrenceVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            daysOfWeek.forEach { day ->
                                val isSel = selectedDays.contains(day)
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable { if (isSel) selectedDays.remove(day) else selectedDays.add(day); editRecurrenceVal = selectedDays.joinToString(",") }, contentAlignment = Alignment.Center) { Text(day.take(1), color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    } else if (editRecurrenceType == "INTERVAL") {
                        OutlinedTextField(value = editRecurrenceVal, onValueChange = { editRecurrenceVal = it }, label = { Text("Every X Days (e.g. 2)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val idToSave = editingSupplementItem!!.id.ifEmpty { java.util.UUID.randomUUID().toString() }
                    viewModel.saveSupplementItem(SupplementItem(idToSave, editName, editDosage, editCategory, editTime, editRecurrenceType, editRecurrenceVal, editInstruction))
                    editingSupplementItem = null
                }) { Text("Save") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingSupplementItem!!.id.isNotEmpty()) {
                        TextButton(onClick = { viewModel.deleteSupplementItem(editingSupplementItem!!.id); editingSupplementItem = null }) { Text("Delete", color = Color(0xFFE53935)) }
                    }
                    TextButton(onClick = { editingSupplementItem = null }) { Text("Cancel") }
                }
            }
        )
    }

    if (showSupplementManageDialog && editingSupplementItem == null) {
        AlertDialog(
            onDismissRequest = { showSupplementManageDialog = false },
            title = { Text("Medication & Supplements", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    // --- FIX: Added Create button here! ---
                    item {
                        TextButton(
                            onClick = {
                                editingSupplementItem = SupplementItem(id = "", name = "", dosage = "", category = categories.firstOrNull() ?: "Supplement", timeOfUse = "Anytime", recurrenceType = "DAILY", recurrenceValue = "", instruction = "None")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ Create New Supplement", fontWeight = FontWeight.Bold)
                        }
                    }

                    val groupedRoster = supplementsRoster.groupBy { it.category }
                    groupedRoster.forEach { (cat, items) ->
                        item {
                            Text(cat, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        }
                        items(items, key = { it.id }) { item ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { editingSupplementItem = item }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold)
                                    Text("${item.dosage} • ${if(item.recurrenceType=="DAILY") "Daily" else "Scheduled"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showSupplementManageDialog = false }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { showCategoryDialog = true }) { Text("Categories") } }
        )
    }

    if (foodToDelete != null) {
        AlertDialog(
            onDismissRequest = { foodToDelete = null },
            title = { Text("Delete Food?") },
            text = { Text("Permanently delete '${foodToDelete?.name}' from the pantry?") },
            confirmButton = { Button(onClick = { viewModel.deleteFoodItem(foodToDelete!!); foodToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { foodToDelete = null }) { Text("Cancel") } }
        )
    }

    if (logDetailsToShow != null) {
        val log = logDetailsToShow!!
        val timeStr = timelineDateFormatter.get()?.format(Date(log.timestamp)) ?: ""
        AlertDialog(
            onDismissRequest = { logDetailsToShow = null },
            title = { Text("Meal Details") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(80.dp)) { Box(contentAlignment = Alignment.Center) { Text(log.emoji, fontSize = 40.sp) } }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(log.foodName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

                    val unitDisplay = log.servingUnit.ifBlank { "serving" }
                    val uText = unitDisplay.trim()
                    val mResult = "\\d+(\\.\\d+)?".toRegex().find(uText)
                    val calculatedNumStr: String
                    val finalUnitText: String
                    val showMultiplier: Boolean

                    if (mResult != null) {
                        val baseNum = mResult.value.toFloat()
                        calculatedNumStr = (baseNum * log.servings).formatWeight()
                        finalUnitText = uText.replaceFirst(mResult.value, "").trim()
                        showMultiplier = false
                    } else {
                        calculatedNumStr = ""
                        finalUnitText = uText
                        showMultiplier = true
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (finalUnitText.isNotBlank()) { Text(finalUnitText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.width(4.dp)) }
                        if (showMultiplier) Text("${log.servings.formatWeight()}x", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else Text(calculatedNumStr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("•  $timeStr", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${log.calories}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium); Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${log.protein}g", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), style = MaterialTheme.typography.titleMedium); Text("Pro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${log.carbs}g", fontWeight = FontWeight.Bold, color = Color(0xFFFFA726), style = MaterialTheme.typography.titleMedium); Text("Carbs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${log.fats}g", fontWeight = FontWeight.Bold, color = Color(0xFFE53935), style = MaterialTheme.typography.titleMedium); Text("Fats", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            },
            confirmButton = { Button(onClick = { logDetailsToShow = null }) { Text("Close") } },
            dismissButton = { TextButton(onClick = { viewModel.removeConsumedFoodLog(log); logDetailsToShow = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete Log") } }
        )
    }

    if (foodToLog != null && showFoodDialog) {
        var servingsStr by remember { mutableStateOf("1") }
        AlertDialog(
            onDismissRequest = { showFoodDialog = false; foodToLog = null },
            title = { Text("Log ${foodToLog!!.name}") },
            text = {
                Column {
                    val currentMultiplier = servingsStr.toFloatOrNull() ?: 1f
                    val dynamicCals = foodToLog!!.calories * currentMultiplier
                    val unitText = foodToLog!!.servingUnit.trim()
                    val matchResult = "\\d+(\\.\\d+)?".toRegex().find(unitText)

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

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (textPart.isNotBlank()) { Text(text = textPart, style = MaterialTheme.typography.bodyLarge); Spacer(modifier = Modifier.width(6.dp)) }
                        Text(text = formattedNum, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.width(12.dp)); Text(text = "=", style = MaterialTheme.typography.bodyLarge); Spacer(modifier = Modifier.width(12.dp))
                        val calsStr = if (dynamicCals % 1f == 0f) dynamicCals.toInt().toString() else String.format(Locale.US, "%.1f", dynamicCals)
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(text = "$calsStr kcal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = servingsStr, onValueChange = { servingsStr = it }, label = { Text("Multiplier (e.g., 2 for double, 0.5 for half)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { Button(onClick = { viewModel.logFoodItemToPast(foodToLog!!, servingsStr.toFloatOrNull() ?: 1f); showFoodDialog = false; foodToLog = null }) { Text("Log Meal") } },
            dismissButton = { TextButton(onClick = { showFoodDialog = false; foodToLog = null }) { Text("Cancel") } }
        )
    }

    if (showFoodPantryDialog) {
        val currentSearchQuery by viewModel.foodSearchQuery.collectAsState()
        val searchedFoods by viewModel.searchedFoodItems.collectAsState()

        AlertDialog(
            onDismissRequest = { showFoodPantryDialog = false },
            title = { Text("Edit Pantry & Log Meals") },
            text = {
                Column(modifier = Modifier.heightIn(max = 500.dp)) {
                    OutlinedTextField(value = currentSearchQuery, onValueChange = { viewModel.updateFoodSearchQuery(it) }, placeholder = { Text("Search foods to log/edit...") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { TextButton(onClick = { showNewFoodDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Create New Food") } }
                        items(searchedFoods, key = { it.id }) { food ->
                            Surface(
                                shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { foodToLog = food; showFoodDialog = true; showFoodPantryDialog = false }, onLongClick = { foodToDelete = food })
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🍽️", fontSize = 24.sp); Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(food.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(text = "P: ${food.protein}g • C: ${food.carbs}g • F: ${food.fats}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        val uText = food.servingUnit.trim()
                                        val mResult = "\\d+(\\.\\d+)?".toRegex().find(uText)
                                        val numStr = mResult?.value ?: ""
                                        val textStr = if (mResult != null) uText.replaceFirst(numStr, "").trim() else uText

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "${food.calories} kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                    if (textStr.isNotBlank()) { Text(text = textStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.width(4.dp)) }
                                                    if (numStr.isNotBlank()) { Text(text = numStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold) }
                                                }
                                            }
                                        }
                                    }
                                }
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
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Food Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
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
                        viewModel.addFoodItem(FoodItem(name = inputName, servingUnit = inputServing, calories = inputCals.toFloatOrNull() ?: 0f, protein = inputPro.toFloatOrNull() ?: 0f, carbs = inputCarbs.toFloatOrNull() ?: 0f, fats = inputFats.toFloatOrNull() ?: 0f))
                        showNewFoodDialog = false
                    }
                }) { Text("Save Food") }
            },
            dismissButton = { TextButton(onClick = { showNewFoodDialog = false }) { Text("Cancel") } }
        )
    }

    if (showMacroEditDialog) {
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
                    viewModel.updatePastMacros(editCals.toIntOrNull() ?: 0, editPro.toIntOrNull() ?: 0, editCarbs.toIntOrNull() ?: 0, editFats.toIntOrNull() ?: 0)
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
            confirmButton = { Button(onClick = { viewModel.deletePastExercise(exerciseToDelete!!); exerciseToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { exerciseToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showDetailedExerciseDialog) {
        Dialog(
            onDismissRequest = { showDetailedExerciseDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(modifier = Modifier.fillMaxWidth(0.95f).imePadding().padding(vertical = 24.dp).heightIn(max = 750.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
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
                                        OutlinedTextField(value = inputName, onValueChange = { inputName = it; expanded = true }, label = { Text("Search Exercise") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth())
                                        val filtered = globalTemplates.filter { it.name.contains(inputName, true) }
                                        val exactMatch = globalTemplates.any { it.name.equals(inputName, ignoreCase = true) }

                                        if ((filtered.isNotEmpty() || (inputName.isNotBlank() && !exactMatch)) && expanded) {
                                            ExposedDropdownMenu(expanded = true, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                                                filtered.forEach { option -> Box(modifier = Modifier.fillMaxWidth().clickable { inputName = option.name; expanded = false }.padding(16.dp)) { Text(option.name) } }
                                                if (inputName.isNotBlank() && !exactMatch) { Box(modifier = Modifier.fillMaxWidth().clickable { viewModel.createExerciseTemplate(inputName); expanded = false }.padding(16.dp)) { Text("Create new exercise: \"$inputName\"", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } }
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
                                        if (isTimeBased) { OutlinedTextField(value = inputDuration, onValueChange = { inputDuration = it }, label = { Text("Seconds") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)) }
                                        else { OutlinedTextField(value = inputReps, onValueChange = { inputReps = it }, label = { Text("Reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)) }
                                    }

                                    if (!isTimeBased) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = varyWeight, onCheckedChange = { varyWeight = it }, modifier = Modifier.scale(0.8f)); Spacer(modifier = Modifier.width(8.dp)); Text("Different weight per set", style = MaterialTheme.typography.bodySmall) }

                                        if (varyWeight) {
                                            val setCount = inputSets.toIntOrNull() ?: 0
                                            if (setCount > 0) {
                                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    for (i in 0 until setCount) { OutlinedTextField(value = advancedWeights[i] ?: "", onValueChange = { newVal -> advancedWeights = advancedWeights.toMutableMap().apply { put(i, newVal) } }, label = { Text("Set ${i + 1} (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().height(56.dp)) }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(value = inputWeight, onValueChange = { inputWeight = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { showDetailedExerciseDialog = false }) { Text("Cancel") }
                                    Button(onClick = {
                                        if (inputName.isNotBlank()) {
                                            val wList = if (varyWeight) { val setCount = inputSets.toIntOrNull() ?: 0; (0 until setCount).map { advancedWeights[it]?.toFloatOrNull() ?: 0f } } else emptyList()
                                            viewModel.addPastExercise(name = inputName, sets = inputSets.toIntOrNull() ?: 0, reps = inputReps.toIntOrNull() ?: 0, weight = inputWeight.toFloatOrNull() ?: 0f, weightListJson = Json.encodeToString(wList), isTimeBased = isTimeBased, duration = inputDuration.toIntOrNull() ?: 0)
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
                                            value = activePresetName.ifEmpty { "Select a Preset..." },
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                                .fillMaxWidth(),
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetDropdownExpanded)
                                            },
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                        )

                                        ExposedDropdownMenu(
                                            expanded = presetDropdownExpanded,
                                            onDismissRequest = { presetDropdownExpanded = false }
                                        ) {
                                            allPresets.forEach { preset ->
                                                DropdownMenuItem(
                                                    text = { Text(preset.presetName) },
                                                    onClick = {
                                                        activePresetName = preset.presetName
                                                        activeExercises = preset.exercises
                                                        presetDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(8.dp)) {
                                        if (activeExercises.isEmpty()) { Text("No exercises selected.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp)) }
                                        activeExercises.forEach { ex ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(ex.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                    if (ex.isTimeBased) { Text("${ex.sets} Sets × ${ex.durationInSeconds} Secs", style = MaterialTheme.typography.bodySmall) }
                                                    else { val wList = try { Json.decodeFromString<List<Float>>(ex.weightListJson) } catch(_:Exception){ emptyList() }; if (wList.isNotEmpty()) { Text("${ex.sets} Sets × ${ex.reps} Reps @ ${wList.joinToString(", ") { it.formatWeight() }}kg", style = MaterialTheme.typography.bodySmall) } else { Text("${ex.sets} Sets × ${ex.reps} Reps @ ${ex.weight.formatWeight()}kg", style = MaterialTheme.typography.bodySmall) } }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { showDetailedExerciseDialog = false }) { Text("Cancel") }
                                    Button(onClick = { if (activePresetName.isNotBlank() && activeExercises.isNotEmpty()) { viewModel.logPresetExercisesToPast(activePresetName, activeExercises); Toast.makeText(context, "Preset Logged!", Toast.LENGTH_SHORT).show(); showDetailedExerciseDialog = false } }) { Text("Log Entire Preset") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingProductContext != null) {
        val routineContext = editingProductContext!!.first
        val oldProduct = editingProductContext!!.second
        ProductProfileDialog(
            initialProduct = oldProduct,
            onDismiss = { editingProductContext = null },
            onSave = { updatedProduct ->
                val currentProducts = try { Json.decodeFromString<List<SkincareProduct>>(routineContext.productsJson) } catch (_: Exception) { emptyList() }
                val updatedList = currentProducts.map { if (it.name == oldProduct.name) updatedProduct else it }
                viewModel.saveSkincareRoutine(routineContext.copy(productsJson = Json.encodeToString(updatedList)))
                editingProductContext = null
            },
            onDelete = {
                val currentProducts = try { Json.decodeFromString<List<SkincareProduct>>(routineContext.productsJson) } catch (_: Exception) { emptyList() }
                val updatedList = currentProducts.filter { it.name != oldProduct.name }
                viewModel.saveSkincareRoutine(routineContext.copy(productsJson = Json.encodeToString(updatedList)))
                editingProductContext = null
            }
        )
    }

    if (showDeleteConfirmDialog && routineToEdit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Preset?") },
            text = { Text("This will permanently delete the '${routineToEdit?.name}' template. If you just want to remove it from this day's list, swipe it away on the home screen instead.") },
            confirmButton = { Button(onClick = { viewModel.deleteSkincareRoutine(routineToEdit!!); showDeleteConfirmDialog = false; showSkincareDialog = false; routineToEdit = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete Template") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") } }
        )
    }

    if (showSkincareDialog) {
        var inputRoutineName by remember(routineToEdit) { mutableStateOf(routineToEdit?.name ?: "") }
        var isPinnedToHome by remember(routineToEdit) { mutableStateOf(routineToEdit?.isPinnedToHome ?: true) }
        val initialProducts = remember(routineToEdit) {
            try { Json.decodeFromString<List<SkincareProduct>>(routineToEdit?.productsJson ?: "[]") } catch(_: Exception){ try { Json.decodeFromString<List<String>>(routineToEdit?.productsJson ?: "[]").map { SkincareProduct(name = it) } } catch (_: Exception) { emptyList() } }
        }
        var selectedProducts by remember(routineToEdit) { mutableStateOf(initialProducts) }
        var routineDropdownExpanded by remember { mutableStateOf(false) }
        var productSearchText by remember { mutableStateOf("") }
        var productDropdownExpanded by remember { mutableStateOf(false) }
        var dialogProductToEdit by remember { mutableStateOf<SkincareProduct?>(null) }

        if (dialogProductToEdit != null) {
            ProductProfileDialog(
                initialProduct = dialogProductToEdit!!,
                onDismiss = { dialogProductToEdit = null },
                onSave = { updated -> selectedProducts = selectedProducts.map { if (it.name == dialogProductToEdit!!.name) updated else it }; dialogProductToEdit = null },
                onDelete = { selectedProducts = selectedProducts.filter { it.name != dialogProductToEdit!!.name }; dialogProductToEdit = null }
            )
        }

        AlertDialog(
            onDismissRequest = { showSkincareDialog = false; routineToEdit = null },
            title = { Text(if (routineToEdit == null) "New Skincare Routine" else "Edit Routine", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    item {
                        ExposedDropdownMenuBox(expanded = routineDropdownExpanded, onExpandedChange = { routineDropdownExpanded = it }) {
                            OutlinedTextField(value = inputRoutineName, onValueChange = { inputRoutineName = it; routineDropdownExpanded = true }, label = { Text("Routine Name (e.g., Morning Wash)") }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, true), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routineDropdownExpanded) }, colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(), singleLine = true)
                            val filteredRoutines = skincareRoutines.filter { it.name.contains(inputRoutineName, true) }
                            if (filteredRoutines.isNotEmpty() && routineDropdownExpanded) {
                                ExposedDropdownMenu(expanded = true, onDismissRequest = { routineDropdownExpanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                                    filteredRoutines.forEach { routine -> DropdownMenuItem(text = { Text(routine.name) }, onClick = { routineToEdit = routine; routineDropdownExpanded = false }) }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = isPinnedToHome, onCheckedChange = { isPinnedToHome = it }, modifier = Modifier.scale(0.8f)); Spacer(modifier = Modifier.width(8.dp)); Text("Pin to Home Screen", style = MaterialTheme.typography.bodyMedium) }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Products (Application Order):", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        ReorderableProductList(products = selectedProducts, onReorder = { newOrder -> selectedProducts = newOrder }, onRemove = { removedProduct -> selectedProducts = selectedProducts.filter { it.name != removedProduct.name } }, onLongClick = { dialogProductToEdit = it })

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExposedDropdownMenuBox(expanded = productDropdownExpanded, onExpandedChange = { productDropdownExpanded = it }, modifier = Modifier.weight(1f)) {
                                OutlinedTextField(value = productSearchText, onValueChange = { productSearchText = it; productDropdownExpanded = true }, placeholder = { Text("Search or add...") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(), singleLine = true)
                                val filteredProducts = skincareProducts.filter { it.name.contains(productSearchText, true) && !selectedProducts.any { sp -> sp.name == it.name } }
                                val exactMatch = skincareProducts.any { it.name.equals(productSearchText.trim(), ignoreCase = true) }
                                if ((filteredProducts.isNotEmpty() || (productSearchText.isNotBlank() && !exactMatch)) && productDropdownExpanded) {
                                    ExposedDropdownMenu(expanded = true, onDismissRequest = { productDropdownExpanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                                        filteredProducts.forEach { product -> Box(modifier = Modifier.fillMaxWidth().clickable(onClick = { selectedProducts = selectedProducts + product; productSearchText = ""; productDropdownExpanded = false }).padding(16.dp)) { Text(product.name) } }
                                        if (productSearchText.isNotBlank() && !exactMatch) {
                                            DropdownMenuItem(text = { Text("Create new: \"${productSearchText.trim()}\"", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }, onClick = { val trimmed = productSearchText.trim(); viewModel.addSkincareProduct(trimmed); selectedProducts = selectedProducts + SkincareProduct(name = trimmed); productSearchText = ""; productDropdownExpanded = false })
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                val trimmed = productSearchText.trim()
                                if (trimmed.isNotBlank() && !selectedProducts.any { it.name == trimmed }) { viewModel.addSkincareProduct(trimmed); selectedProducts = selectedProducts + SkincareProduct(name = trimmed); productSearchText = ""; productDropdownExpanded = false }
                            }) { Icon(Icons.Default.Add, "Add Product", tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (inputRoutineName.isNotBlank()) {
                                    val existingId = routineToEdit?.id ?: skincareRoutines.find { it.name.equals(inputRoutineName, ignoreCase = true) }?.id ?: 0
                                    val savedRoutine = SkincareRoutine(id = existingId, name = inputRoutineName, isPinnedToHome = isPinnedToHome, productsJson = Json.encodeToString(selectedProducts))
                                    viewModel.saveSkincareRoutine(savedRoutine)
                                    routineToEdit = savedRoutine
                                    Toast.makeText(context, "Preset Saved!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Save Preset") }

                        Button(
                            onClick = {
                                if (inputRoutineName.isNotBlank()) {
                                    val existingId = routineToEdit?.id ?: skincareRoutines.find { it.name.equals(inputRoutineName, ignoreCase = true) }?.id ?: 0
                                    val savedRoutine = SkincareRoutine(id = existingId, name = inputRoutineName, isPinnedToHome = isPinnedToHome, productsJson = Json.encodeToString(selectedProducts))
                                    viewModel.saveSkincareRoutine(savedRoutine)
                                    viewModel.logSkincareRoutine(inputRoutineName)
                                    showSkincareDialog = false
                                    routineToEdit = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Log Now") }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        if (routineToEdit != null) { TextButton(onClick = { showDeleteConfirmDialog = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete Preset") } } else { Spacer(modifier = Modifier.width(8.dp)) }
                        TextButton(onClick = { showSkincareDialog = false; routineToEdit = null }) { Text("Cancel") }
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                }
                Text(
                    text = currentMonth.format(monthYearFormatter),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                }
            }

            IconButton(
                onClick = { isMonthView = !isMonthView },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = if (isMonthView) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle View",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
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

        AnimatedContent(targetState = isMonthView, label = "CalendarGridSwitch") { showMonth ->
            if (showMonth) {
                val firstDayOfMonth = currentMonth.atDay(1)
                val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7
                val daysInMonth = currentMonth.lengthOfMonth()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dayOfWeekOffset) { Spacer(modifier = Modifier.size(40.dp)) }

                    items(daysInMonth, key = { it }) { dayIndex ->
                        val dayNumber = dayIndex + 1
                        val date = currentMonth.atDay(dayNumber)
                        val dateIdStr = date.format(dayMonthYearFormatter)
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
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    val currentDayOfWeek = selectedDate.dayOfWeek.value % 7
                    val startOfWeek = selectedDate.minusDays(currentDayOfWeek.toLong())
                    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

                    weekDays.forEach { date ->
                        val dateIdStr = date.format(dayMonthYearFormatter)
                        val hasData = activeDays.contains(dateIdStr)
                        val isSelected = date == selectedDate

                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .weight(1f)
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
                                text = date.dayOfMonth.toString(),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected || hasData) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedContent(targetState = selectedDateId != null, label = "BottomCardSwitch") { hasSelection ->
            if (!hasSelection) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).weight(1f, fill = false),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                        Text("${currentMonth.format(monthYearFormatter)} Overview", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AnalyticsBox(title = "Avg Sleep", value = "${String.format(Locale.US, "%.1f", analytics.averageSleep)}h", icon = Icons.Outlined.Bedtime, modifier = Modifier.weight(1f))
                            AnalyticsBox(title = "Workouts", value = "${analytics.workoutDays} Days", icon = Icons.AutoMirrored.Outlined.DirectionsRun, modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AnalyticsBox(title = "Avg Cals", value = "${analytics.averageCalories} kcal", icon = Icons.Outlined.LocalDining, modifier = Modifier.weight(1f))
                            AnalyticsBox(title = "Tasks Done", value = "${analytics.tasksCompleted}/${analytics.totalTasks}", icon = Icons.Outlined.CheckCircle, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Tap any date above to view or edit daily details.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                    }
                }
            } else {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).weight(1f, fill = false),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedDate.format(fullDateFormatter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { viewModel.clearSelectedDate() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SimpleMacroRing(calories = record?.calories ?: 0, modifier = Modifier.size(46.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Fuel", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { showMacroEditDialog = true }, modifier = Modifier.size(20.dp).padding(start = 4.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text("P: ${record?.protein ?: 0}g  C: ${record?.carbs ?: 0}g  F: ${record?.fats ?: 0}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { viewModel.removePastWater(250) }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Remove, "Less", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                                    Icon(Icons.Outlined.WaterDrop, contentDescription = "Water", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp).size(18.dp))
                                    IconButton(onClick = { viewModel.addPastWater(250) }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Add, "More", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                                }
                                val waterLiters = (record?.waterIntakeMl ?: 0) / 1000f
                                Text("${String.format(Locale.US, "%.2f", waterLiters)} L", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 2.dp))
                            }
                        }

                        val consumedList = remember(record?.consumedFoodsJson) { try { Json.decodeFromString<List<ConsumedFoodLog>>(record?.consumedFoodsJson ?: "[]") } catch(_: Exception){ emptyList() } }
                        val completedSupplements = remember(record?.completedSupplementsJson) { try { Json.decodeFromString<List<String>>(record?.completedSupplementsJson ?: "[]") } catch(_: Exception){ emptyList() } }
                        val completedRoutines = remember(record?.completedSkincareRoutinesJson) { try { Json.decodeFromString<List<String>>(record?.completedSkincareRoutinesJson ?: "[]") } catch(_: Exception){ emptyList() } }
                        val completedSteps = remember(record?.completedSkincareStepsJson) { try { Json.decodeFromString<List<String>>(record?.completedSkincareStepsJson ?: "[]") } catch(_: Exception){ emptyList() } }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Nutrition", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { showFoodPantryDialog = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(20.dp)) { Text("+ Add Food", fontSize = 12.sp) }
                        }

                        if (consumedList.isEmpty()) {
                            Text("No meals logged.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            consumedList.sortedByDescending { it.timestamp }.forEach { log ->
                                key(log.id) {
                                    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { viewModel.removeConsumedFoodLog(log); true } else false })
                                    SwipeToDismissBox(
                                        state = dismissState, enableDismissFromStartToEnd = false,
                                        backgroundContent = {
                                            val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                                            val color by animateColorAsState(if (isSwiping) Color(0xFFE53935) else Color.Transparent)
                                            Box(modifier = Modifier.fillMaxSize().background(color, RoundedCornerShape(4.dp)), contentAlignment = Alignment.CenterEnd) {
                                                if (isSwiping) Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(14.dp).padding(end = 4.dp))
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).combinedClickable(onClick = { logDetailsToShow = log }, onLongClick = { logDetailsToShow = log }).padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(log.emoji, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Text(log.foodName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                                Text(" · ${log.calories} kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            val formatT = timelineDateFormatter.get()?.format(Date(log.timestamp)) ?: ""
                                            Text(formatT, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        // --- 3. SUPPLEMENTS (FIXED: Swipe-to-delete removed to prevent global deletes) ---
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Supplements", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { showSupplementManageDialog = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(20.dp)) { Text("Manage", fontSize = 12.sp) }
                        }

                        if (todayScheduled.isEmpty() && completedSupplements.isEmpty()) {
                            Text("No supplements scheduled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val itemsToDisplay = (todayScheduled + supplementsRoster.filter { completedSupplements.contains(it.id) }).distinctBy { it.id }
                            itemsToDisplay.forEach { item ->
                                key(item.id) {
                                    val isDone = completedSupplements.contains(item.id)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).combinedClickable(onClick = { viewModel.toggleSupplement(item.id) }, onLongClick = { editingSupplementItem = item }).padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(if (isDone) Icons.Outlined.CheckCircle else Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${item.name} · ${item.dosage}", style = MaterialTheme.typography.bodySmall, textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None, color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                        if (item.timeOfUse != "Anytime") {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(item.timeOfUse, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Workouts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { showDetailedExerciseDialog = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(20.dp)) { Text("+ Log Exercise", fontSize = 12.sp) }
                        }

                        val exercises = snapshot?.exercises ?: emptyList()
                        if (exercises.isEmpty()) {
                            Text("Rest day.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            exercises.forEach { ex ->
                                val wList = remember(ex.weightListJson) { try { Json.decodeFromString<List<Float>>(ex.weightListJson) } catch(_:Exception){ emptyList() } }
                                val weightStr = if (wList.isNotEmpty()) wList.joinToString(",") { it.formatWeight() } else ex.weight.formatWeight()
                                val details = if (ex.isTimeBased) "${ex.sets}x${ex.durationInSeconds}s" else "${ex.sets}x${ex.reps} | ${weightStr}kg"

                                Row(
                                    modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { viewModel.toggleExerciseCompletion(ex) }, onLongClick = { exerciseToDelete = ex }).padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(if (ex.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (ex.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${ex.exerciseName} · $details", style = MaterialTheme.typography.bodySmall, textDecoration = if (ex.isCompleted) TextDecoration.LineThrough else TextDecoration.None, color = if (ex.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        val visibleRoutines = skincareRoutines.filter { completedRoutines.contains(it.name) }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Skincare", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { routineToEdit = null; showSkincareDialog = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(20.dp)) { Text("Manage", fontSize = 12.sp) }
                        }

                        if (visibleRoutines.isEmpty()) {
                            Text("No routines logged.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            visibleRoutines.forEach { routine ->
                                val products = remember(routine.productsJson) { try { Json.decodeFromString<List<SkincareProduct>>(routine.productsJson) } catch (_: Exception) { emptyList() } }
                                Row(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { skincareRoutineToRemove = routine.name }).padding(top = 4.dp, bottom = 2.dp)) {
                                    Text(routine.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                products.forEach { product ->
                                    val stepId = "${routine.name}_${product.name}"
                                    val isStepDone = completedSteps.contains(stepId)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().combinedClickable(
                                            onClick = { viewModel.toggleSkincareStep(routine.name, product.name) },
                                            onLongClick = { editingProductContext = Pair(routine, product) }
                                        ).padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(if (isStepDone) Icons.Outlined.CheckCircle else Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isStepDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${product.timeOfUseEmoji} ", fontSize = 12.sp)
                                        Text(product.name, style = MaterialTheme.typography.bodySmall, textDecoration = if (isStepDone) TextDecoration.LineThrough else TextDecoration.None, color = if (isStepDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }

                        val loggedHours = record?.sleepHours ?: 0f
                        val loggedWeight = record?.weightLogKg ?: 0f
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Daily Metrics", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }

                        Row(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { if (loggedHours == 0f) showSleepDialog = true }, onLongClick = { showSleepDialog = true }).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Bedtime, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (loggedHours > 0f) "Sleep · ${String.format(Locale.US, "%.1f", loggedHours)}h" else "Log Sleep...", style = MaterialTheme.typography.bodySmall, color = if (loggedHours > 0f) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
                            val loggedRating = record?.sleepRating ?: 0
                            if (loggedRating > 0) {
                                Spacer(modifier = Modifier.weight(1f))
                                Row { (1..5).forEach { star -> Icon(Icons.Default.Star, null, tint = if (star <= loggedRating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.3f), modifier = Modifier.size(10.dp)) } }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { if (loggedWeight == 0f) showWeightLogDialog = true }, onLongClick = { showWeightLogDialog = true }).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Scale, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (loggedWeight > 0f) "Weight · ${loggedWeight.formatWeight()} kg" else "Log Weight...", style = MaterialTheme.typography.bodySmall, color = if (loggedWeight > 0f) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
                        }

                        val tasks = snapshot?.tasks ?: emptyList()
                        val diaryEntry = record?.diaryEntry ?: ""
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Tasks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
                        if (tasks.isEmpty()) {
                            Text("No tasks scheduled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            tasks.forEach { task ->
                                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (task.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(task.title, style = MaterialTheme.typography.bodySmall, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None, color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        if (diaryEntry.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Note", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
                            Text("\"$diaryEntry\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}