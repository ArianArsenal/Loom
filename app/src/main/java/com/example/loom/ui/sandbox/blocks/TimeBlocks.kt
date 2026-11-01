package com.example.loom.ui.sandbox.blocks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.models.SandboxBlock
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Suppress("unused", "DiscouragedApi")
@Composable
fun TimerBlockRenderer(
    block: SandboxBlock,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var showBlockMenu by remember { mutableStateOf(false) }
    var isWorkMode by remember { mutableStateOf(true) }
    val maxTime = if (isWorkMode) 25 * 60 else 5 * 60
    var timeLeft by remember { mutableIntStateOf(maxTime) }
    var isRunning by remember { mutableStateOf(false) }

    // --- UPDATED: Mapped to your 4 ambient tracks ---
    var selectedAmbient by remember { mutableStateOf("None") }
    val ambientSounds = listOf("None", "Lofi", "Airplane", "Rain", "Cafe")

    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var ambientVolume by remember { mutableFloatStateOf(0.5f) }

    // OPTIMIZATION: Safely release the ambient player memory when the timer is deleted or screen closed
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // --- ENGINE 1: The Ambient Native Looper ---
    LaunchedEffect(isRunning, selectedAmbient) {
        if (isRunning && selectedAmbient != "None") {
            mediaPlayer?.release() // Clean up previous track if switching

            val rawFileName = when (selectedAmbient) {
                "Lofi" -> "lofi"
                "Airplane" -> "airplane"
                "Rain" -> "rain"
                "Cafe" -> "cafe"
                else -> ""
            }

            val resId = context.resources.getIdentifier(rawFileName, "raw", context.packageName)

            if (resId != 0) {
                try {
                    mediaPlayer = android.media.MediaPlayer.create(context, resId).apply {
                        isLooping = true // Defers looping optimization to Android's native AudioFlinger
                        setVolume(ambientVolume, ambientVolume)
                        start()
                    }
                } catch (e: Exception) {
                    // Fail gracefully if the raw file isn't loaded properly
                }
            }
        } else {
            mediaPlayer?.pause()
        }
    }

    // Dynamically update volume without restarting the track
    LaunchedEffect(ambientVolume) {
        mediaPlayer?.setVolume(ambientVolume, ambientVolume)
    }

    // --- ENGINE 2: The Countdown & Throwaway Chime Trigger ---
    LaunchedEffect(isRunning, isWorkMode) {
        while (isRunning && timeLeft > 0) {
            delay(1000.milliseconds)
            timeLeft--
        }

        // When timer hits 0 naturally while running
        if (isRunning && timeLeft == 0) {
            isRunning = false

            // Fire the completion chime using your 'chimes' file
            val chimeId = context.resources.getIdentifier("chimes", "raw", context.packageName)
            if (chimeId != 0) {
                try {
                    val chimePlayer = android.media.MediaPlayer.create(context, chimeId)
                    // OPTIMIZATION: Instantly release the memory the millisecond the chime stops
                    chimePlayer.setOnCompletionListener { it.release() }
                    chimePlayer.start()
                } catch (e: Exception) {
                    // Fail gracefully
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Pomodoro Timer", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
        }

        DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
            DropdownMenuItem(text = { Text("Delete Timer", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).padding(4.dp)) {
            TextButton(onClick = { isWorkMode = true; timeLeft = 25 * 60; isRunning = false }, modifier = Modifier.background(if (isWorkMode) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(50))) { Text("Work", color = if (isWorkMode) Color.White else Color.Gray) }
            TextButton(onClick = { isWorkMode = false; timeLeft = 5 * 60; isRunning = false }, modifier = Modifier.background(if (!isWorkMode) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(50))) { Text("Break", color = if (!isWorkMode) Color.White else Color.Gray) }
        }

        Spacer(Modifier.height(16.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            CircularProgressIndicator(progress = { timeLeft.toFloat() / maxTime.toFloat() }, modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary, strokeWidth = 6.dp, trackColor = MaterialTheme.colorScheme.outlineVariant)
            Text(String.format(Locale.US, "%02d:%02d", timeLeft / 60, timeLeft % 60), fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        IconButton(onClick = { isRunning = !isRunning }, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) {
            Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Ambient Audio Control UI ---
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Headset, "Ambient Audio", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ambient Sound", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))

                // Allow the chips to scroll horizontally if the user adds more tracks later
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ambientSounds.forEach { ambient ->
                        @OptIn(ExperimentalMaterial3Api::class)
                        FilterChip(
                            selected = selectedAmbient == ambient,
                            onClick = { selectedAmbient = ambient },
                            label = { Text(ambient, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                AnimatedVisibility(
                    visible = selectedAmbient != "None",
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Volume", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = ambientVolume,
                                onValueChange = { ambientVolume = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Serializable
data class CountdownMeta(
    val targetTimeMillis: Long = 0L,
    val title: String = "Countdown"
)

class ConfettiParticle {
    val xOffset = (Math.random() - 0.5).toFloat() * 1.5f
    val yOffset = (Math.random() - 0.5).toFloat() * 1.5f
    val radius = (Math.random() * 15f + 5f).toFloat()
    val color = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta).random()
}

@Composable
fun ConfettiBurst() {
    val particles = remember { List(60) { ConfettiParticle() } }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(3000, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val x = size.width / 2 + (p.xOffset * progress.value * size.width)
            val y = size.height / 2 + (p.yOffset * progress.value * size.height) + (progress.value * progress.value * 300f)
            val alpha = (1f - progress.value).coerceIn(0f, 1f)
            drawCircle(p.color.copy(alpha = alpha), radius = p.radius * (1f - progress.value * 0.5f), center = Offset(x, y))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownBlockRenderer(
    block: SandboxBlock,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    var showBlockMenu by remember { mutableStateOf(false) }

    val meta = try {
        if (block.secondaryContent.isNotBlank()) {
            Json.decodeFromString<CountdownMeta>(block.secondaryContent)
        } else {
            val legacyTime = block.content.toLongOrNull() ?: 0L
            CountdownMeta(targetTimeMillis = legacyTime)
        }
    } catch (_: Exception) { CountdownMeta() }

    var timeLeft by remember { mutableLongStateOf(0L) }
    var isTimeSinceMode by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(meta.targetTimeMillis) {
        if (meta.targetTimeMillis > 0) {
            var hasTriggeredConfetti = false
            while (true) {
                val difference = meta.targetTimeMillis - System.currentTimeMillis()
                if (difference > 0) {
                    timeLeft = difference
                    isTimeSinceMode = false
                } else {
                    timeLeft = kotlin.math.abs(difference)
                    isTimeSinceMode = true

                    if (!hasTriggeredConfetti && timeLeft < 2000) {
                        hasTriggeredConfetti = true
                        showConfetti = true
                        delay(3500.milliseconds)
                        showConfetti = false
                    }
                }
                delay(1000.milliseconds)
            }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempDateMillis by remember { mutableLongStateOf(0L) }

    if (showDatePicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { tempDateMillis = it }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dateState) }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = tempDateMillis
                    calendar.set(Calendar.HOUR_OF_DAY, timeState.hour)
                    calendar.set(Calendar.MINUTE, timeState.minute)
                    calendar.set(Calendar.SECOND, 0)

                    val newMeta = meta.copy(targetTimeMillis = calendar.timeInMillis)
                    onUpdate(block.copy(secondaryContent = Json.encodeToString(newMeta)))
                    showTimePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    var showTitleDialog by remember { mutableStateOf(false) }
    if (showTitleDialog) {
        var editTitle by remember { mutableStateOf(meta.title) }
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            title = { Text("Countdown Name") },
            text = {
                OutlinedTextField(
                    value = editTitle, onValueChange = { editTitle = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newMeta = meta.copy(title = editTitle)
                    onUpdate(block.copy(secondaryContent = Json.encodeToString(newMeta)))
                    showTitleDialog = false
                }) { Text("Save") }
            }
        )
    }

    Box {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val headerText = if (meta.targetTimeMillis == 0L) "New Countdown" else if (isTimeSinceMode) "Time Since: ${meta.title}" else "Countdown: ${meta.title}"
                    val headerColor = if (isTimeSinceMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showTitleDialog = true }) {
                        if (showConfetti) {
                            Icon(Icons.Default.Celebration, "Complete!", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(headerText, fontWeight = FontWeight.Bold, color = headerColor, maxLines = 1)
                    }

                    IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                }

                DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                    DropdownMenuItem(text = { Text("Rename Countdown") }, onClick = { showTitleDialog = true; showBlockMenu = false })
                    DropdownMenuItem(text = { Text("Change Target Date") }, onClick = { showDatePicker = true; showBlockMenu = false })
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (meta.targetTimeMillis == 0L) {
                    Button(onClick = { showDatePicker = true }) { Text("Set Target Date & Time") }
                } else {
                    val days = TimeUnit.MILLISECONDS.toDays(timeLeft)
                    val hours = TimeUnit.MILLISECONDS.toHours(timeLeft) % 24
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {

                        @Composable fun TimeBox(value: Long, label: String) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = String.format(Locale.US, "%02d", value),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }

                        TimeBox(days, "DAYS")
                        Text(":", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.outline)
                        TimeBox(hours, "HRS")
                        Text(":", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.outline)
                        TimeBox(minutes, "MIN")
                        Text(":", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.outline)
                        TimeBox(seconds, "SEC")
                    }
                }
            }
        }

        if (showConfetti) {
            ConfettiBurst()
        }
    }
}
