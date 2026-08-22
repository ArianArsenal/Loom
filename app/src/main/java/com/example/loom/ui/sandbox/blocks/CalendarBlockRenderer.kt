package com.example.loom.ui.sandbox.blocks

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.loom.data.models.CalendarChecklistItem
import com.example.loom.data.models.CalendarEvent
import com.example.loom.data.models.SandboxBlock
import com.example.loom.ui.sandbox.copyMediaToLocal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarBlockRenderer(
    block: SandboxBlock,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var showBlockMenu by remember { mutableStateOf(false) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var isCalendarEditMode by remember { mutableStateOf(false) }
    var pendingCalendarDate by remember { mutableStateOf<String?>(null) }

    val calendarImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val localUrl = copyMediaToLocal(context, it)
            if (localUrl != null) {
                pendingCalendarDate?.let { dateStr ->
                    val eventsForDay = block.calendarEvents[dateStr] ?: emptyList()
                    val newEvents = eventsForDay.toMutableList().apply { add(CalendarEvent(type = "Image", imageUrl = localUrl)) }
                    onUpdate(block.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, "Prev") }
                Text("${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, "Next") }
                    IconButton(onClick = { showBlockMenu = true }) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                    DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                        DropdownMenuItem(text = { Text("Delete Calendar", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(8.dp))

            val daysInMonth = currentMonth.lengthOfMonth()
            val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value

            var dayCounter = 1
            for (row in 0..5) {
                if (dayCounter > daysInMonth) break
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (col in 1..7) {
                        if ((row == 0 && col < firstDayOfWeek) || dayCounter > daysInMonth) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = currentMonth.atDay(dayCounter)
                            val dateStr = date.toString()
                            val events = block.calendarEvents[dateStr] ?: emptyList()

                            Surface(
                                modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp).combinedClickable(
                                    onClick = { selectedDate = date; isCalendarEditMode = false },
                                    onLongClick = { selectedDate = date; isCalendarEditMode = true }
                                ),
                                shape = RoundedCornerShape(8.dp),
                                color = if (events.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (date == LocalDate.now()) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text(dayCounter.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (events.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Unspecified)
                                    if (events.isNotEmpty()) {
                                        Text("${events.size} card${if(events.size>1)"s" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontSize = 8.sp)
                                    }
                                }
                            }
                            dayCounter++
                        }
                    }
                }
            }
        }
    }

    if (selectedDate != null) {
        val dateStr = selectedDate.toString()
        val eventsForDay = block.calendarEvents[dateStr] ?: emptyList()
        var expandedImage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { selectedDate = null },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(dateStr, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { isCalendarEditMode = !isCalendarEditMode }) {
                        Icon(if(isCalendarEditMode) Icons.Outlined.Visibility else Icons.Outlined.Edit, "Toggle Mode")
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    if (eventsForDay.isEmpty()) {
                        Text("No cards for this day.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                    }

                    eventsForDay.forEachIndexed { eIndex, event ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = if (isCalendarEditMode) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.6f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {

                                if (!isCalendarEditMode) {
                                    if (event.type == "Image" && event.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = event.imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)).clickable { expandedImage = event.imageUrl }
                                        )
                                        if (event.title.isNotEmpty()) Text(event.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                                    } else {
                                        if (event.title.isNotEmpty()) Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        if (event.details.isNotEmpty()) Text(event.details, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))

                                        if (event.type == "Checklist") {
                                            Spacer(Modifier.height(8.dp))
                                            event.checklist.forEachIndexed { cIndex, item ->
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                                    Icon(
                                                        if(item.isChecked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                                                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).clickable {
                                                            val newChecklist = event.checklist.toMutableList()
                                                            newChecklist[cIndex] = item.copy(isChecked = !item.isChecked)
                                                            val newEvents = eventsForDay.toMutableList()
                                                            newEvents[eIndex] = event.copy(checklist = newChecklist)
                                                            onUpdate(block.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                                        }
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(item.text, style = MaterialTheme.typography.bodyMedium.copy(textDecoration = if (item.isChecked) TextDecoration.LineThrough else null, color = if(item.isChecked) Color.Gray else MaterialTheme.colorScheme.onSurface))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                                            Text(event.type, color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                        }
                                        IconButton(onClick = {
                                            val newEvents = eventsForDay.toMutableList().apply { removeAt(eIndex) }
                                            onUpdate(block.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                        }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "Delete", tint = MaterialTheme.colorScheme.error) }
                                    }

                                    OutlinedTextField(
                                        value = event.title, onValueChange = { t ->
                                            val newEvents = eventsForDay.toMutableList()
                                            newEvents[eIndex] = event.copy(title = t)
                                            onUpdate(block.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                        },
                                        label = { Text("Title") }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    )

                                    if (event.type == "Task" || event.type == "Note") {
                                        OutlinedTextField(
                                            value = event.details, onValueChange = { d ->
                                                val newEvents = eventsForDay.toMutableList()
                                                newEvents[eIndex] = event.copy(details = d)
                                                onUpdate(block.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                            },
                                            label = { Text("Details") }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        )
                                    }

                                    if (event.type == "Checklist") {
                                        Spacer(Modifier.height(8.dp))
                                        event.checklist.forEachIndexed { cIndex, item ->
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                                Icon(if(item.isChecked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                                                Spacer(Modifier.width(8.dp))
                                                BasicTextField(
                                                    value = item.text, onValueChange = { newT ->
                                                        val newChecklist = event.checklist.toMutableList()
                                                        newChecklist[cIndex] = item.copy(text = newT)
                                                        val newEvents = eventsForDay.toMutableList()
                                                        newEvents[eIndex] = event.copy(checklist = newChecklist)
                                                        onUpdate(block.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                                    },
                                                    modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp)).padding(6.dp),
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                                                )
                                                IconButton(onClick = {
                                                    val newChecklist = event.checklist.toMutableList().apply { removeAt(cIndex) }
                                                    val newEvents = eventsForDay.toMutableList()
                                                    newEvents[eIndex] = event.copy(checklist = newChecklist)
                                                    onUpdate(block.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                                }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Remove, "Remove", tint = MaterialTheme.colorScheme.error) }
                                            }
                                        }
                                        TextButton(onClick = {
                                            val newChecklist = event.checklist.toMutableList().apply { add(CalendarChecklistItem()) }
                                            val newEvents = eventsForDay.toMutableList()
                                            newEvents[eIndex] = event.copy(checklist = newChecklist)
                                            onUpdate(block.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                        }) { Text("+ Add Item") }
                                    }

                                    if (event.type == "Image" && event.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = event.imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                            modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).clickable { expandedImage = event.imageUrl }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isCalendarEditMode) {
                        var showAddMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { showAddMenu = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("+ Create Card") }
                            DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                                DropdownMenuItem(text = { Text("Task") }, onClick = {
                                    val newEvents = eventsForDay.toMutableList().apply { add(CalendarEvent(type = "Task")) }
                                    onUpdate(block.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                    showAddMenu = false
                                })
                                DropdownMenuItem(text = { Text("Checklist") }, onClick = {
                                    val newEvents = eventsForDay.toMutableList().apply { add(CalendarEvent(type = "Checklist", checklist = listOf(CalendarChecklistItem()))) }
                                    onUpdate(block.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                    showAddMenu = false
                                })
                                DropdownMenuItem(text = { Text("Image") }, onClick = {
                                    pendingCalendarDate = dateStr
                                    calendarImageLauncher.launch("image/*")
                                    showAddMenu = false
                                })
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedDate = null }) { Text("Close") } }
        )

        if (expandedImage != null) {
            AlertDialog(
                onDismissRequest = { expandedImage = null },
                text = { AsyncImage(model = expandedImage, contentDescription = null, modifier = Modifier.fillMaxWidth()) },
                confirmButton = { TextButton(onClick = { expandedImage = null }) { Text("Close") } }
            )
        }
    }
}