package com.example.loom.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList // FIX: Correct AutoMirrored import
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt // FIX: Using fast KTX color engine
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.data.local.entity.SubTask
import com.example.loom.data.local.entity.Task
import com.example.loom.data.local.entity.TaskSection
import com.example.loom.ui.home.HomeViewModel
import kotlinx.serialization.json.Json

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksAndDeadlinesCard(
    sections: List<TaskSection>,
    allTasks: List<Task>,
    tags: List<NoteTag>,
    isKanbanView: Boolean,
    viewModel: HomeViewModel,
    onToggleKanban: () -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteSection: (TaskSection) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tasks & Deadlines", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onToggleKanban, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = if (isKanbanView) Icons.Default.ViewKanban else Icons.AutoMirrored.Filled.ViewList, contentDescription = "Toggle View", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Button(onClick = onAddTask, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("+ Add Task", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val activeSections = sections.filter { sec -> allTasks.any { it.sectionId == sec.id } }

            if (activeSections.isEmpty()) {
                Text("No tasks here yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                activeSections.forEach { section ->
                    val sectionTasks = allTasks.filter { it.sectionId == section.id }
                    if (sectionTasks.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).combinedClickable(onClick = {}, onLongClick = { onDeleteSection(section) }),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = section.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }

                            if (isKanbanView) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val statuses = listOf("TO DO", "IN PROGRESS", "DONE")
                                    statuses.forEach { status ->
                                        val colTasks = sectionTasks.filter { it.status == status }
                                        Column(
                                            modifier = Modifier.width(280.dp).fillMaxHeight().defaultMinSize(minHeight = 150.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(12.dp)
                                        ) {
                                            Text(status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                                            if (colTasks.isEmpty()) {
                                                Text(text = "Empty", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                                            } else {
                                                colTasks.forEach { task -> TaskCard(task, tags, viewModel, true, onEdit = { onEditTask(it) }) }
                                            }
                                        }
                                    }
                                }
                            } else {
                                sectionTasks.forEach { task -> TaskCard(task, tags, viewModel, false, onEdit = { onEditTask(it) }) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(task: Task, allTags: List<NoteTag>, viewModel: HomeViewModel, isKanbanView: Boolean, onEdit: (Task) -> Unit) {
    val priorityColor = when (task.priority) { 1 -> Color(0xFF4CAF50); 2 -> Color(0xFF8BC34A); 3 -> Color(0xFFFFEB3B); 4 -> Color(0xFFFF9800); 5 -> Color(0xFFF44336); else -> Color.Gray }
    var showMenu by remember { mutableStateOf(false) }

    val subtasks = remember(task.subtasksJson) { try { Json.decodeFromString<List<SubTask>>(task.subtasksJson) } catch (_: Exception) { emptyList() } } // FIX: Unused 'e'
    val taskTags = remember(task.tagsJson) { try { Json.decodeFromString<List<String>>(task.tagsJson) } catch (_: Exception) { emptyList() } } // FIX: Unused 'e'

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).combinedClickable(onClick = { viewModel.toggleTaskCompletion(task) }, onLongClick = { onEdit(task) }),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(50)).background(priorityColor))
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None))
                    if (task.description.isNotBlank()) Text(task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                IconButton(onClick = { viewModel.toggleTaskCompletion(task) }, modifier = Modifier.size(32.dp)) {
                    Icon(if (task.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, "Complete", tint = if (task.isCompleted) priorityColor else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (subtasks.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 4.dp)) {
                    subtasks.forEach { subtask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleSubtaskCompletion(task, subtask.id) }.padding(vertical = 2.dp)
                        ) {
                            Icon(imageVector = if (subtask.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Circle, contentDescription = "Toggle Subtask", modifier = Modifier.size(16.dp), tint = if (subtask.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = subtask.title, style = MaterialTheme.typography.bodySmall.copy(textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None), color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (taskTags.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).weight(1f, fill = false)) {
                            taskTags.forEach { tagName ->
                                // --- FIX: Uses strict, fast toColorInt() parsing ---
                                val tagColor = allTags.find { it.name == tagName }?.colorHex?.let { try { Color(it.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.primary } } ?: MaterialTheme.colorScheme.primary
                                Surface(color = tagColor.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) { Text(tagName, style = MaterialTheme.typography.labelSmall, color = tagColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
                            }
                        }
                    }

                    val remaining = getRemainingDaysText(task.targetDate)
                    if (remaining != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val isOverdue = remaining.contains("overdue")
                        Text(text = remaining, style = MaterialTheme.typography.labelSmall, color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                    val statuses = listOf("TO DO", "IN PROGRESS", "DONE")
                    val currentIndex = statuses.indexOf(task.status)

                    if (isKanbanView && currentIndex > 0) {
                        IconButton(onClick = { viewModel.updateTaskStatus(task, statuses[currentIndex - 1]) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.ChevronLeft, "Move Left", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }

                    Surface(
                        modifier = Modifier.clickable { viewModel.updateTaskStatus(task, when (task.status) { "TO DO" -> "IN PROGRESS"; "IN PROGRESS" -> "DONE"; else -> "TO DO" }) },
                        color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)
                    ) { Text(task.status, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }

                    if (isKanbanView && currentIndex < statuses.size - 1) {
                        IconButton(onClick = { viewModel.updateTaskStatus(task, statuses[currentIndex + 1]) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.ChevronRight, "Move Right", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Edit Task") }, onClick = { onEdit(task); showMenu = false }, leadingIcon = { Icon(Icons.Default.Edit, null) })
            DropdownMenuItem(text = { Text("Delete Task", color = MaterialTheme.colorScheme.error) }, onClick = { viewModel.deleteTask(task); showMenu = false }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
        }
    }
}