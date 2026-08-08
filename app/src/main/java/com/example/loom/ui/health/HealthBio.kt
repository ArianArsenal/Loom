package com.example.loom.ui.health

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
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
fun SegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        options.forEach { opt ->
            val isSelected = selectedOption == opt
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onOptionSelected(opt) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = opt,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun MacroBadge(emoji: String, amount: String, label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
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

    val targetCalories by viewModel.targetCalories.collectAsState()
    val targetProtein by viewModel.targetProtein.collectAsState()
    val targetCarbs by viewModel.targetCarbs.collectAsState()
    val targetFats by viewModel.targetFats.collectAsState()

    var isEditingBio by remember { mutableStateOf(false) }
    var showEnergyInfoDialog by remember { mutableStateOf(false) }

    if (showEnergyInfoDialog) {
        AlertDialog(
            onDismissRequest = { showEnergyInfoDialog = false },
            title = { Text("Energy Stats Explained", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("BMR (Basal Metabolic Rate)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("The number of calories your body burns at rest just to perform basic life-sustaining functions (like breathing and circulating blood).", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("TDEE (Total Daily Energy Expenditure)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("The total number of calories you burn in a day. It is calculated by taking your BMR and multiplying it by your daily physical activity level.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showEnergyInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bio Engine & Macros", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { isEditingBio = !isEditingBio }) {
                    Icon(if (isEditingBio) Icons.Default.Close else Icons.Default.Edit, contentDescription = "Toggle Edit")
                }
            }

            AnimatedContent(
                targetState = isEditingBio,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "BioEditTransition"
            ) { editing ->
                if (editing) {
                    var editWeight by remember { mutableStateOf(weight.formatWeight()) }
                    var editHeight by remember { mutableStateOf(height.toString()) }
                    var editAge by remember { mutableStateOf(age.toString()) }
                    var editGender by remember { mutableStateOf(gender) }
                    var editActivity by remember { mutableStateOf(activity) }
                    var editGoal by remember { mutableStateOf(goal) }

                    var activityExpanded by remember { mutableStateOf(false) }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = editWeight, onValueChange = { editWeight = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = editHeight, onValueChange = { editHeight = it }, label = { Text("Height (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = editAge, onValueChange = { editAge = it }, label = { Text("Age") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Gender", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                        SegmentedControl(
                            options = listOf("Male", "Female"),
                            selectedOption = editGender,
                            onOptionSelected = { editGender = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        ExposedDropdownMenuBox(expanded = activityExpanded, onExpandedChange = { activityExpanded = it }) {
                            OutlinedTextField(
                                value = editActivity, onValueChange = {}, readOnly = true,
                                label = { Text("Activity Level") }, modifier = Modifier.menuAnchor().fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) }
                            )
                            ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }) {
                                listOf("Sedentary", "Light", "Moderate", "Active", "Very Active").forEach { opt ->
                                    DropdownMenuItem(text = { Text(opt) }, onClick = { editActivity = opt; activityExpanded = false })
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fitness Goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                        SegmentedControl(
                            options = listOf("Lose Weight", "Maintain", "Build Muscle"),
                            selectedOption = editGoal,
                            onOptionSelected = { editGoal = it }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.saveBioMetrics(editWeight.toFloatOrNull() ?: 70f, editHeight.toIntOrNull() ?: 175, editAge.toIntOrNull() ?: 21, editGender, editActivity, editGoal)
                                isEditingBio = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Calculate & Save Targets", fontWeight = FontWeight.Bold) }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // BMI & Base Stats Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${weight.formatWeight()} kg | $height cm | $age yrs", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

                            val heightM = height / 100f
                            val bmi = if (heightM > 0) weight / (heightM * heightM) else 0f
                            val bmiFormatted = String.format(Locale.US, "%.1f", bmi)

                            val (bmiLabel, bmiColor) = when {
                                bmi < 18.5 -> "Underweight" to Color(0xFF2196F3) // Blue
                                bmi < 25 -> "Normal" to Color(0xFF4CAF50)      // Green
                                bmi < 30 -> "Overweight" to Color(0xFFFF9800)  // Orange
                                else -> "Obese" to Color(0xFFF44336)           // Red
                            }

                            Surface(
                                color = bmiColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "BMI $bmiFormatted ($bmiLabel)",
                                    color = bmiColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Target Macros Dashboard
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Daily Targets", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(goal, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MacroBadge(emoji = "🔥", amount = "$targetCalories", label = "kcal")
                            MacroBadge(emoji = "🥩", amount = "${targetProtein}g", label = "Pro")
                            MacroBadge(emoji = "🍞", amount = "${targetCarbs}g", label = "Carb")
                            MacroBadge(emoji = "🥑", amount = "${targetFats}g", label = "Fat")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Energy Stats Footer
                        val bmr = if (gender == "Male") { (10 * weight) + (6.25 * height) - (5 * age) + 5 } else { (10 * weight) + (6.25 * height) - (5 * age) - 161 }
                        val multiplier = when (activity) { "Sedentary" -> 1.2f; "Light" -> 1.375f; "Moderate" -> 1.55f; "Active" -> 1.725f; "Very Active" -> 1.9f; else -> 1.2f }
                        val tdee = (bmr * multiplier).toInt()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showEnergyInfoDialog = true }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Info",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Resting BMR: ${bmr.toInt()} kcal   •   Active TDEE: $tdee kcal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}