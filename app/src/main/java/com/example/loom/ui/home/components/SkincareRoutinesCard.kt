package com.example.loom.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.loom.data.local.entity.SkincareRoutine
import com.example.loom.ui.home.HomeViewModel
import kotlinx.serialization.json.Json

@Composable
fun SkincareRoutinesCard(pinnedRoutines: List<SkincareRoutine>, completedRoutines: List<String>, completedSteps: List<String>, viewModel: HomeViewModel) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Skincare Routines", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            if (pinnedRoutines.isEmpty()) {
                Text("No routines pinned yet. Create them in the Health tab!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                pinnedRoutines.forEach { routine ->
                    val isDone = completedRoutines.contains(routine.name)
                    val products = remember(routine.productsJson) { try { Json.decodeFromString<List<String>>(routine.productsJson) } catch (e: Exception) { emptyList() } }

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = isDone, onCheckedChange = { viewModel.toggleSkincareRoutine(routine.name) })
                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(routine.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None)
                                if (routine.isPinnedToHome) {
                                    Text("Pinned to Home Screen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }

                        if (products.isNotEmpty()) {
                            Column(modifier = Modifier.padding(top = 4.dp, start = 48.dp)) {
                                products.forEach { product ->
                                    val stepId = "${routine.name}_${product}"
                                    val isStepDone = completedSteps.contains(stepId)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleSkincareStep(routine.name, product) }.padding(vertical = 4.dp)
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