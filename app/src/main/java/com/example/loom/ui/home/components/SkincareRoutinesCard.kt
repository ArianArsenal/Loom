package com.example.loom.ui.home.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.local.entity.SkincareProduct
import com.example.loom.data.local.entity.SkincareRoutine
import com.example.loom.ui.health.ProductProfileDialog
import com.example.loom.ui.home.HomeViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SkincareRoutinesCard(
    pinnedRoutines: List<SkincareRoutine>,
    completedRoutines: List<String>,
    completedSteps: List<String>,
    viewModel: HomeViewModel
) {
    var editingProductContext by remember { mutableStateOf<Pair<SkincareRoutine, SkincareProduct>?>(null) }

    if (editingProductContext != null) {
        val routineContext = editingProductContext!!.first
        val oldProduct = editingProductContext!!.second
        ProductProfileDialog(
            initialProduct = oldProduct,
            onDismiss = { editingProductContext = null },
            onSave = { updatedProduct ->
                val currentProducts = try { Json.decodeFromString<List<SkincareProduct>>(routineContext.productsJson) } catch (_: Exception) { emptyList() } // FIX: Unused 'e'
                val updatedList = currentProducts.map { if (it.name == oldProduct.name) updatedProduct else it }
                viewModel.saveSkincareRoutine(routineContext.copy(productsJson = Json.encodeToString(updatedList)))
                editingProductContext = null
            },
            onDelete = {
                val currentProducts = try { Json.decodeFromString<List<SkincareProduct>>(routineContext.productsJson) } catch (_: Exception) { emptyList() } // FIX: Unused 'e'
                val updatedList = currentProducts.filter { it.name != oldProduct.name }
                viewModel.saveSkincareRoutine(routineContext.copy(productsJson = Json.encodeToString(updatedList)))
                editingProductContext = null
            }
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Skincare Routines", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            val activelyLoggedRoutines = pinnedRoutines.filter { completedRoutines.contains(it.name) }

            if (activelyLoggedRoutines.isEmpty()) {
                Text("No routines logged and pinned for today. Add them in the Health tab!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                activelyLoggedRoutines.forEach { routine ->
                    val products = remember(routine.productsJson) {
                        try { Json.decodeFromString<List<SkincareProduct>>(routine.productsJson) }
                        catch (_: Exception) { // FIX: Unused 'e'
                            try { Json.decodeFromString<List<String>>(routine.productsJson).map { SkincareProduct(name = it) } }
                            catch (_: Exception) { emptyList() } // FIX: Unused 'e2'
                        }
                    }

                    val isRoutineFullyChecked = products.isNotEmpty() && products.all { completedSteps.contains("${routine.name}_${it.name}") }

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {

                            Checkbox(
                                checked = isRoutineFullyChecked,
                                onCheckedChange = { isChecked -> viewModel.toggleAllSkincareSteps(routine.name, products.map { it.name }, isChecked) }
                            )

                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(routine.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, textDecoration = if (isRoutineFullyChecked) TextDecoration.LineThrough else TextDecoration.None)
                                if (routine.isPinnedToHome) {
                                    Text("Pinned to Home Screen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }

                        if (products.isNotEmpty()) {
                            Column(modifier = Modifier.padding(top = 4.dp, start = 48.dp)) {
                                products.forEach { product ->
                                    val stepId = "${routine.name}_${product.name}"
                                    val isStepDone = completedSteps.contains(stepId)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = { viewModel.toggleSkincareStep(routine.name, product.name) },
                                                onLongClick = { editingProductContext = Pair(routine, product) }
                                            )
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Icon(if (isStepDone) Icons.Outlined.CheckCircle else Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text("${product.timeOfUseEmoji} ", fontSize = 14.sp)

                                        Text(product.name, style = MaterialTheme.typography.bodySmall.copy(textDecoration = if (isStepDone) TextDecoration.LineThrough else TextDecoration.None), color = if(isStepDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
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