package com.example.loom.ui.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthBio(viewModel: HealthViewModel) {
    val weight by viewModel.weight.collectAsState()
    val height by viewModel.height.collectAsState()
    val age by viewModel.age.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val activity by viewModel.activity.collectAsState()
    val goal by viewModel.goal.collectAsState()

    var isEditingBio by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Bio Engine & Macros", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { isEditingBio = !isEditingBio }) { Icon(if (isEditingBio) Icons.Default.Close else Icons.Default.Edit, contentDescription = "Edit Bio") }
            }

            if (isEditingBio) {
                var editWeight by remember { mutableStateOf(weight.formatWeight()) }
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
                        OutlinedTextField(value = editWeight, onValueChange = { editWeight = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
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
                    Text("${weight.formatWeight()} kg | $height cm | $age yrs", style = MaterialTheme.typography.bodyLarge)
                    Text(goal, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}