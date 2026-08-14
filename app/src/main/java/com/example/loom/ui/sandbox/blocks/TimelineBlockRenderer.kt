package com.example.loom.ui.sandbox.blocks

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import com.example.loom.ui.sandbox.BlockRenderer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Serializable
data class TimelineSettings(
    val mode: String = "WEEKDAYS",
    val startDayOfWeek: Int = Calendar.MONDAY,
    val granularityMin: Int = 30,
    val liveStartDateMillis: Long = System.currentTimeMillis(),
    val startHour: Int = 0,
    val endHour: Int = 24,
    val customDays: List<String> = listOf("Day 1", "Day 2", "Day 3", "Day 4", "Day 5", "Day 6", "Day 7")
)

@Serializable
data class TimelineCardMeta(
    val dayIndex: Int = 0,
    val startTimeMin: Int = 0,
    val durationMin: Int = 0,
    val colorHex: String = "#4CAF50"
)

@Composable
private fun ModernChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun getCustomTimePickerColors() = TimePickerDefaults.colors(
    clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
    clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
    clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
    selectorColor = MaterialTheme.colorScheme.primary,
    containerColor = MaterialTheme.colorScheme.surface,
    periodSelectorBorderColor = MaterialTheme.colorScheme.outlineVariant,
    periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    periodSelectorSelectedContentColor = MaterialTheme.colorScheme.primary,
    periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.primary,
    timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineBlockRenderer(
    block: SandboxBlock,
    allSpaces: List<SandboxSpace>,
    currentSpaceId: String,
    recentTableColors: MutableList<Color>,
    focusRequesters: MutableMap<String, FocusRequester>,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    val currentBlock by rememberUpdatedState(block)

    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }

    val settings = try { Json.decodeFromString<TimelineSettings>(currentBlock.secondaryContent) } catch (e: Exception) { TimelineSettings() }

    val rowHeight = 60.dp
    val hourWidth = 120.dp
    val density = LocalDensity.current
    val minuteWidthDp = hourWidth / 60f
    val rowHeightPx = with(density) { rowHeight.toPx() }

    val horizontalScrollState = rememberScrollState()

    val dayLabels = remember(settings.mode, settings.startDayOfWeek, settings.liveStartDateMillis, settings.customDays) {
        val labels = mutableListOf<String>()
        val cal = Calendar.getInstance()
        when (settings.mode) {
            "TEMPLATE" -> for (i in 1..7) labels.add("Day $i")
            "CUSTOM" -> labels.addAll(settings.customDays)
            "LIVE" -> {
                cal.timeInMillis = settings.liveStartDateMillis
                val sdf = SimpleDateFormat("MMM d (EEE)", Locale.getDefault())
                for (i in 0..6) { labels.add(sdf.format(cal.time)); cal.add(Calendar.DAY_OF_YEAR, 1) }
            }
            else -> {
                val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                var current = settings.startDayOfWeek - 1
                for (i in 0..6) { labels.add(daysOfWeek[current % 7]); current++ }
            }
        }
        labels
    }

    var cardToEditProps by remember { mutableStateOf<SandboxBlock?>(null) }
    var isNewCard by remember { mutableStateOf(false) }
    var activeCanvasCardId by remember { mutableStateOf<String?>(null) }
    var canvasFocusedBlockId by remember { mutableStateOf<String?>(null) }

    // --- EVENT PROPERTIES DIALOG ---
    val propsCard = cardToEditProps
    if (propsCard != null) {
        val meta = try { Json.decodeFromString<TimelineCardMeta>(propsCard.secondaryContent) } catch (e: Exception) { TimelineCardMeta() }

        var title by remember { mutableStateOf(propsCard.content.replace("\u200B", "")) }
        var dayIndex by remember { mutableIntStateOf(meta.dayIndex) }

        var startHour by remember { mutableIntStateOf(meta.startTimeMin / 60) }
        var startMin by remember { mutableIntStateOf(meta.startTimeMin % 60) }
        var endHour by remember { mutableIntStateOf((meta.startTimeMin + meta.durationMin) / 60) }
        var endMin by remember { mutableIntStateOf((meta.startTimeMin + meta.durationMin) % 60) }

        val initialColor = try { Color(android.graphics.Color.parseColor(meta.colorHex)) } catch (e: Exception) { Color(0xFF4CAF50) }
        var hue by remember { mutableFloatStateOf(0f) }
        var saturation by remember { mutableFloatStateOf(0f) }
        var value by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(initialColor) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
            hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
        }

        val dynamicColor = Color.hsv(hue, saturation, value)
        val hexString = String.format("#%06X", (0xFFFFFF and dynamicColor.toArgb()))

        var showStartPicker by remember { mutableStateOf(false) }
        var showEndPicker by remember { mutableStateOf(false) }

        if (showStartPicker) {
            val state = rememberTimePickerState(initialHour = startHour.coerceIn(0, 23), initialMinute = startMin, is24Hour = true)
            val selectedTotalMin = (state.hour * 60) + state.minute
            val minLimit = settings.startHour * 60
            val maxLimit = settings.endHour * 60
            val isValid = selectedTotalMin in minLimit..maxLimit

            AlertDialog(
                onDismissRequest = { showStartPicker = false },
                title = { Text("Start Time") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TimePicker(state = state, colors = getCustomTimePickerColors())
                        if (!isValid) {
                            Spacer(Modifier.height(8.dp))
                            Text("Time must be between ${settings.startHour}:00 and ${settings.endHour}:00", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { startHour = state.hour; startMin = state.minute; showStartPicker = false }, enabled = isValid) { Text("OK") } },
                dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancel") } }
            )
        }

        if (showEndPicker) {
            val state = rememberTimePickerState(initialHour = endHour.coerceIn(0, 23), initialMinute = endMin, is24Hour = true)
            val mappedHour = if (state.hour == 0 && startHour >= 0) 24 else state.hour
            val selectedTotalMin = (mappedHour * 60) + state.minute
            val minLimit = settings.startHour * 60
            val maxLimit = settings.endHour * 60
            val startTotalMin = (startHour * 60) + startMin
            val isValid = selectedTotalMin in minLimit..maxLimit && selectedTotalMin > startTotalMin

            AlertDialog(
                onDismissRequest = { showEndPicker = false },
                title = { Text("End Time") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TimePicker(state = state, colors = getCustomTimePickerColors())
                        if (!isValid) {
                            Spacer(Modifier.height(8.dp))
                            Text("Time must be between ${settings.startHour}:00 and ${settings.endHour}:00, and after Start Time.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { endHour = mappedHour; endMin = state.minute; showEndPicker = false }, enabled = isValid) { Text("OK") } },
                dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancel") } }
            )
        }

        Dialog(onDismissRequest = { cardToEditProps = null }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                    Text(if (isNewCard) "New Event" else "Edit Event", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(16.dp))

                    Text("Day of Week:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    var expandedDay by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedDay, onExpandedChange = { expandedDay = it }) {
                        OutlinedTextField(value = dayLabels[dayIndex.coerceIn(0..6)], onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDay) })
                        ExposedDropdownMenu(expanded = expandedDay, onDismissRequest = { expandedDay = false }) {
                            dayLabels.forEachIndexed { i, label -> DropdownMenuItem(text = { Text(label) }, onClick = { dayIndex = i; expandedDay = false }) }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start Time", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = "${startHour.toString().padStart(2, '0')}:${startMin.toString().padStart(2, '0')}",
                                onValueChange = {}, readOnly = true,
                                modifier = Modifier.fillMaxWidth().clickable { showStartPicker = true },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("End Time", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = "${endHour.toString().padStart(2, '0')}:${endMin.toString().padStart(2, '0')}",
                                onValueChange = {}, readOnly = true,
                                modifier = Modifier.fillMaxWidth().clickable { showEndPicker = true },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Card Color:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.hsv(hue, 1f, 1f))
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                saturation = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                value = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                            }
                        }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(brush = Brush.horizontalGradient(colors = listOf(Color.White, Color.Transparent)))
                            drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black)))
                            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(saturation * size.width, (1f - value) * size.height), style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Canvas(modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(50))
                        .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); hue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f } }
                    ) {
                        drawRect(brush = Brush.horizontalGradient(colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                        drawCircle(color = Color.White, radius = 12.dp.toPx(), center = Offset((hue / 360f) * size.width, size.height / 2), style = Stroke(width = 3.dp.toPx()))
                    }

                    Spacer(Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { cardToEditProps = null }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val startTotalMin = (startHour * 60) + startMin
                            val endTotalMin = (endHour * 60) + endMin
                            val safeDuration = maxOf(settings.granularityMin, endTotalMin - startTotalMin)

                            val updatedMeta = meta.copy(dayIndex = dayIndex, startTimeMin = startTotalMin, durationMin = safeDuration, colorHex = hexString)
                            val finalBlock = propsCard.copy(content = "\u200B$title", secondaryContent = Json.encodeToString(updatedMeta))

                            val newChildren = currentBlock.children.toMutableList()
                            if (isNewCard) newChildren.add(finalBlock) else {
                                val idx = newChildren.indexOfFirst { it.id == finalBlock.id }
                                if (idx != -1) newChildren[idx] = finalBlock
                            }
                            onUpdate(currentBlock.copy(children = newChildren))
                            cardToEditProps = null
                        }) { Text("Save Event") }
                    }
                }
            }
        }
    }

    // --- TIMELINE SETTINGS DIALOG ---
    if (showSettings) {
        var startHour by remember { mutableIntStateOf(settings.startHour) }
        var endHour by remember { mutableIntStateOf(settings.endHour) }
        var customDays by remember { mutableStateOf(settings.customDays) }

        var showSettingsStartPicker by remember { mutableStateOf(false) }
        var showSettingsEndPicker by remember { mutableStateOf(false) }
        var showLiveDatePicker by remember { mutableStateOf(false) }

        if (showSettingsStartPicker) {
            val state = rememberTimePickerState(initialHour = startHour.coerceIn(0, 23), initialMinute = 0, is24Hour = true)
            AlertDialog(
                onDismissRequest = { showSettingsStartPicker = false },
                title = { Text("Timeline Start Hour") },
                text = { TimePicker(state = state, colors = getCustomTimePickerColors()) },
                confirmButton = { TextButton(onClick = { startHour = state.hour; showSettingsStartPicker = false }) { Text("OK") } },
                dismissButton = { TextButton(onClick = { showSettingsStartPicker = false }) { Text("Cancel") } }
            )
        }

        if (showSettingsEndPicker) {
            val state = rememberTimePickerState(initialHour = endHour.coerceIn(0, 23), initialMinute = 0, is24Hour = true)
            AlertDialog(
                onDismissRequest = { showSettingsEndPicker = false },
                title = { Text("Timeline End Hour") },
                text = { TimePicker(state = state, colors = getCustomTimePickerColors()) },
                confirmButton = {
                    TextButton(onClick = {
                        val mappedHour = if (state.hour == 0) 24 else state.hour
                        endHour = mappedHour
                        showSettingsEndPicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showSettingsEndPicker = false }) { Text("Cancel") } }
            )
        }

        if (showLiveDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = settings.liveStartDateMillis)
            DatePickerDialog(
                onDismissRequest = { showLiveDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onUpdate(currentBlock.copy(secondaryContent = Json.encodeToString(settings.copy(liveStartDateMillis = it))))
                        }
                        showLiveDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showLiveDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Dialog(onDismissRequest = { showSettings = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                    Text("Timeline Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    Text("Timeline Mode:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModernChip("WEEKDAYS", settings.mode == "WEEKDAYS") { onUpdate(currentBlock.copy(secondaryContent = Json.encodeToString(settings.copy(mode = "WEEKDAYS")))) }
                        ModernChip("TEMPLATE", settings.mode == "TEMPLATE") { onUpdate(currentBlock.copy(secondaryContent = Json.encodeToString(settings.copy(mode = "TEMPLATE")))) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModernChip("LIVE", settings.mode == "LIVE") { onUpdate(currentBlock.copy(secondaryContent = Json.encodeToString(settings.copy(mode = "LIVE")))) }
                        ModernChip("CUSTOM", settings.mode == "CUSTOM") { onUpdate(currentBlock.copy(secondaryContent = Json.encodeToString(settings.copy(mode = "CUSTOM")))) }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Visible Hours:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = "${startHour.toString().padStart(2, '0')}:00",
                            onValueChange = {}, readOnly = true, modifier = Modifier.weight(1f).clickable { showSettingsStartPicker = true },
                            label = { Text("Start Hour") }, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline)
                        )
                        OutlinedTextField(
                            value = "${endHour.toString().padStart(2, '0')}:00",
                            onValueChange = {}, readOnly = true, modifier = Modifier.weight(1f).clickable { showSettingsEndPicker = true },
                            label = { Text("End Hour") }, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Snap Granularity:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf(60, 30, 15).forEach { min ->
                            ModernChip("${min}m", settings.granularityMin == min) { onUpdate(currentBlock.copy(secondaryContent = Json.encodeToString(settings.copy(granularityMin = min)))) }
                        }
                    }

                    if (settings.mode == "WEEKDAYS") {
                        Spacer(Modifier.height(16.dp))
                        Text("Start Day:", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        var startDayExpanded by remember { mutableStateOf(false) }
                        val daysList = listOf(Calendar.MONDAY to "Monday", Calendar.TUESDAY to "Tuesday", Calendar.WEDNESDAY to "Wednesday", Calendar.THURSDAY to "Thursday", Calendar.FRIDAY to "Friday", Calendar.SATURDAY to "Saturday", Calendar.SUNDAY to "Sunday")
                        ExposedDropdownMenuBox(expanded = startDayExpanded, onExpandedChange = { startDayExpanded = it }) {
                            OutlinedTextField(
                                value = daysList.find { it.first == settings.startDayOfWeek }?.second ?: "Monday",
                                onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startDayExpanded) }
                            )
                            ExposedDropdownMenu(expanded = startDayExpanded, onDismissRequest = { startDayExpanded = false }) {
                                daysList.forEach { (calValue, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = {
                                        onUpdate(currentBlock.copy(secondaryContent = Json.encodeToString(settings.copy(startDayOfWeek = calValue))))
                                        startDayExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    if (settings.mode == "CUSTOM") {
                        Spacer(Modifier.height(16.dp))
                        Text("Custom Day Labels:", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        customDays.forEachIndexed { index, day ->
                            OutlinedTextField(
                                value = day,
                                onValueChange = { newVal ->
                                    val newDays = customDays.toMutableList()
                                    newDays[index] = newVal
                                    customDays = newDays
                                },
                                label = { Text("Row ${index + 1}") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                singleLine = true
                            )
                        }
                    }

                    if (settings.mode == "LIVE") {
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showLiveDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text("Select Start Date") }
                    }

                    Spacer(Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            onDeleteBlock("", true)
                            showSettings = false
                        }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete Timeline") }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = {
                            val safeStart = minOf(startHour, endHour - 1).coerceAtLeast(0)
                            val safeEnd = maxOf(endHour, safeStart + 1).coerceAtMost(24)
                            onUpdate(currentBlock.copy(secondaryContent = Json.encodeToString(settings.copy(startHour = safeStart, endHour = safeEnd, customDays = customDays))))
                            showSettings = false
                        }) { Text("Save") }
                    }
                }
            }
        }
    }

    // --- FULL SCREEN CANVAS DIALOG ---
    val canvasCard = currentBlock.children.find { it.id == activeCanvasCardId }
    if (canvasCard != null) {
        val meta = try { Json.decodeFromString<TimelineCardMeta>(canvasCard.secondaryContent) } catch (e: Exception) { TimelineCardMeta() }
        val cardColor = try { Color(android.graphics.Color.parseColor(meta.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

        Dialog(
            onDismissRequest = { activeCanvasCardId = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 4.dp) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { activeCanvasCardId = null }) { Icon(Icons.Default.ArrowBack, "Back") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.size(12.dp).background(cardColor, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = canvasCard.content.replace("\u200B", "").ifEmpty { "Untitled Event" },
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                                    maxLines = 1,
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                )
                            }
                            IconButton(onClick = { cardToEditProps = canvasCard; isNewCard = false }) { Icon(Icons.Default.Edit, "Edit Event Properties", tint = MaterialTheme.colorScheme.primary) }
                            IconButton(onClick = {
                                val newChildren = currentBlock.children.toMutableList()
                                newChildren.removeIf { it.id == canvasCard.id }
                                onUpdate(currentBlock.copy(children = newChildren))
                                activeCanvasCardId = null
                            }) { Icon(Icons.Default.Delete, "Delete Event", tint = MaterialTheme.colorScheme.error) }
                        }
                    }

                    val childrenBlocks = canvasCard.children
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
                        if (childrenBlocks.isEmpty()) {
                            item {
                                TextButton(onClick = {
                                    val liveCard = currentBlock.children.find { it.id == activeCanvasCardId }
                                    if (liveCard != null) {
                                        val newId = java.util.UUID.randomUUID().toString()
                                        val updatedChild = liveCard.copy(children = listOf(SandboxBlock(id = newId, content = "\u200B")))
                                        val newChildren = currentBlock.children.toMutableList()
                                        val idx = newChildren.indexOfFirst { c -> c.id == updatedChild.id }
                                        if (idx != -1) {
                                            newChildren[idx] = updatedChild
                                            onUpdate(currentBlock.copy(children = newChildren))
                                            canvasFocusedBlockId = newId
                                        }
                                    }
                                }) { Text("+ Add Canvas Blocks", color = Color.Gray) }
                            }
                        } else {
                            childrenBlocks.forEachIndexed { childIndex, childBlock ->
                                val cNumberedIndex = if (childBlock.type == BlockType.NUMBER) {
                                    var cCount = 1
                                    for (i in childIndex - 1 downTo 0) { if (childrenBlocks[i].type == BlockType.NUMBER) cCount++ else break }
                                    cCount
                                } else 1

                                item(key = childBlock.id) {
                                    BlockRenderer(
                                        block = childBlock, numberIndex = cNumberedIndex, allSpaces = allSpaces, currentSpaceId = currentSpaceId, recentTableColors = recentTableColors,
                                        focusRequesters = focusRequesters, focusTargetId = canvasFocusedBlockId, allBlocks = childrenBlocks,
                                        isWidgetMode = false, isReadOnly = false,
                                        onScrollToBlock = {}, onScrollToTop = {}, onNavigateToSpace = {}, onCreateLinkedSpace = { "" },
                                        onFocusRequest = { canvasFocusedBlockId = it },
                                        onMove = { offset ->
                                            val liveCard = currentBlock.children.find { it.id == activeCanvasCardId }
                                            if (liveCard != null) {
                                                val liveChildren = liveCard.children
                                                val currentIndex = liveChildren.indexOfFirst { it.id == childBlock.id }
                                                if (currentIndex != -1) {
                                                    val newIndex = currentIndex + offset
                                                    if (newIndex in 0 until liveChildren.size) {
                                                        val newChildren = liveChildren.toMutableList()
                                                        val temp = newChildren[currentIndex]
                                                        newChildren[currentIndex] = newChildren[newIndex]
                                                        newChildren[newIndex] = temp
                                                        val uCard = liveCard.copy(children = newChildren)
                                                        val bC = currentBlock.children.toMutableList()
                                                        val cardIdx = bC.indexOfFirst { it.id == uCard.id }
                                                        if (cardIdx != -1) { bC[cardIdx] = uCard; onUpdate(currentBlock.copy(children = bC)) }
                                                    }
                                                }
                                            }
                                        },
                                        onUpdate = { updatedChild ->
                                            val liveCard = currentBlock.children.find { it.id == activeCanvasCardId }
                                            if (liveCard != null) {
                                                val liveChildren = liveCard.children.toMutableList()
                                                val safeIndex = liveChildren.indexOfFirst { it.id == updatedChild.id }
                                                if (safeIndex != -1) {
                                                    liveChildren[safeIndex] = updatedChild
                                                    val uCard = liveCard.copy(children = liveChildren)
                                                    val bC = currentBlock.children.toMutableList()
                                                    val cardIdx = bC.indexOfFirst { it.id == uCard.id }
                                                    if (cardIdx != -1) { bC[cardIdx] = uCard; onUpdate(currentBlock.copy(children = bC)) }
                                                }
                                            }
                                        },
                                        onSplitBlock = { currentContent, nextType, nextContent ->
                                            val liveCard = currentBlock.children.find { it.id == activeCanvasCardId }
                                            if (liveCard != null) {
                                                val liveChildren = liveCard.children.toMutableList()
                                                val safeIndex = liveChildren.indexOfFirst { it.id == childBlock.id }
                                                if (safeIndex != -1) {
                                                    liveChildren[safeIndex] = liveChildren[safeIndex].copy(content = currentContent)
                                                    val newBlockId = java.util.UUID.randomUUID().toString()
                                                    liveChildren.add(safeIndex + 1, SandboxBlock(id = newBlockId, type = nextType, content = nextContent))
                                                    val uCard = liveCard.copy(children = liveChildren)
                                                    val bC = currentBlock.children.toMutableList()
                                                    val cardIdx = bC.indexOfFirst { it.id == uCard.id }
                                                    if (cardIdx != -1) { bC[cardIdx] = uCard; onUpdate(currentBlock.copy(children = bC)) }
                                                    canvasFocusedBlockId = newBlockId
                                                }
                                            }
                                        },
                                        onDeleteBlock = { remainder, deleteEntirely ->
                                            val liveCard = currentBlock.children.find { it.id == activeCanvasCardId }
                                            if (liveCard != null) {
                                                val liveChildren = liveCard.children.toMutableList()
                                                val safeIndex = liveChildren.indexOfFirst { it.id == childBlock.id }
                                                if (safeIndex != -1 && safeIndex < liveChildren.size) {
                                                    if (liveChildren.size == 1 && safeIndex == 0 && (deleteEntirely || remainder.trim().replace("\u200B","").isEmpty())) {
                                                        liveChildren[0] = liveChildren[0].copy(type = BlockType.TEXT, content = "\u200B")
                                                        val uCard = liveCard.copy(children = liveChildren)
                                                        val bC = currentBlock.children.toMutableList()
                                                        val cardIdx = bC.indexOfFirst { it.id == uCard.id }
                                                        if (cardIdx != -1) { bC[cardIdx] = uCard; onUpdate(currentBlock.copy(children = bC)) }
                                                        canvasFocusedBlockId = liveChildren[0].id
                                                    } else {
                                                        if (deleteEntirely) {
                                                            val prevId = if (safeIndex > 0) liveChildren[safeIndex - 1].id else null
                                                            liveChildren.removeAt(safeIndex)
                                                            val uCard = liveCard.copy(children = liveChildren)
                                                            val bC = currentBlock.children.toMutableList()
                                                            val cardIdx = bC.indexOfFirst { it.id == uCard.id }
                                                            if (cardIdx != -1) { bC[cardIdx] = uCard; onUpdate(currentBlock.copy(children = bC)) }
                                                            if (prevId != null) canvasFocusedBlockId = prevId
                                                        } else if (safeIndex > 0) {
                                                            val prevChild = liveChildren[safeIndex - 1]
                                                            val isPrevTextBased = prevChild.type in listOf(BlockType.TEXT, BlockType.HEADING, BlockType.HEADING_2, BlockType.HEADING_3, BlockType.HEADING_4, BlockType.BULLET, BlockType.NUMBER, BlockType.TODO, BlockType.QUOTE, BlockType.CALLOUT, BlockType.TOGGLE)
                                                            if (isPrevTextBased) {
                                                                val prevId = prevChild.id
                                                                liveChildren[safeIndex - 1] = prevChild.copy(content = prevChild.content + remainder)
                                                                liveChildren.removeAt(safeIndex)
                                                                val uCard = liveCard.copy(children = liveChildren)
                                                                val bC = currentBlock.children.toMutableList()
                                                                val cardIdx = bC.indexOfFirst { it.id == uCard.id }
                                                                if (cardIdx != -1) { bC[cardIdx] = uCard; onUpdate(currentBlock.copy(children = bC)) }
                                                                canvasFocusedBlockId = prevId
                                                            } else {
                                                                liveChildren.removeAt(safeIndex - 1)
                                                                val uCard = liveCard.copy(children = liveChildren)
                                                                val bC = currentBlock.children.toMutableList()
                                                                val cardIdx = bC.indexOfFirst { it.id == uCard.id }
                                                                if (cardIdx != -1) { bC[cardIdx] = uCard; onUpdate(currentBlock.copy(children = bC)) }
                                                            }
                                                        } else if (safeIndex == 0) {
                                                            if (deleteEntirely || remainder.trim().replace("\u200B","").isEmpty()) {
                                                                liveChildren.removeAt(0)
                                                                val uCard = liveCard.copy(children = liveChildren)
                                                                val bC = currentBlock.children.toMutableList()
                                                                val cardIdx = bC.indexOfFirst { it.id == uCard.id }
                                                                if (cardIdx != -1) { bC[cardIdx] = uCard; onUpdate(currentBlock.copy(children = bC)) }
                                                            } else {
                                                                liveChildren[0] = liveChildren[0].copy(content = "\u200B" + remainder)
                                                                val uCard = liveCard.copy(children = liveChildren)
                                                                val bC = currentBlock.children.toMutableList()
                                                                val cardIdx = bC.indexOfFirst { it.id == uCard.id }
                                                                if (cardIdx != -1) { bC[cardIdx] = uCard; onUpdate(currentBlock.copy(children = bC)) }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // TIMELINE GRID VIEW
    // =========================================================================
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(currentBlock.content.replace("\u200B", "").ifEmpty { "Weekly Schedule" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = {
                        // --- FIX: Start exactly at the beginning of the visible timeline! ---
                        val newEvent = SandboxBlock(type = BlockType.TIMELINE_CARD, content = "\u200BNew Event", secondaryContent = Json.encodeToString(TimelineCardMeta(startTimeMin = settings.startHour * 60)))
                        cardToEditProps = newEvent
                        isNewCard = true
                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, "Add Event", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = { showSettings = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, "Settings", tint = Color.Gray) }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.width(80.dp).background(MaterialTheme.colorScheme.surface)) {
                    Box(modifier = Modifier.height(30.dp).fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f)))
                    dayLabels.forEachIndexed { index, label ->
                        val bg = if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                        Box(modifier = Modifier.height(rowHeight).fillMaxWidth().background(bg).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f)), contentAlignment = Alignment.Center) {
                            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).horizontalScroll(horizontalScrollState)) {
                    Column {
                        val hoursCount = settings.endHour - settings.startHour

                        Row(modifier = Modifier.height(30.dp).background(MaterialTheme.colorScheme.surface)) {
                            for (hour in settings.startHour until settings.endHour) {
                                Box(modifier = Modifier.width(hourWidth).fillMaxHeight()) {
                                    Text("$hour:00", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 4.dp))

                                    if (hour == settings.endHour - 1) {
                                        Text("${settings.endHour}:00", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 4.dp))
                                    }

                                    val ticksPerHour = 60 / settings.granularityMin
                                    val tickSpacingPx = hourWidth / ticksPerHour
                                    for (t in 0 until ticksPerHour) {
                                        val tickHeight = if (t == 0) 12.dp else 6.dp
                                        Box(modifier = Modifier.offset(x = tickSpacingPx * t).width(1.dp).height(tickHeight).background(Color.Gray.copy(alpha=0.5f)).align(Alignment.BottomStart))
                                    }

                                    if (hour == settings.endHour - 1) {
                                        Box(modifier = Modifier.align(Alignment.BottomEnd).width(1.dp).height(12.dp).background(Color.Gray.copy(alpha=0.5f)))
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.width(hourWidth * hoursCount).height(rowHeight * 7)) {
                            Column {
                                for (i in 0..6) {
                                    val bg = if (i % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                                    Box(modifier = Modifier.fillMaxWidth().height(rowHeight).background(bg).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.2f)))
                                }
                            }

                            currentBlock.children.forEach { childCard ->
                                val meta = try { Json.decodeFromString<TimelineCardMeta>(childCard.secondaryContent) } catch (e: Exception) { TimelineCardMeta() }
                                val cardStartTotalMin = meta.startTimeMin
                                val visibleStartMin = settings.startHour * 60

                                if (cardStartTotalMin >= visibleStartMin && cardStartTotalMin < settings.endHour * 60) {
                                    val cardColor = try { Color(android.graphics.Color.parseColor(meta.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

                                    val cardXDp = minuteWidthDp * (cardStartTotalMin - visibleStartMin)
                                    val cardYDp = rowHeight * meta.dayIndex
                                    val cardWidthDp = minuteWidthDp * meta.durationMin

                                    Box(
                                        modifier = Modifier
                                            .offset(x = cardXDp, y = cardYDp)
                                            .width(cardWidthDp)
                                            .height(rowHeight - 8.dp)
                                            .padding(top = 8.dp)
                                            .background(cardColor, RoundedCornerShape(8.dp))
                                            .clickable { activeCanvasCardId = childCard.id }
                                    ) {
                                        Text(text = childCard.content.replace("\u200B", ""), style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp), maxLines = 1)
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