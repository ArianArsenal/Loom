package com.example.loom.ui.sandbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.loom.data.models.CalendarChecklistItem
import com.example.loom.data.models.KanbanCard
import com.example.loom.data.models.SandboxSpace

@Composable
fun EditSpaceTagsDialog(space: SandboxSpace, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    var tagsString by remember { mutableStateOf(space.tags.joinToString(", ")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Space Tags") },
        text = {
            OutlinedTextField(
                value = tagsString,
                onValueChange = { tagsString = it },
                label = { Text("Tags (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val parsedTags = tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                onSave(parsedTags)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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