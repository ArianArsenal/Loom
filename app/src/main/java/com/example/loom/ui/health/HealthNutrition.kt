package com.example.loom.ui.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.local.entity.ConsumedFoodLog
import com.example.loom.data.local.entity.FoodItem
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HealthNutrition(viewModel: HealthViewModel) {
    val todayRecord by viewModel.todayRecord.collectAsState()
    val consumedList by viewModel.consumedFoods.collectAsState()
    val targetProteinInt by viewModel.targetProtein.collectAsState()
    val targetCarbsInt by viewModel.targetCarbs.collectAsState()
    val targetFatsInt by viewModel.targetFats.collectAsState()

    var showFoodDialog by remember { mutableStateOf(false) }
    var showFoodPantryDialog by remember { mutableStateOf(false) }
    var foodToLog by remember { mutableStateOf<FoodItem?>(null) }
    var showNewFoodDialog by remember { mutableStateOf(false) }
    var foodToDelete by remember { mutableStateOf<FoodItem?>(null) }
    var logDetailsToShow by remember { mutableStateOf<ConsumedFoodLog?>(null) }

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

    if (logDetailsToShow != null) {
        val log = logDetailsToShow!!
        val timeStr = timelineDateFormatter.format(Date(log.timestamp))
        AlertDialog(
            onDismissRequest = { logDetailsToShow = null },
            title = { Text("Meal Details") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(log.emoji, fontSize = 40.sp) }
                    }
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
                        val totalNum = baseNum * log.servings
                        calculatedNumStr = totalNum.formatWeight()
                        finalUnitText = uText.replaceFirst(mResult.value, "").trim()
                        showMultiplier = false
                    } else {
                        calculatedNumStr = ""
                        finalUnitText = uText
                        showMultiplier = true
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (finalUnitText.isNotBlank()) {
                            Text(finalUnitText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (showMultiplier) {
                            Text("${log.servings.formatWeight()}x", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(calculatedNumStr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("•  $timeStr", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${log.calories}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                            Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${log.protein}g", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), style = MaterialTheme.typography.titleMedium)
                            Text("Pro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${log.carbs}g", fontWeight = FontWeight.Bold, color = Color(0xFFFFA726), style = MaterialTheme.typography.titleMedium)
                            Text("Carbs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${log.fats}g", fontWeight = FontWeight.Bold, color = Color(0xFFE53935), style = MaterialTheme.typography.titleMedium)
                            Text("Fats", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { logDetailsToShow = null }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.removeConsumedFoodLog(log)
                    logDetailsToShow = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete Log")
                }
            }
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
                    OutlinedTextField(
                        value = servingsStr,
                        onValueChange = { servingsStr = it },
                        label = { Text("Multiplier (e.g., 2 for double, 0.5 for half)") },
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

    if (showFoodPantryDialog) {
        val currentSearchQuery by viewModel.foodSearchQuery.collectAsState()
        val searchedFoods by viewModel.searchedFoodItems.collectAsState()

        AlertDialog(
            onDismissRequest = { showFoodPantryDialog = false },
            title = { Text("Edit Pantry & Log Meals") },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentSearchQuery,
                        onValueChange = { viewModel.updateFoodSearchQuery(it) },
                        placeholder = { Text("Search foods to log/edit...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            TextButton(onClick = { showNewFoodDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("+ Create New Food")
                            }
                        }

                        items(searchedFoods) { food ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().combinedClickable(
                                    onClick = { foodToLog = food; showFoodDialog = true; showFoodPantryDialog = false },
                                    onLongClick = { foodToDelete = food }
                                )
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🍽️", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(food.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                                        Text(
                                            text = "P: ${food.protein}g • C: ${food.carbs}g • F: ${food.fats}g",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))

                                        val uText = food.servingUnit.trim()
                                        val mResult = "\\d+(\\.\\d+)?".toRegex().find(uText)
                                        val numStr = mResult?.value ?: ""
                                        val textStr = if (mResult != null) uText.replaceFirst(numStr, "").trim() else uText

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${food.calories} kcal",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

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
                        viewModel.addFoodItem(
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
            dismissButton = { TextButton(onClick = { showNewFoodDialog = false }) { Text("Cancel") } }
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Nutrition Log", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { showFoodPantryDialog = true }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("Pantry")
                }
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
                            drawArc(Color.LightGray.copy(alpha=0.3f), 0f, 360f, false, style = Stroke(width = strokeW, cap = StrokeCap.Round))
                        } else {
                            var startAngle = -90f
                            val proAngle = ((proGrams * 4f) / totalMacroCals) * 360f
                            val carbAngle = ((carbGrams * 4f) / totalMacroCals) * 360f
                            val fatAngle = ((fatGrams * 9f) / totalMacroCals) * 360f

                            if (proAngle > 0f) { drawArc(Color(0xFF4CAF50), startAngle, proAngle, false, style = Stroke(width = strokeW, cap = StrokeCap.Round)); startAngle += proAngle }
                            if (carbAngle > 0f) { drawArc(Color(0xFFFFA726), startAngle, carbAngle, false, style = Stroke(width = strokeW, cap = StrokeCap.Round)); startAngle += carbAngle }
                            if (fatAngle > 0f) { drawArc(Color(0xFFE53935), startAngle, fatAngle, false, style = Stroke(width = strokeW, cap = StrokeCap.Round)) }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$storedCals", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("kcal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Circle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("${proGrams}g / ${targetProteinInt}g Protein", style = MaterialTheme.typography.bodyMedium) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Circle, null, tint = Color(0xFFFFA726), modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("${carbGrams}g / ${targetCarbsInt}g Carbs", style = MaterialTheme.typography.bodyMedium) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Circle, null, tint = Color(0xFFE53935), modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("${fatGrams}g / ${targetFatsInt}g Fats", style = MaterialTheme.typography.bodyMedium) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Consumed Today", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

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
                            val timeStr = timelineDateFormatter.format(Date(log.timestamp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .combinedClickable(
                                        onClick = { logDetailsToShow = log },
                                        onLongClick = { logDetailsToShow = log }
                                    )
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(log.emoji, fontSize = 24.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val servingDisplay = log.servings.formatWeight()
                                    val unitDisplay = log.servingUnit.ifBlank { "serving" }

                                    val uText = unitDisplay.trim()
                                    val mResult = "\\d+(\\.\\d+)?".toRegex().find(uText)

                                    val calculatedNumStr: String
                                    val finalUnitText: String
                                    val showMultiplier: Boolean

                                    if (mResult != null) {
                                        val baseNum = mResult.value.toFloat()
                                        val totalNum = baseNum * log.servings
                                        calculatedNumStr = totalNum.formatWeight()
                                        finalUnitText = uText.replaceFirst(mResult.value, "").trim()
                                        showMultiplier = false
                                    } else {
                                        calculatedNumStr = ""
                                        finalUnitText = uText
                                        showMultiplier = true
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (finalUnitText.isNotBlank()) {
                                                    Text(finalUnitText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                if (showMultiplier) {
                                                    Text("${servingDisplay}x", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                } else {
                                                    Text(calculatedNumStr, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = log.foodName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
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