package com.example.loom.ui.sandbox.blocks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.loom.data.models.CalendarChecklistItem
import com.example.loom.data.models.KanbanCard
import com.example.loom.data.models.KanbanColumn

import com.example.loom.data.models.SandboxBlock
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun KanbanBlockRenderer(
    block: SandboxBlock,
    textState: TextFieldValue,
    handleTextChange: (TextFieldValue) -> Unit,
    commonModifier: Modifier,
    placeholderUI: @Composable (@Composable () -> Unit) -> Unit,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    var showBlockMenu by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<Triple<Int, Int, KanbanCard>?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = textState, onValueChange = handleTextChange,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                modifier = commonModifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = placeholderUI
            )
            IconButton(onClick = { showBlockMenu = true }) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
            DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                DropdownMenuItem(text = { Text("Delete Board", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
            }
        }

        val columns = if (block.kanbanColumns.isEmpty()) listOf(KanbanColumn(title = "To Do"), KanbanColumn(title = "In Progress"), KanbanColumn(title = "Done")) else block.kanbanColumns

        // --- FIX: IntrinsicSize.Max ensures all columns dynamically stretch to match the tallest one ---
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            columns.forEachIndexed { colIndex, col ->
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight() // Takes up full Intrinsic size available in the row
                        .defaultMinSize(minHeight = 150.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = col.title, onValueChange = { t ->
                                val newCols = columns.toMutableList(); newCols[colIndex] = col.copy(title = t)
                                onUpdate(block.copy(kanbanColumns = newCols))
                            },
                            textStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                            decorationBox = { inner -> Box{ if (col.title.isEmpty()) Text("COLUMN NAME", color = Color.Gray, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)); inner() } }
                        )
                        IconButton(onClick = {
                            val newCols = columns.toMutableList().apply{ removeAt(colIndex) }
                            if (newCols.isEmpty()) { onDeleteBlock("", true) } else { onUpdate(block.copy(kanbanColumns = newCols)) }
                        }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Close, "Delete Column", tint = Color.Gray, modifier = Modifier.size(14.dp)) }
                    }

                    col.cards.forEachIndexed { cardIndex, card ->
                        val priorityColor = when (card.priority) { 1 -> Color(0xFF4CAF50); 2 -> Color(0xFF8BC34A); 3 -> Color(0xFFFFEB3B); 4 -> Color(0xFFFF9800); 5 -> Color(0xFFF44336); else -> Color.Gray }
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).combinedClickable(onClick = { editingCard = Triple(colIndex, cardIndex, card) }, onLongClick = { editingCard = Triple(colIndex, cardIndex, card) }),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(50)).background(priorityColor)); Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(card.text.ifBlank { "New Task" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                        if (card.description.isNotBlank()) Text(card.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }

                                if (card.subtasks.isNotEmpty()) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 4.dp)) {
                                        card.subtasks.forEachIndexed { sIndex, subtask ->
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                                                val newSub = card.subtasks.toMutableList().apply { set(sIndex, subtask.copy(isChecked = !subtask.isChecked)) }
                                                val newCards = col.cards.toMutableList().apply { set(cardIndex, card.copy(subtasks = newSub)) }
                                                val newCols = columns.toMutableList().apply { set(colIndex, col.copy(cards = newCards)) }
                                                onUpdate(block.copy(kanbanColumns = newCols))
                                            }.padding(vertical = 2.dp)) {
                                                Icon(if (subtask.isChecked) Icons.Outlined.CheckCircle else Icons.Outlined.Circle, null, modifier = Modifier.size(16.dp), tint = if (subtask.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(Modifier.width(8.dp))
                                                Text(subtask.text, style = MaterialTheme.typography.bodySmall.copy(textDecoration = if (subtask.isChecked) TextDecoration.LineThrough else TextDecoration.None), color = if(subtask.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        if (card.tags.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).weight(1f, fill = false)) {
                                                card.tags.forEach { tagName ->
                                                    val tagHash = Math.abs(tagName.hashCode()) % 5
                                                    val tagColor = when(tagHash) { 0->Color(0xFF2196F3); 1->Color(0xFF9C27B0); 2->Color(0xFFFF9800); 3->Color(0xFF00BCD4); else->Color(0xFFE91E63) }
                                                    Surface(color = tagColor.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) { Text(tagName, style = MaterialTheme.typography.labelSmall, color = tagColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
                                                }
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                                        if (colIndex > 0) {
                                            IconButton(onClick = {
                                                val newCols = columns.toMutableList()
                                                newCols[colIndex] = newCols[colIndex].copy(cards = newCols[colIndex].cards.toMutableList().apply { removeAt(cardIndex) })
                                                newCols[colIndex - 1] = newCols[colIndex - 1].copy(cards = newCols[colIndex - 1].cards + card)
                                                onUpdate(block.copy(kanbanColumns = newCols))
                                            }, modifier = Modifier.size(28.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBackIos, "Move Left", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        }
                                        if (colIndex < columns.size - 1) {
                                            IconButton(onClick = {
                                                val newCols = columns.toMutableList()
                                                newCols[colIndex] = newCols[colIndex].copy(cards = newCols[colIndex].cards.toMutableList().apply { removeAt(cardIndex) })
                                                newCols[colIndex + 1] = newCols[colIndex + 1].copy(cards = newCols[colIndex + 1].cards + card)
                                                onUpdate(block.copy(kanbanColumns = newCols))
                                            }, modifier = Modifier.size(28.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, "Move Right", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TextButton(onClick = {
                        val newCols = columns.toMutableList()
                        newCols[colIndex] = newCols[colIndex].copy(cards = newCols[colIndex].cards + KanbanCard(text = "New Card"))
                        onUpdate(block.copy(kanbanColumns = newCols))
                    }, modifier = Modifier.fillMaxWidth()) { Text("+ Add Card", color = Color.Gray) }
                }
            }

            Column(
                modifier = Modifier
                    .width(50.dp)
                    .fillMaxHeight()
                    .defaultMinSize(minHeight = 150.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clickable { onUpdate(block.copy(kanbanColumns = columns + KanbanColumn(title = "New Column"))) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Add, "Add Column", tint = Color.Gray)
            }
        }

        // --- FIX: Completely Re-Written Inline Kanban Card Edit Dialog ---
        if (editingCard != null) {
            val (cIdx, rIdx, cardToEdit) = editingCard!!

            var editTitle by remember { mutableStateOf(cardToEdit.text) }
            var editDesc by remember { mutableStateOf(cardToEdit.description) }
            var editPriority by remember { mutableIntStateOf(cardToEdit.priority) }

            // Chips tag state
            var editTags by remember { mutableStateOf(cardToEdit.tags) }
            var newTagText by remember { mutableStateOf("") }

            // Subtasks state
            var editSubtasks by remember { mutableStateOf(cardToEdit.subtasks) }

            AlertDialog(
                onDismissRequest = { editingCard = null },
                title = { Text("Edit Task Card") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Task Name") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

                        Spacer(Modifier.height(16.dp))
                        Text("Priority (1-5)", style = MaterialTheme.typography.labelMedium)
                        Slider(value = editPriority.toFloat(), onValueChange = { editPriority = it.roundToInt() }, valueRange = 1f..5f, steps = 3)

                        Spacer(Modifier.height(16.dp))
                        Text("Tags", style = MaterialTheme.typography.labelMedium)

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                            editTags.forEach { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = { editTags = editTags - tag },
                                    label = { Text(tag) },
                                    trailingIcon = { Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = { newTagText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Type tag and press Add...") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (newTagText.isNotBlank() && !editTags.contains(newTagText.trim())) {
                                        editTags = editTags + newTagText.trim()
                                        newTagText = ""
                                    }
                                }) { Icon(Icons.Default.Add, "Add Tag") }
                            }
                        )

                        Spacer(Modifier.height(16.dp))
                        Text("Subtasks", style = MaterialTheme.typography.labelMedium)
                        editSubtasks.forEachIndexed { sIdx, sub ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Checkbox(checked = sub.isChecked, onCheckedChange = { chk -> editSubtasks = editSubtasks.toMutableList().apply { set(sIdx, sub.copy(isChecked = chk)) } })
                                OutlinedTextField(
                                    value = sub.text,
                                    onValueChange = { txt -> editSubtasks = editSubtasks.toMutableList().apply { set(sIdx, sub.copy(text = txt)) } },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { editSubtasks = editSubtasks.toMutableList().apply { removeAt(sIdx) } }) { Icon(Icons.Default.Close, "Remove") }
                            }
                        }
                        TextButton(onClick = { editSubtasks = editSubtasks + CalendarChecklistItem(
                            text = "New Subtask"
                        )
                        }) { Text("+ Add Subtask") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val newCols = columns.toMutableList()
                        val newCards = newCols[cIdx].cards.toMutableList()
                        newCards[rIdx] = cardToEdit.copy(text = editTitle, description = editDesc, priority = editPriority, tags = editTags, subtasks = editSubtasks)
                        newCols[cIdx] = newCols[cIdx].copy(cards = newCards)
                        onUpdate(block.copy(kanbanColumns = newCols))
                        editingCard = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        val newCols = columns.toMutableList()
                        val newCards = newCols[cIdx].cards.toMutableList()
                        newCards.removeAt(rIdx)
                        newCols[cIdx] = newCols[cIdx].copy(cards = newCards)
                        onUpdate(block.copy(kanbanColumns = newCols))
                        editingCard = null
                    }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete Card") }
                }
            )
        }
    }
}