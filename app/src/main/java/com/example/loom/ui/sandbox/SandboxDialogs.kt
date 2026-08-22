package com.example.loom.ui.sandbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt // --- FIX: Fast String parsing ---
import com.example.loom.data.models.CalendarChecklistItem
import com.example.loom.data.models.KanbanCard
import com.example.loom.data.models.SandboxSpace

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditSpaceTagsDialog(
    space: SandboxSpace,
    allExistingTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
    onDeleteGlobalTag: (String) -> Unit
) {
    var currentTags by remember { mutableStateOf(space.tags) }
    var localExistingTags by remember { mutableStateOf(allExistingTags) }
    var newTagText by remember { mutableStateOf("") }
    var tagToDelete by remember { mutableStateOf<String?>(null) }

    var hue by remember { mutableFloatStateOf(120f) }
    var saturation by remember { mutableFloatStateOf(0.9f) }
    var value by remember { mutableFloatStateOf(0.9f) }
    val selectedColor = Color.hsv(hue, saturation, value)

    val availableTags = localExistingTags.filter { it !in currentTags }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Space Tags", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Tap to equip/unequip. Hold to permanently delete.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                // 1. DISPLAY CURRENT (ACTIVE) TAGS
                if (currentTags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentTags.forEach { tagString ->
                            val parts = tagString.split("|")
                            val text = parts[0]
                            val hex = parts.getOrNull(1) ?: "#4CAF50"
                            val tagColor = try { Color(hex.toColorInt()) } catch (_: Exception) { Color(0xFF4CAF50) } // --- FIX: KTX & Unused e ---

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = tagColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, tagColor),
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            currentTags = currentTags.filter { it != tagString }
                                        },
                                        onLongPress = {
                                            tagToDelete = tagString
                                        }
                                    )
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(text, color = tagColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.Close, contentDescription = "Unequip", tint = tagColor, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                } else {
                    Text("No tags equipped.", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
                }

                // 2. DISPLAY AVAILABLE GLOBAL TAGS
                if (availableTags.isNotEmpty()) {
                    Text("Available Tags", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableTags.forEach { tagString ->
                            val parts = tagString.split("|")
                            val text = parts[0]
                            val hex = parts.getOrNull(1) ?: "#4CAF50"
                            val tagColor = try { Color(hex.toColorInt()) } catch (_: Exception) { Color(0xFF4CAF50) } // --- FIX: KTX & Unused e ---

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, tagColor),
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            currentTags = currentTags + tagString
                                        },
                                        onLongPress = {
                                            tagToDelete = tagString
                                        }
                                    )
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(text, color = tagColor, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.Add, contentDescription = "Equip", tint = tagColor, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                }

                // 3. CREATE NEW TAG
                Text("Create New Tag", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    placeholder = { Text("New tag name...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // HSV COLOR PICKER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.hsv(hue, 1f, 1f))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                saturation = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                value = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                saturation = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                value = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        drawCircle(Color.White, 6.dp.toPx(), Offset(saturation * size.width, (1f - value) * size.height), style = Stroke(2.dp.toPx()))
                    }
                }

                Spacer(Modifier.height(12.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(50))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                hue = (offset.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                hue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                            }
                        }
                ) {
                    drawRect(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                    drawCircle(Color.White, 10.dp.toPx(), Offset((hue / 360f) * size.width, size.height / 2), style = Stroke(3.dp.toPx()))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (newTagText.isNotBlank()) {
                            val hexColor = String.format("#%06X", (0xFFFFFF and selectedColor.toArgb()))
                            val newTag = "${newTagText.trim()}|$hexColor"
                            if (!currentTags.contains(newTag)) {
                                currentTags = currentTags + newTag
                                localExistingTags = localExistingTags + newTag
                            }
                            newTagText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newTagText.isNotBlank()
                ) {
                    Text("Create & Equip Tag")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(currentTags) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (tagToDelete != null) {
        val parts = tagToDelete!!.split("|")
        val text = parts[0]

        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("Delete '$text' Globally?") },
            text = { Text("This will permanently remove this tag from your database and unequips it from all spaces. This action cannot be undone.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDeleteGlobalTag(tagToDelete!!)
                        currentTags = currentTags.filter { it != tagToDelete }
                        localExistingTags = localExistingTags.filter { it != tagToDelete }
                        tagToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Suppress("unused") // --- FIX: Acknowledged as component library dialog ---
@Composable
fun EditKanbanCardDialog(
    card: KanbanCard,
    onDismiss: () -> Unit,
    onSave: (KanbanCard) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(card.text) }
    var description by remember { mutableStateOf(card.description) }
    var priority by remember { mutableIntStateOf(card.priority) }

    var tagsString by remember { mutableStateOf(card.tags.joinToString(", ")) }
    var subtasks by remember { mutableStateOf(card.subtasks) }
    var newSubtaskText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Card") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }

                item {
                    Text("Priority (1-5):", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth().padding(top=4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        (1..5).forEach { lvl ->
                            val pColor = when(lvl) { 1->Color(0xFF4CAF50); 2->Color(0xFF8BC34A); 3->Color(0xFFFFEB3B); 4->Color(0xFFFF9800); 5->Color(0xFFF44336); else->Color.Gray }
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(pColor.copy(alpha = if (priority == lvl) 1f else 0.3f))
                                    .border(if (priority == lvl) 2.dp else 0.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                    .clickable { priority = lvl },
                                contentAlignment = Alignment.Center
                            ) { Text("$lvl", color = if (priority == lvl) Color.White else MaterialTheme.colorScheme.onSurface) }
                        }
                    }
                }

                item {
                    Text("Subtasks:", style = MaterialTheme.typography.labelMedium)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        subtasks.forEachIndexed { index, subtask ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (subtask.isChecked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).clickable {
                                    val newSubtasks = subtasks.toMutableList()
                                    newSubtasks[index] = subtask.copy(isChecked = !subtask.isChecked)
                                    subtasks = newSubtasks
                                })
                                Spacer(Modifier.width(8.dp))
                                BasicTextField(value = subtask.text, onValueChange = { t ->
                                    val newSubtasks = subtasks.toMutableList()
                                    newSubtasks[index] = subtask.copy(text = t)
                                    subtasks = newSubtasks
                                }, modifier = Modifier.weight(1f), textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface))
                                IconButton(onClick = { subtasks = subtasks.filterIndexed { i, _ -> i != index } }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = newSubtaskText, onValueChange = { newSubtaskText = it }, placeholder = { Text("New subtask...") }, modifier = Modifier.weight(1f), singleLine = true)
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = {
                                if (newSubtaskText.isNotBlank()) {
                                    subtasks = subtasks + CalendarChecklistItem(text = newSubtaskText)
                                    newSubtaskText = ""
                                }
                            }) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }

                item {
                    OutlinedTextField(value = tagsString, onValueChange = { tagsString = it }, label = { Text("Tags (comma separated)") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDelete) { Text("Delete Card", color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    val parsedTags = tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(card.copy(text = title, description = description, priority = priority, subtasks = subtasks, tags = parsedTags))
                }) { Text("Save") }
            }
        }
    )
}