package com.example.loom.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.loom.reminders.ReminderScheduler
import com.example.loom.reminders.ReminderSettings

/** Settings card for configurable reminders. [onChanged] re-arms the alarms after each edit. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsSection(onChanged: () -> Unit) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(ReminderSettings.load(context)) }
    var canExact by remember { mutableStateOf(ReminderScheduler.canScheduleExact(context)) }

    // Re-check the exact-alarm permission when returning from system settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) canExact = ReminderScheduler.canScheduleExact(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun update(transform: (ReminderSettings) -> ReminderSettings) {
        settings = transform(settings)
        ReminderSettings.save(context, settings)
        onChanged()
    }

    var editingTime by remember { mutableStateOf<TimeEdit?>(null) }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            if (!canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text("Allow exact timing", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Without it, Android may deliver reminders several minutes late.") },
                    leadingContent = { Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = {
                        TextButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                        }) { Text("Allow") }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
            }

            ReminderRow("Daily recap", "Evening nudge to log your day", settings.recapEnabled, { on -> update { it.copy(recapEnabled = on) } }) {
                TimeChip(settings.recapHour, settings.recapMinute) { editingTime = TimeEdit(settings.recapHour, settings.recapMinute) { h, m -> update { s -> s.copy(recapHour = h, recapMinute = m) } } }
            }
            ReminderRow("Water", "Every ${settings.waterIntervalHours} h, skipped once your goal is met", settings.waterEnabled, { on -> update { it.copy(waterEnabled = on) } }) {
                Text("From", style = MaterialTheme.typography.labelMedium)
                TimeChip(settings.waterStartHour) { editingTime = TimeEdit(settings.waterStartHour) { h, _ -> update { s -> s.copy(waterStartHour = h) } } }
                Text("to", style = MaterialTheme.typography.labelMedium)
                TimeChip(settings.waterEndHour) { editingTime = TimeEdit(settings.waterEndHour) { h, _ -> update { s -> s.copy(waterEndHour = h) } } }
                listOf(1, 2, 3).forEach { n ->
                    FilterChip(selected = settings.waterIntervalHours == n, onClick = { update { it.copy(waterIntervalHours = n) } }, label = { Text("${n}h") })
                }
            }
            ReminderRow("Supplements", "At each item's time of use, if not taken yet", settings.supplementsEnabled, { on -> update { it.copy(supplementsEnabled = on) } }) {
                LabeledTime("Morning", settings.morningHour) { editingTime = TimeEdit(settings.morningHour) { h, _ -> update { s -> s.copy(morningHour = h) } } }
                LabeledTime("Afternoon", settings.afternoonHour) { editingTime = TimeEdit(settings.afternoonHour) { h, _ -> update { s -> s.copy(afternoonHour = h) } } }
                LabeledTime("Evening", settings.eveningHour) { editingTime = TimeEdit(settings.eveningHour) { h, _ -> update { s -> s.copy(eveningHour = h) } } }
                LabeledTime("Before bed", settings.beforeBedHour) { editingTime = TimeEdit(settings.beforeBedHour) { h, _ -> update { s -> s.copy(beforeBedHour = h) } } }
            }
            ReminderRow("Skincare", "Morning and evening, if the routine isn't logged", settings.skincareEnabled, { on -> update { it.copy(skincareEnabled = on) } }) {
                LabeledTime("AM", settings.skincareAmHour) { editingTime = TimeEdit(settings.skincareAmHour) { h, _ -> update { s -> s.copy(skincareAmHour = h) } } }
                LabeledTime("PM", settings.skincarePmHour) { editingTime = TimeEdit(settings.skincarePmHour) { h, _ -> update { s -> s.copy(skincarePmHour = h) } } }
            }
            ReminderRow("Task deadlines", "Tasks due today and overdue", settings.tasksEnabled, { on -> update { it.copy(tasksEnabled = on) } }) {
                TimeChip(settings.tasksHour) { editingTime = TimeEdit(settings.tasksHour) { h, _ -> update { s -> s.copy(tasksHour = h) } } }
            }
            ReminderRow("Fasting", "When your fasting goal is reached", settings.fastingEnabled, { on -> update { it.copy(fastingEnabled = on) } }, divider = false)
        }
    }

    editingTime?.let { edit ->
        val state = rememberTimePickerState(initialHour = edit.hour, initialMinute = edit.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { editingTime = null },
            title = { Text("Reminder time") },
            text = { TimePicker(state = state) },
            confirmButton = { TextButton(onClick = { edit.onPicked(state.hour, state.minute); editingTime = null }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { editingTime = null }) { Text("Cancel") } }
        )
    }
}

private class TimeEdit(val hour: Int, val minute: Int = 0, val onPicked: (Int, Int) -> Unit)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ReminderRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    divider: Boolean = true,
    options: (@Composable FlowRowScope.() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        if (enabled && options != null) {
            FlowRow(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.Center,
                content = options
            )
        }
    }
    if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun TimeChip(hour: Int, minute: Int = 0, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text("%02d:%02d".format(hour, minute)) })
}

@Composable
private fun LabeledTime(label: String, hour: Int, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text("$label %02d:00".format(hour)) })
}
