package com.example.loom.ui.health

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.loom.data.local.entity.SkincareProduct
import com.example.loom.data.local.entity.SkincareRoutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HealthSkincare(viewModel: HealthViewModel) {
    val skincareRoutines by viewModel.allSkincareRoutines.collectAsState()
    val skincareProducts by viewModel.allSkincareProducts.collectAsState()
    val completedRoutines by viewModel.completedSkincareRoutines.collectAsState()
    val completedSteps by viewModel.completedSkincareSteps.collectAsState()

    var showSkincareDialog by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<SkincareRoutine?>(null) }
    var productToDelete by remember { mutableStateOf<SkincareProduct?>(null) }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product?") },
            text = { Text("Are you sure you want to permanently delete '${productToDelete?.name}' from your global library?") },
            confirmButton = {
                Button(onClick = { viewModel.deleteSkincareProduct(productToDelete!!); productToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { productToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showSkincareDialog) {
        var inputRoutineName by remember(routineToEdit) { mutableStateOf(routineToEdit?.name ?: "") }
        var isPinnedToHome by remember(routineToEdit) { mutableStateOf(routineToEdit?.isPinnedToHome ?: true) }
        val initialProducts = try { Json.decodeFromString<List<String>>(routineToEdit?.productsJson ?: "[]") } catch(e: Exception){ emptyList() }
        var selectedProducts by remember(routineToEdit) { mutableStateOf(initialProducts) }

        var routineDropdownExpanded by remember { mutableStateOf(false) }
        var productSearchText by remember { mutableStateOf("") }
        var productDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSkincareDialog = false; routineToEdit = null },
            title = { Text(if (routineToEdit == null) "New Skincare Routine" else "Edit Routine") },
            text = {
                LazyColumn {
                    item {
                        ExposedDropdownMenuBox(expanded = routineDropdownExpanded, onExpandedChange = { routineDropdownExpanded = it }) {
                            OutlinedTextField(
                                value = inputRoutineName, onValueChange = { inputRoutineName = it; routineDropdownExpanded = true },
                                label = { Text("Routine Name (e.g., Morning Wash)") }, modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routineDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(), singleLine = true
                            )
                            val filteredRoutines = skincareRoutines.filter { it.name.contains(inputRoutineName, true) }
                            if (filteredRoutines.isNotEmpty() && routineDropdownExpanded) {
                                ExposedDropdownMenu(expanded = routineDropdownExpanded, onDismissRequest = { routineDropdownExpanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                                    filteredRoutines.forEach { routine ->
                                        DropdownMenuItem(text = { Text(routine.name) }, onClick = { routineToEdit = routine; routineDropdownExpanded = false })
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = isPinnedToHome, onCheckedChange = { isPinnedToHome = it }, modifier = Modifier.scale(0.8f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pin to Home Screen", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Products (Application Order):", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        ReorderableProductList(
                            products = selectedProducts,
                            onReorder = { newOrder -> selectedProducts = newOrder },
                            onRemove = { removedProduct -> selectedProducts = selectedProducts.filter { it != removedProduct } }
                        )

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExposedDropdownMenuBox(expanded = productDropdownExpanded, onExpandedChange = { productDropdownExpanded = it }, modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = productSearchText, onValueChange = { productSearchText = it; productDropdownExpanded = true },
                                    placeholder = { Text("Search or add...") }, modifier = Modifier.menuAnchor().fillMaxWidth(), singleLine = true
                                )
                                val filteredProducts = skincareProducts.filter { it.name.contains(productSearchText, true) && !selectedProducts.contains(it.name) }
                                if (filteredProducts.isNotEmpty() && productDropdownExpanded) {
                                    ExposedDropdownMenu(expanded = productDropdownExpanded, onDismissRequest = { productDropdownExpanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                                        filteredProducts.forEach { product ->
                                            Box(modifier = Modifier.fillMaxWidth().combinedClickable(
                                                onClick = { selectedProducts = selectedProducts + product.name; productSearchText = ""; productDropdownExpanded = false },
                                                onLongClick = { productToDelete = product }
                                            ).padding(16.dp)) { Text(product.name) }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                val trimmed = productSearchText.trim()
                                if (trimmed.isNotBlank() && !selectedProducts.contains(trimmed)) {
                                    viewModel.addSkincareProduct(trimmed)
                                    selectedProducts = selectedProducts + trimmed
                                    productSearchText = ""
                                    productDropdownExpanded = false
                                }
                            }) { Icon(Icons.Default.Add, "Add Product", tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (inputRoutineName.isNotBlank()) {
                        viewModel.saveSkincareRoutine(SkincareRoutine(id = routineToEdit?.id ?: 0, name = inputRoutineName, isPinnedToHome = isPinnedToHome, productsJson = Json.encodeToString(selectedProducts)))
                        showSkincareDialog = false; routineToEdit = null
                    }
                }) { Text("Save Routine") }
            },
            dismissButton = {
                Row {
                    if (routineToEdit != null) {
                        TextButton(onClick = { viewModel.deleteSkincareRoutine(routineToEdit!!); showSkincareDialog = false; routineToEdit = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                    }
                    TextButton(onClick = { showSkincareDialog = false; routineToEdit = null }) { Text("Cancel") }
                }
            }
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Skincare Routines", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { routineToEdit = null; showSkincareDialog = true }) { Text("+ Create") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (skincareRoutines.isEmpty()) {
                Text("No skincare routines created.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                skincareRoutines.forEach { routine ->
                    val isRoutineDone = completedRoutines.contains(routine.name)
                    val products = remember(routine.productsJson) { try { Json.decodeFromString<List<String>>(routine.productsJson) } catch (e: Exception) { emptyList() } }

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = isRoutineDone, onCheckedChange = { viewModel.toggleSkincareRoutine(routine.name) })
                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(routine.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, textDecoration = if (isRoutineDone) TextDecoration.LineThrough else TextDecoration.None)
                                if (routine.isPinnedToHome) {
                                    Text("Pinned to Home Screen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                            IconButton(onClick = { routineToEdit = routine; showSkincareDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Routine", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (products.isNotEmpty()) {
                            Column(modifier = Modifier.padding(top = 4.dp, start = 48.dp)) {
                                products.forEach { product ->
                                    val stepId = "${routine.name}_${product}"
                                    val isStepDone = completedSteps.contains(stepId)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleSkincareStep(routine.name, product) }
                                            .padding(vertical = 4.dp)
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