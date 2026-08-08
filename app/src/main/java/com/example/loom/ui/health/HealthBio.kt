package com.example.loom.ui.health

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun SegmentedControl(options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(4.dp)) {
        options.forEach { opt ->
            val isSelected = selectedOption == opt
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { onOptionSelected(opt) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text(text = opt, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
fun MacroBadge(emoji: String, amount: String, label: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(amount, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HealthBio(viewModel: HealthViewModel) {
    val weight by viewModel.weight.collectAsState()
    val height by viewModel.height.collectAsState()
    val age by viewModel.age.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val activity by viewModel.activity.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val bodyFat by viewModel.bodyFat.collectAsState()
    val dietType by viewModel.dietType.collectAsState()
    val phaseWeeks by viewModel.phaseWeeks.collectAsState()
    val phaseStart by viewModel.phaseStartTime.collectAsState()
    val isCycling by viewModel.calorieCycling.collectAsState()

    val targetCalories by viewModel.targetCalories.collectAsState()
    val targetProtein by viewModel.targetProtein.collectAsState()
    val targetCarbs by viewModel.targetCarbs.collectAsState()
    val targetFats by viewModel.targetFats.collectAsState()

    val todayRecord by viewModel.todayRecord.collectAsState()

    var isEditingBio by remember { mutableStateOf(false) }
    var showEnergyInfoDialog by remember { mutableStateOf(false) }
    var viewCyclingTab by remember { mutableStateOf("Train") } // "Train" or "Rest"

    if (showEnergyInfoDialog) {
        AlertDialog(
            onDismissRequest = { showEnergyInfoDialog = false },
            title = { Text("Energy Stats Explained", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("BMR (Basal Metabolic Rate)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Calories burned at rest. Uses Katch-McArdle if Body Fat % is provided.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("TDEE (Total Energy Expenditure)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Your BMR multiplied by your daily activity level.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = { showEnergyInfoDialog = false }) { Text("Got it") } }
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Bio Engine & Macros", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { isEditingBio = !isEditingBio }) { Icon(if (isEditingBio) Icons.Default.Close else Icons.Default.Edit, contentDescription = "Toggle Edit") }
            }

            AnimatedContent(targetState = isEditingBio, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "BioEditTransition") { editing ->
                if (editing) {
                    var editWeight by remember { mutableStateOf(weight.formatWeight()) }
                    var editHeight by remember { mutableStateOf(height.toString()) }
                    var editAge by remember { mutableStateOf(age.toString()) }
                    var editGender by remember { mutableStateOf(gender) }
                    var editActivity by remember { mutableStateOf(activity) }
                    var editGoal by remember { mutableStateOf(goal) }
                    var editBodyFat by remember { mutableStateOf(if (bodyFat > 0f) bodyFat.toString() else "") }
                    var editDietType by remember { mutableStateOf(dietType) }
                    var editPhaseWeeks by remember { mutableStateOf(phaseWeeks.toString()) }
                    var editCycling by remember { mutableStateOf(isCycling) }

                    var activityExpanded by remember { mutableStateOf(false) }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = editWeight, onValueChange = { editWeight = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = editHeight, onValueChange = { editHeight = it }, label = { Text("Height (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = editAge, onValueChange = { editAge = it }, label = { Text("Age") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = editBodyFat, onValueChange = { editBodyFat = it }, label = { Text("Body Fat % (Opt)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = editPhaseWeeks, onValueChange = { editPhaseWeeks = it }, label = { Text("Phase Length (Wks)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Gender", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                        SegmentedControl(options = listOf("Male", "Female"), selectedOption = editGender, onOptionSelected = { editGender = it })

                        Spacer(modifier = Modifier.height(16.dp))
                        ExposedDropdownMenuBox(expanded = activityExpanded, onExpandedChange = { activityExpanded = it }) {
                            OutlinedTextField(value = editActivity, onValueChange = {}, readOnly = true, label = { Text("Activity Level") }, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) })
                            ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }) {
                                listOf("Sedentary", "Light", "Moderate", "Active", "Very Active").forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { editActivity = opt; activityExpanded = false }) }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fitness Goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                        SegmentedControl(options = listOf("Lose Weight", "Maintain", "Build Muscle"), selectedOption = editGoal, onOptionSelected = { editGoal = it })

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Diet Split", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                        SegmentedControl(options = listOf("Balanced", "High Protein", "Low Carb", "Keto"), selectedOption = editDietType, onOptionSelected = { editDietType = it })

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Calorie Cycling", fontWeight = FontWeight.Bold)
                                Text("Split macros for Train vs. Rest days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = editCycling, onCheckedChange = { editCycling = it })
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            viewModel.saveBioMetrics(editWeight.toFloatOrNull() ?: 70f, editHeight.toIntOrNull() ?: 175, editAge.toIntOrNull() ?: 21, editGender, editActivity, editGoal, editBodyFat.toFloatOrNull() ?: 0f, editDietType, editPhaseWeeks.toIntOrNull() ?: 12, editCycling)
                            isEditingBio = false
                        }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 14.dp), shape = RoundedCornerShape(12.dp)) { Text("Calculate & Save Targets", fontWeight = FontWeight.Bold) }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        // Phase Tracking
                        val daysElapsed = ((System.currentTimeMillis() - phaseStart) / (1000 * 60 * 60 * 24)).toInt()
                        val currentWeek = (daysElapsed / 7) + 1
                        val phaseProgress = (daysElapsed.toFloat() / (phaseWeeks * 7).toFloat()).coerceIn(0f, 1f)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Phase Progress", style = MaterialTheme.typography.labelMedium)
                            Text("Week $currentWeek of $phaseWeeks", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        GaplessProgressBar(progress = phaseProgress, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().height(6.dp))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Base Stats
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${weight.formatWeight()} kg | $height cm | $age yrs", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

                            val heightM = height / 100f
                            val bmi = if (heightM > 0) weight / (heightM * heightM) else 0f
                            val bmiFormatted = String.format(Locale.US, "%.1f", bmi)
                            val (bmiLabel, bmiColor) = when { bmi < 18.5 -> "Underweight" to Color(0xFF2196F3); bmi < 25 -> "Normal" to Color(0xFF4CAF50); bmi < 30 -> "Overweight" to Color(0xFFFF9800); else -> "Obese" to Color(0xFFF44336) }

                            Surface(color = bmiColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                                Text("BMI $bmiFormatted ($bmiLabel)", color = bmiColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Macros & Calorie Cycling
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Daily Targets", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            if (isCycling) {
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Row {
                                        Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(if(viewCyclingTab=="Train") MaterialTheme.colorScheme.primary else Color.Transparent).clickable{ viewCyclingTab="Train" }.padding(horizontal=12.dp, vertical=4.dp)) { Text("Train", color = if(viewCyclingTab=="Train") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style=MaterialTheme.typography.labelSmall) }
                                        Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(if(viewCyclingTab=="Rest") MaterialTheme.colorScheme.primary else Color.Transparent).clickable{ viewCyclingTab="Rest" }.padding(horizontal=12.dp, vertical=4.dp)) { Text("Rest", color = if(viewCyclingTab=="Rest") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style=MaterialTheme.typography.labelSmall) }
                                    }
                                }
                            } else {
                                Text(goal, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Cycling Math Adjustments
                        val displayCals = if(isCycling && viewCyclingTab=="Train") (targetCalories * 1.1f).toInt() else if(isCycling) (targetCalories * 0.9f).toInt() else targetCalories
                        val displayCarbs = if(isCycling && viewCyclingTab=="Train") (targetCarbs * 1.2f).toInt() else if(isCycling) (targetCarbs * 0.8f).toInt() else targetCarbs

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MacroBadge(emoji = "🔥", amount = "$displayCals", label = "kcal")
                            MacroBadge(emoji = "🥩", amount = "${targetProtein}g", label = "Pro")
                            MacroBadge(emoji = "🍞", amount = "${displayCarbs}g", label = "Carb")
                            MacroBadge(emoji = "🥑", amount = "${targetFats}g", label = "Fat")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Energy Stats Footer
                        val bmr = if (bodyFat > 0f) {
                            370f + (21.6f * (weight * (1f - (bodyFat / 100f))))
                        } else if (gender == "Male") {
                            (10f * weight) + (6.25f * height) - (5f * age) + 5f
                        } else {
                            (10f * weight) + (6.25f * height) - (5f * age) - 161f
                        }
                        val multiplier = when (activity) { "Sedentary" -> 1.2f; "Light" -> 1.375f; "Moderate" -> 1.55f; "Active" -> 1.725f; "Very Active" -> 1.9f; else -> 1.2f }
                        val tdee = (bmr * multiplier).toInt()

                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { showEnergyInfoDialog = true }.padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, contentDescription = "Info", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resting BMR: ${bmr.toInt()} kcal   •   Active TDEE: $tdee kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Daily Weight Logger
                        var showWeightLogDialog by remember { mutableStateOf(false) }

                        if (showWeightLogDialog) {
                            // Start the dialog at today's logged weight, OR the base weight if nothing is logged yet
                            var sliderWeight by remember { mutableFloatStateOf(if ((todayRecord?.weightLogKg ?: 0f) > 0f) todayRecord!!.weightLogKg else weight) }

                            // Sets the initial scroll position so the active weight is visible immediately
                            val listState = rememberLazyListState(initialFirstVisibleItemIndex = (sliderWeight.toInt() - 20).coerceAtLeast(0))

                            AlertDialog(
                                onDismissRequest = { showWeightLogDialog = false },
                                title = { Text("Log Today's Weight", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", sliderWeight)} kg",
                                            style = MaterialTheme.typography.displayMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        // Horizontal Scrollable Number Selection (Whole KGs)
                                        LazyRow(
                                            state = listState,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            contentPadding = PaddingValues(horizontal = 60.dp) // Gives room to scroll smoothly to the edges
                                        ) {
                                            val minWeight = 20
                                            val maxWeight = 250
                                            items(maxWeight - minWeight + 1) { index ->
                                                val itemWeight = minWeight + index
                                                val isSelected = itemWeight == sliderWeight.toInt()

                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable {
                                                            // Keep the exact decimal when a new whole number is tapped
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
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                    )
                                                }
                                            }
                                        }

                                        // Precision Controls
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
                                    }) { Text("Save Log") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showWeightLogDialog = false }) { Text("Cancel") }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        val loggedWeight = todayRecord?.weightLogKg ?: 0f
                        val isWeightLoggedToday = loggedWeight > 0f

                        if (isWeightLoggedToday) {
                            val weightDiff = loggedWeight - weight
                            val diffText = when {
                                weightDiff < 0f -> "⬇ ${String.format(Locale.US, "%.1f", -weightDiff)} kg"
                                weightDiff > 0f -> "⬆ ${String.format(Locale.US, "%.1f", weightDiff)} kg"
                                else -> "⚖️ No change"
                            }

                            val diffColor = when {
                                weightDiff < 0f -> if (goal == "Lose Weight") Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                weightDiff > 0f -> if (goal == "Build Muscle") Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                onClick = { showWeightLogDialog = true } // <-- Lets you edit today's weight by tapping the card!
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Today: ${loggedWeight.formatWeight()} kg", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteDailyWeight() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete Weight", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Change from base weight:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text(diffText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = diffColor)
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showWeightLogDialog = true }, // <-- Now opens the precision dialog instead of auto-logging!
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Log Weight Today", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}