package com.example.loom.ui.health

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.local.entity.SkincareProduct
import com.example.loom.data.local.entity.SkincareRoutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HealthSkincare(viewModel: HealthViewModel) {
    val context = LocalContext.current
    val skincareRoutines by viewModel.allSkincareRoutines.collectAsState()
    val skincareProducts by viewModel.allSkincareProducts.collectAsState()
    val completedRoutines by viewModel.completedSkincareRoutines.collectAsState()
    val completedSteps by viewModel.completedSkincareSteps.collectAsState()

    var showSkincareDialog by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<SkincareRoutine?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var editingProductContext by remember { mutableStateOf<Pair<SkincareRoutine, SkincareProduct>?>(null) }

    if (editingProductContext != null) {
        val routineContext = editingProductContext!!.first
        val oldProduct = editingProductContext!!.second
        ProductProfileDialog(
            initialProduct = oldProduct,
            onDismiss = { editingProductContext = null },
            onSave = { updatedProduct ->
                val currentProducts = try { Json.decodeFromString<List<SkincareProduct>>(routineContext.productsJson) } catch (_: Exception) { emptyList() } // --- FIX ---
                val updatedList = currentProducts.map { if (it.name == oldProduct.name) updatedProduct else it }
                viewModel.saveSkincareRoutine(routineContext.copy(productsJson = Json.encodeToString(updatedList)))
                editingProductContext = null
            },
            onDelete = {
                val currentProducts = try { Json.decodeFromString<List<SkincareProduct>>(routineContext.productsJson) } catch (_: Exception) { emptyList() } // --- FIX ---
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
            text = { Text("This will permanently delete the '${routineToEdit?.name}' template. If you just want to remove it from today's list, swipe it away on the home screen instead.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteSkincareRoutine(routineToEdit!!)
                    showDeleteConfirmDialog = false
                    showSkincareDialog = false
                    routineToEdit = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete Template") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") } }
        )
    }

    if (showSkincareDialog) {
        var inputRoutineName by remember(routineToEdit) { mutableStateOf(routineToEdit?.name ?: "") }
        var isPinnedToHome by remember(routineToEdit) { mutableStateOf(routineToEdit?.isPinnedToHome ?: true) }

        val initialProducts = remember(routineToEdit) {
            try {
                Json.decodeFromString<List<SkincareProduct>>(routineToEdit?.productsJson ?: "[]")
            } catch(_: Exception){
                try {
                    Json.decodeFromString<List<String>>(routineToEdit?.productsJson ?: "[]").map { SkincareProduct(name = it) }
                } catch (_: Exception) { emptyList() } // --- FIX ---
            }
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
                onSave = { updated ->
                    selectedProducts = selectedProducts.map { if (it.name == dialogProductToEdit!!.name) updated else it }
                    dialogProductToEdit = null
                },
                onDelete = {
                    selectedProducts = selectedProducts.filter { it.name != dialogProductToEdit!!.name }
                    dialogProductToEdit = null
                }
            )
        }

        AlertDialog(
            onDismissRequest = { showSkincareDialog = false; routineToEdit = null },
            title = { Text(if (routineToEdit == null) "New Skincare Routine" else "Edit Routine", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    item {
                        ExposedDropdownMenuBox(expanded = routineDropdownExpanded, onExpandedChange = { routineDropdownExpanded = it }) {
                            // --- FIX: MenuAnchor explicitly set to PrimaryEditable ---
                            OutlinedTextField(
                                value = inputRoutineName, onValueChange = { inputRoutineName = it; routineDropdownExpanded = true },
                                label = { Text("Routine Name (e.g., Morning Wash)") }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, true),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routineDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(), singleLine = true
                            )
                            val filteredRoutines = skincareRoutines.filter { it.name.contains(inputRoutineName, true) }
                            if (filteredRoutines.isNotEmpty() && routineDropdownExpanded) {
                                // --- FIX: Expanded is always true inside this block ---
                                ExposedDropdownMenu(expanded = true, onDismissRequest = { routineDropdownExpanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
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
                            onRemove = { removedProduct -> selectedProducts = selectedProducts.filter { it.name != removedProduct.name } },
                            onLongClick = { dialogProductToEdit = it }
                        )

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExposedDropdownMenuBox(expanded = productDropdownExpanded, onExpandedChange = { productDropdownExpanded = it }, modifier = Modifier.weight(1f)) {
                                // --- FIX: MenuAnchor explicitly set to PrimaryEditable ---
                                OutlinedTextField(
                                    value = productSearchText, onValueChange = { productSearchText = it; productDropdownExpanded = true },
                                    placeholder = { Text("Search or add...") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(), singleLine = true
                                )
                                val filteredProducts = skincareProducts.filter { it.name.contains(productSearchText, true) && !selectedProducts.any { sp -> sp.name == it.name } }
                                val exactMatch = skincareProducts.any { it.name.equals(productSearchText.trim(), ignoreCase = true) }

                                if ((filteredProducts.isNotEmpty() || (productSearchText.isNotBlank() && !exactMatch)) && productDropdownExpanded) {
                                    // --- FIX: Expanded is always true inside this block ---
                                    ExposedDropdownMenu(expanded = true, onDismissRequest = { productDropdownExpanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                                        filteredProducts.forEach { product ->
                                            Box(modifier = Modifier.fillMaxWidth().clickable(
                                                onClick = { selectedProducts = selectedProducts + product; productSearchText = ""; productDropdownExpanded = false }
                                            ).padding(16.dp)) { Text(product.name) }
                                        }
                                        if (productSearchText.isNotBlank() && !exactMatch) {
                                            DropdownMenuItem(
                                                text = { Text("Create new: \"${productSearchText.trim()}\"", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    val trimmed = productSearchText.trim()
                                                    viewModel.addSkincareProduct(trimmed)
                                                    selectedProducts = selectedProducts + SkincareProduct(name = trimmed)
                                                    productSearchText = ""
                                                    productDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                val trimmed = productSearchText.trim()
                                if (trimmed.isNotBlank() && !selectedProducts.any { it.name == trimmed }) {
                                    viewModel.addSkincareProduct(trimmed)
                                    selectedProducts = selectedProducts + SkincareProduct(name = trimmed)
                                    productSearchText = ""
                                    productDropdownExpanded = false
                                }
                            }) { Icon(Icons.Default.Add, "Add Product", tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (routineToEdit != null) {
                            TextButton(
                                onClick = { showDeleteConfirmDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text("Delete Preset") }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        TextButton(onClick = { showSkincareDialog = false; routineToEdit = null }) {
                            Text("Cancel")
                        }
                    }
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

            val visibleRoutines = skincareRoutines.filter { completedRoutines.contains(it.name) }

            if (visibleRoutines.isEmpty()) {
                Text("No skincare routines logged today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                visibleRoutines.forEach { routine ->
                    // --- OPTIMIZATION: Caches parsing result until DB changes ---
                    val products = remember(routine.productsJson) {
                        try { Json.decodeFromString<List<SkincareProduct>>(routine.productsJson) }
                        catch (_: Exception) {
                            try { Json.decodeFromString<List<String>>(routine.productsJson).map { SkincareProduct(name = it) } }
                            catch (_: Exception) { emptyList() } // --- FIX ---
                        }
                    }

                    val isRoutineFullyChecked = products.isNotEmpty() && products.all { completedSteps.contains("${routine.name}_${it.name}") }

                    key(routine.id) {
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.removeLoggedSkincareRoutine(routine.name, products.map { it.name })
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else Color.Transparent
                                Box(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp).background(color, RoundedCornerShape(16.dp)).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
                                    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.onError)
                                }
                            }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(vertical = 12.dp)) {
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
                                    IconButton(onClick = { routineToEdit = routine; showSkincareDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Routine", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    }
}