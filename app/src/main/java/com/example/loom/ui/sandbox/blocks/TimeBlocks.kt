package com.example.loom.ui.sandbox.blocks

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.models.SandboxBlock
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.concurrent.TimeUnit

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

    var selectedAmbient by remember { mutableStateOf("None") }
    val ambients = listOf("None", "Rain", "Forest", "Focus")
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer?.release() }
    }

    LaunchedEffect(isRunning, selectedAmbient) {
        if (isRunning && selectedAmbient != "None") {
            mediaPlayer?.release()
            val resId = when(selectedAmbient) {
                "Rain" -> context.resources.getIdentifier("rain", "raw", context.packageName)
                "Forest" -> context.resources.getIdentifier("forest", "raw", context.packageName)
                "Focus" -> context.resources.getIdentifier("focus", "raw", context.packageName)
                else -> 0
            }
            if (resId != 0) {
                mediaPlayer = android.media.MediaPlayer.create(context, resId).apply {
                    isLooping = true
                    start()
                }
            }
        } else {
            mediaPlayer?.pause()
        }
    }

    LaunchedEffect(isRunning, isWorkMode) {
        while (isRunning && timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        if (timeLeft == 0) isRunning = false
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
            Text(String.format("%02d:%02d", timeLeft / 60, timeLeft % 60), fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        IconButton(onClick = { isRunning = !isRunning }, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) {
            Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Headset, "Ambient Audio", tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            ambients.forEach { ambient ->
                @OptIn(ExperimentalMaterial3Api::class)
                FilterChip(
                    selected = selectedAmbient == ambient,
                    onClick = { selectedAmbient = ambient },
                    label = { Text(ambient, fontSize = 12.sp) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun CountdownBlockRenderer(
    block: SandboxBlock,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var showBlockMenu by remember { mutableStateOf(false) }
    var targetTime by remember { mutableLongStateOf(block.content.toLongOrNull() ?: 0L) }
    var timeLeft by remember { mutableLongStateOf(0L) }

    LaunchedEffect(targetTime) {
        if (targetTime > 0) {
            while (true) {
                val remaining = targetTime - System.currentTimeMillis()
                timeLeft = if (remaining > 0) remaining else 0L
                delay(1000)
            }
        }
    }

    val openDatePicker = {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                val newTime = calendar.timeInMillis
                targetTime = newTime
                onUpdate(block.copy(content = newTime.toString()))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Countdown", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options") }
            }

            DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (targetTime == 0L) {
                Button(onClick = openDatePicker) { Text("Set Target Date") }
            } else {
                val days = TimeUnit.MILLISECONDS.toDays(timeLeft)
                val hours = TimeUnit.MILLISECONDS.toHours(timeLeft) % 24
                val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    @Composable fun TimeBox(value: Long, label: String) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(String.format("%02d", value), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    TimeBox(days, "DAYS")
                    Text(":", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    TimeBox(hours, "HRS")
                    Text(":", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    TimeBox(minutes, "MIN")
                    Text(":", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    TimeBox(seconds, "SEC")
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = openDatePicker) { Text("Change Date") }
            }
        }
    }
}