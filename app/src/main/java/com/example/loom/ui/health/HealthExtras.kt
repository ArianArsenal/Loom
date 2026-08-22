package com.example.loom.ui.health

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FastingTimerCard(viewModel: HealthViewModel) {
    val isFasting by viewModel.isFasting.collectAsState()
    val startTime by viewModel.fastingStartTime.collectAsState()
    val goalHours by viewModel.fastingGoalHours.collectAsState()

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedGoal by remember { mutableIntStateOf(goalHours) }

    LaunchedEffect(goalHours) { selectedGoal = goalHours }
    LaunchedEffect(isFasting) { while (isFasting) { delay(1000.milliseconds); currentTime = System.currentTimeMillis() } }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Intermittent Fasting", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
                Button(onClick = {
                    viewModel.logSleep(sliderHours, rating)
                    showDialog = false
                }) { Text("Save Sleep") }
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
                Surface(shape = RoundedCornerShape(20.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth().clickable { showDialog = true }) {
                    Box(modifier = Modifier.background(Brush.linearGradient(listOf(Color(0xFF383A59), Color(0xFF26273B)))).padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("${String.format(Locale.US, "%.1f", loggedHours)} Hours", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    (1..5).forEach { star -> Icon(Icons.Default.Star, contentDescription = null, tint = if (star <= loggedRating) Color(0xFFFFD700) else Color.White.copy(alpha=0.2f), modifier = Modifier.size(18.dp)) }
                                }
                            }
                            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(36.dp), onClick = { viewModel.logSleep(0f, 0) }) {
                                Icon(Icons.Default.Close, contentDescription = "Delete Sleep Log", tint = Color.White, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            } else {
                OutlinedButton(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Log Last Night's Sleep") }
            }
        }
    }
}

@Composable
fun BodyMeasurementsCard(viewModel: HealthViewModel) {
    val todayRecord by viewModel.todayRecord.collectAsState()
    val rawJson = todayRecord?.measurementsJson ?: "{}"
    val measurements = remember(rawJson) { try { Json.decodeFromString<Map<String, Float>>(rawJson.ifBlank { "{}" }) } catch (_: Exception) { emptyMap() } }

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

val supplementTemplates = listOf(
    SupplementItem("t1","Vitamin D3","2000 IU","Supplement","Morning","DAILY","","With Food", false),
    SupplementItem("t2","Vitamin C","500mg","Supplement","Anytime","DAILY","","None", false),
    SupplementItem("t3","Magnesium","200mg","Supplement","Before Bed","DAILY","","None", false),
    SupplementItem("t4","Zinc","15mg","Supplement","Evening","DAILY","","With Food", false),
    SupplementItem("t5","Omega-3 Fish Oil","1000mg","Supplement","Morning","DAILY","","With Food", false),
    SupplementItem("t6","Creatine","5g","Supplement","Anytime","DAILY","","With Water", false),
    SupplementItem("t7","Whey Protein","30g","Supplement","Anytime","DAILY","","With Water", false),
    SupplementItem("t8","Multivitamin","1 pill","Supplement","Morning","DAILY","","With Food", false),
    SupplementItem("t9","B-Complex","1 cap","Supplement","Morning","DAILY","","With Food", false),
    SupplementItem("t10","Iron","18mg","Supplement","Anytime","DAILY","","With Food", false),
    SupplementItem("t11","Vitamin B12","1000mcg","Supplement","Morning","DAILY","","None", false),
    SupplementItem("t12","Ashwagandha","500mg","Supplement","Evening","DAILY","","None", false),
    SupplementItem("t13","Probiotics","1 cap","Supplement","Morning","DAILY","","Empty Stomach", false)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SupplementsCard(viewModel: HealthViewModel) {
    val roster by viewModel.supplementsRoster.collectAsState()
    val todayScheduled by viewModel.todayScheduledSupplements.collectAsState()
    val categories by viewModel.supplementCategories.collectAsState()
    val completed by viewModel.completedSupplements.collectAsState()
    val focusManager = LocalFocusManager.current

    var showManageDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<SupplementItem?>(null) }

    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete '$categoryToDelete'?\n(Items in this category will still exist but should be updated)") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(categoryToDelete!!)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("Cancel") }
            }
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { categoryToDelete = cat }
                                    )
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("New Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        viewModel.saveCategory(newCategoryName.trim())
                        newCategoryName = ""
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Close") } }
        )
    }

    if (editingItem != null) {
        var editName by remember { mutableStateOf(editingItem!!.name) }
        var editDosage by remember { mutableStateOf(editingItem!!.dosage) }
        var editCategory by remember { mutableStateOf(editingItem!!.category.ifBlank { categories.firstOrNull() ?: "Supplement" }) }
        var editTime by remember { mutableStateOf(editingItem!!.timeOfUse.ifBlank { "Anytime" }) }
        var editRecurrenceType by remember { mutableStateOf(editingItem!!.recurrenceType.ifBlank { "DAILY" }) }
        var editRecurrenceVal by remember { mutableStateOf(editingItem!!.recurrenceValue) }
        var editInstruction by remember { mutableStateOf(editingItem!!.instruction.ifBlank { "None" }) }

        var showSuggestions by remember { mutableStateOf(false) }
        var catExpanded by remember { mutableStateOf(false) }
        var timeExpanded by remember { mutableStateOf(false) }
        var instExpanded by remember { mutableStateOf(false) }

        val allOptions = remember(supplementTemplates, roster) {
            (supplementTemplates + roster).distinctBy { it.name.lowercase() }
        }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text(if (editingItem!!.id.isEmpty()) "Add Item" else "Edit Item") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it; showSuggestions = true },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth().onFocusChanged {
                            if (it.isFocused) showSuggestions = true
                        }
                    )

                    val filtered = if (editName.isBlank()) allOptions else allOptions.filter { it.name.contains(editName, ignoreCase = true) }

                    if (filtered.isNotEmpty() && showSuggestions) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filtered, key = { it.name }) { template ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                editName = template.name; editDosage = template.dosage; editCategory = template.category
                                                editTime = template.timeOfUse; editInstruction = template.instruction
                                                showSuggestions = false
                                                focusManager.clearFocus()
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(template.name, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(value = editDosage, onValueChange = { editDosage = it }, label = { Text("Dosage (e.g. 500mg)") }, modifier = Modifier.fillMaxWidth())

                    ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                        OutlinedTextField(value = editCategory, onValueChange = {}, readOnly = true, label = { Text("Category") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) })
                        ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                            categories.forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { editCategory = opt; catExpanded = false }) }
                        }
                    }

                    ExposedDropdownMenuBox(expanded = timeExpanded, onExpandedChange = { timeExpanded = it }) {
                        OutlinedTextField(value = editTime, onValueChange = {}, readOnly = true, label = { Text("Time of Use") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded) })
                        ExposedDropdownMenu(expanded = timeExpanded, onDismissRequest = { timeExpanded = false }) {
                            listOf("Morning", "Afternoon", "Evening", "Before Bed", "Anytime").forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { editTime = opt; timeExpanded = false }) }
                        }
                    }

                    ExposedDropdownMenuBox(expanded = instExpanded, onExpandedChange = { instExpanded = it }) {
                        OutlinedTextField(value = editInstruction, onValueChange = {}, readOnly = true, label = { Text("Instructions") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = instExpanded) })
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
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable {
                                    if (isSel) selectedDays.remove(day) else selectedDays.add(day)
                                    editRecurrenceVal = selectedDays.joinToString(",")
                                }, contentAlignment = Alignment.Center) {
                                    Text(day.take(1), color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (editRecurrenceType == "INTERVAL") {
                        OutlinedTextField(value = editRecurrenceVal, onValueChange = { editRecurrenceVal = it }, label = { Text("Every X Days (e.g. 2)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val idToSave = editingItem!!.id.ifEmpty { java.util.UUID.randomUUID().toString() }
                    // --- FIX: Preserve isActive state when saving ---
                    viewModel.saveSupplementItem(SupplementItem(idToSave, editName, editDosage, editCategory, editTime, editRecurrenceType, editRecurrenceVal, editInstruction, isActive = editingItem?.isActive ?: true))
                    editingItem = null
                }) { Text("Save") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingItem!!.id.isNotEmpty()) {
                        TextButton(onClick = {
                            viewModel.deleteSupplementItem(editingItem!!.id)
                            editingItem = null
                        }) { Text("Delete", color = Color(0xFFE53935)) }
                    }
                    TextButton(onClick = { editingItem = null }) { Text("Cancel") }
                }
            }
        )
    }

    // --- FIX: The Manage Dialog now shows ALL master templates with Toggle Switches ---
    if (showManageDialog && editingItem == null) {
        val allOptions = remember(supplementTemplates, roster) {
            (supplementTemplates + roster).distinctBy { it.name.lowercase() }
        }

        AlertDialog(
            onDismissRequest = { showManageDialog = false },
            title = { Text("Manage Your Stack", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Turn on the items you want to see in your daily schedule.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        val groupedRoster = allOptions.groupBy { it.category }
                        groupedRoster.forEach { (cat, items) ->
                            item {
                                Text(cat, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            }
                            items(items, key = { it.name }) { item ->
                                val isItemActive = roster.find { it.name.equals(item.name, ignoreCase = true) }?.isActive == true

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f).clickable { editingItem = item }
                                    ) {
                                        Text(item.name, fontWeight = FontWeight.Bold)
                                        Text("${item.dosage} • ${if(item.recurrenceType=="DAILY") "Daily" else "Scheduled"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = isItemActive,
                                        onCheckedChange = {
                                            val existingItem = roster.find { it.name.equals(item.name, ignoreCase = true) } ?: item
                                            viewModel.toggleSupplementActive(existingItem)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showManageDialog = false }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { showCategoryDialog = true }) { Text("Categories") } }
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Supplements & Vitamins", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { editingItem = SupplementItem("", "", "") }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Add New", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.2f)))
                        IconButton(onClick = { showManageDialog = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Manage", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (todayScheduled.isEmpty()) {
                Text("Nothing scheduled for today. Tap the edit icon to setup your stack.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val progress = completed.count { id -> todayScheduled.any { it.id == id } }.toFloat() / todayScheduled.size.toFloat()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily Progress", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                GaplessProgressBar(progress = progress, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().height(6.dp))
                Spacer(modifier = Modifier.height(16.dp))

                val groupedToday = todayScheduled.groupBy { it.category }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    groupedToday.forEach { (category, items) ->
                        Column {
                            Text(category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items.forEach { item ->
                                    key(item.id) {
                                        val isDone = completed.contains(item.id)

                                        val dismissState = rememberSwipeToDismissBoxState(
                                            confirmValueChange = { value ->
                                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                                    viewModel.deleteSupplementItem(item.id)
                                                    true
                                                } else false
                                            }
                                        )

                                        SwipeToDismissBox(
                                            state = dismissState,
                                            enableDismissFromStartToEnd = false,
                                            backgroundContent = {
                                                val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart || dismissState.currentValue == SwipeToDismissBoxValue.EndToStart
                                                val color by animateColorAsState(targetValue = if (isSwiping) Color(0xFFE53935) else Color.Transparent)

                                                Box(modifier = Modifier.fillMaxSize().padding(vertical = 2.dp).clip(RoundedCornerShape(12.dp)).background(color).padding(end = 16.dp), contentAlignment = Alignment.CenterEnd) {
                                                    if (isSwiping) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                                                    }
                                                }
                                            },
                                            content = {
                                                val cardBgColor by animateColorAsState(targetValue = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                                val textColor by animateColorAsState(targetValue = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface)

                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .combinedClickable(
                                                            onClick = { viewModel.toggleSupplement(item.id) },
                                                            onLongClick = { editingItem = item }
                                                        ),
                                                    color = cardBgColor
                                                ) {
                                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector = if (isDone) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                                                                    contentDescription = null,
                                                                    tint = if (isDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                                Spacer(modifier = Modifier.width(12.dp))
                                                                Text(
                                                                    text = item.name,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = textColor,
                                                                    textDecoration = if (isDone) TextDecoration.LineThrough else null
                                                                )
                                                            }
                                                            Text(
                                                                text = item.dosage,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }

                                                        if (item.timeOfUse != "Anytime" || item.instruction != "None") {
                                                            Row(modifier = Modifier.padding(top = 8.dp, start = 36.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                if (item.timeOfUse != "Anytime") {
                                                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDone) 0.2f else 0.5f), shape = RoundedCornerShape(4.dp)) {
                                                                        Text(item.timeOfUse, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = if (isDone) 0.5f else 1f))
                                                                    }
                                                                }
                                                                if (item.instruction != "None") {
                                                                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isDone) 0.2f else 0.5f), shape = RoundedCornerShape(4.dp)) {
                                                                        Text(item.instruction, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = if (isDone) 0.5f else 1f))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        )
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