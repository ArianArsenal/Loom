package com.example.loom.ui.health

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Sanitizer
import androidx.compose.material.icons.outlined.SportsBar
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WineBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.local.entity.WaterContainer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

// Predefined modern icon map
val containerIcons = mapOf(
    "LocalDrink" to Icons.Outlined.LocalDrink,
    "WaterDrop" to Icons.Outlined.WaterDrop,
    "LocalCafe" to Icons.Outlined.LocalCafe,
    "Coffee" to Icons.Outlined.Coffee,
    "EmojiFoodBeverage" to Icons.Outlined.EmojiFoodBeverage,
    "SportsBar" to Icons.Outlined.SportsBar,
    "WineBar" to Icons.Outlined.WineBar,
    "Sanitizer" to Icons.Outlined.Sanitizer
)

fun getContainerIcon(key: String): ImageVector {
    return containerIcons[key] ?: Icons.Outlined.LocalDrink
}

// NEW: Custom Animated Flowing Water Progress Bar
@Composable
fun AnimatedWaterProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    // 1. Smoothly animate the physical width of the progress bar when logging
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "WaterProgressHorizontal"
    )

    // 2. Create a continuous horizontal flowing gradient effect
    val infiniteTransition = rememberInfiniteTransition(label = "flow")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowingOffset"
    )

    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val waterColor = MaterialTheme.colorScheme.primary
    val waterHighlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(waterColor, waterHighlight, waterColor),
                        start = Offset(offsetX, 0f),
                        end = Offset(offsetX + 500f, 0f)
                    )
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthHydration(viewModel: HealthViewModel) {
    val todayRecord by viewModel.todayRecord.collectAsState()
    val dailyGoal by viewModel.waterDailyGoal.collectAsState()
    val isAutoGoal by viewModel.isAutoWaterGoal.collectAsState()
    val defaultIncrement by viewModel.defaultWaterIncrement.collectAsState()
    val customContainers by viewModel.customWaterContainers.collectAsState()
    val logs by viewModel.waterLogs.collectAsState()

    var showGoalDialog by remember { mutableStateOf(false) }
    var showContainerDialog by remember { mutableStateOf(false) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    if (showGoalDialog) {
        var inputGoal by remember { mutableStateOf(dailyGoal.toString()) }
        var autoToggled by remember { mutableStateOf(isAutoGoal) }

        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Hydration Goal") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Auto-Calculate (Weight × 35ml)")
                        Switch(checked = autoToggled, onCheckedChange = { autoToggled = it })
                    }
                    if (!autoToggled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = inputGoal, onValueChange = { inputGoal = it },
                            label = { Text("Daily Target (ml)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val finalGoal = if (autoToggled) (viewModel.weight.value * 35).toInt() else inputGoal.toIntOrNull() ?: 3000
                    viewModel.saveWaterGoal(finalGoal, autoToggled)
                    showGoalDialog = false
                }) { Text("Save Goal") }
            },
            dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") } }
        )
    }

    if (showContainerDialog) {
        var localContainers by remember { mutableStateOf(customContainers) }
        var showNewContainer by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showContainerDialog = false },
            title = { Text("Manage Containers") },
            text = {
                LazyColumn {
                    items(localContainers) { container ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(getContainerIcon(container.iconEmoji), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(container.name, fontWeight = FontWeight.Bold)
                                    Text("${container.volumeMl}ml • x${container.hydrationFactor} Hydration", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = { localContainers = localContainers.filter { it.id != container.id } }) {
                                Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (showNewContainer) {
                        item {
                            var newName by remember { mutableStateOf("") }
                            var newIconKey by remember { mutableStateOf("LocalDrink") }
                            var newVol by remember { mutableStateOf("") }
                            var newFactor by remember { mutableStateOf("1.0") }

                            Column(modifier = Modifier.padding(top = 16.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(12.dp)).padding(12.dp)) {

                                Text("Select Icon", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(containerIcons.keys.toList()) { key ->
                                        IconButton(
                                            onClick = { newIconKey = key },
                                            modifier = Modifier.background(if (newIconKey == key) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, CircleShape)
                                        ) {
                                            Icon(getContainerIcon(key), contentDescription = key, tint = if (newIconKey == key) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Container Name") }, modifier = Modifier.fillMaxWidth())

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = newVol, onValueChange = { newVol = it }, label = { Text("ML") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.5f))
                                    OutlinedTextField(value = newFactor, onValueChange = { newFactor = it }, label = { Text("Multiplier") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.5f))
                                }

                                Button(onClick = {
                                    val vol = newVol.toIntOrNull() ?: 250
                                    val fac = newFactor.toFloatOrNull() ?: 1.0f
                                    if (newName.isNotBlank()) {
                                        localContainers = localContainers + WaterContainer(UUID.randomUUID().toString(), newName, newIconKey, vol, fac)
                                        showNewContainer = false
                                    }
                                }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Add Container") }
                            }
                        }
                    } else {
                        item {
                            TextButton(onClick = { showNewContainer = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Add Container") }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveCustomWaterContainers(localContainers)
                    showContainerDialog = false
                }) { Text("Save Customizations") }
            },
            dismissButton = { TextButton(onClick = { showContainerDialog = false }) { Text("Cancel") } }
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header & Goal Progress
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Hydration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { showGoalDialog = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Edit, "Edit Goal", tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            val currentIntake = todayRecord?.waterIntakeMl ?: 0
            val progress = (currentIntake.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$currentIntake ml", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Goal: $dailyGoal ml", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Replaced GaplessProgressBar with our new Animated Water Progress Bar
            AnimatedWaterProgressBar(progress = progress, modifier = Modifier.fillMaxWidth().height(14.dp))

            Spacer(modifier = Modifier.height(24.dp))

            // Container Quick Select
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Hydration Quick-Add", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { showContainerDialog = true }) { Text("Edit Setup") }
            }

            // Selection Row
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                customContainers.forEach { container ->
                    val isDefault = defaultIncrement == container.volumeMl
                    Surface(
                        modifier = Modifier.weight(1f).clickable {
                            viewModel.saveDefaultWaterIncrement(container.volumeMl)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(modifier = Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${container.volumeMl}ml", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDefault) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(container.name, style = MaterialTheme.typography.labelSmall, color = if (isDefault) MaterialTheme.colorScheme.onPrimary.copy(alpha=0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Dedicated Log Button
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val selectedContainer = customContainers.find { it.volumeMl == defaultIncrement } ?: customContainers.firstOrNull()
                    if (selectedContainer != null) {
                        viewModel.logDetailedWater(selectedContainer)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Water")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log ${defaultIncrement}ml", fontWeight = FontWeight.Bold)
            }

            // Timeline History
            if (logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Today's Logs", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                    logs.sortedByDescending { it.timestamp }.forEach { log ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(getContainerIcon(log.emoji), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(log.containerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(timeFormatter.format(log.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("+${log.amountMl}ml", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { viewModel.removeDetailedWaterLog(log.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}