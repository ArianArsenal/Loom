package com.example.loom.reminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.loom.R
import com.example.loom.data.local.dao.TrackerDao
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.data.repository.TrackerRepository
import com.example.loom.data.repository.addJsonIds
import com.example.loom.data.repository.decodeList
import com.example.loom.ui.health.SupplementItem
import com.example.loom.ui.health.isScheduledOn
import com.example.loom.util.DateUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Fires reminders (re-arming the next one each time), handles notification action buttons and
 * re-arms everything after reboot or clock/time-zone changes.
 */
class ReminderReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderEntryPoint {
        fun trackerRepository(): TrackerRepository
        fun trackerDao(): TrackerDao
        fun userPreferences(): UserPreferences
    }

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val deps = EntryPointAccessors.fromApplication(app, ReminderEntryPoint::class.java)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_FIRE -> {
                        val kind = runCatching { ReminderKind.valueOf(intent.getStringExtra(EXTRA_KIND) ?: "") }.getOrNull()
                        if (kind != null) fire(app, deps, kind)
                        ReminderScheduler.rescheduleAll(app, deps.userPreferences())
                    }
                    ACTION_ADD_WATER -> {
                        deps.trackerRepository().addWater(deps.userPreferences().defaultWaterIncrementFlow.first())
                        NotificationManagerCompat.from(app).cancel(ReminderKind.WATER.notificationId)
                    }
                    ACTION_SUPPLEMENTS_TAKEN -> {
                        val ids = intent.getStringArrayExtra(EXTRA_IDS)?.toList().orEmpty()
                        deps.trackerRepository().editRecord { it.copy(completedSupplementsJson = addJsonIds(it.completedSupplementsJson, ids)) }
                        NotificationManagerCompat.from(app).cancel(ReminderKind.SUPPLEMENTS.notificationId)
                    }
                    ACTION_TASK_DONE -> {
                        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
                        val task = deps.trackerDao().getAllTasks().first().find { it.id == taskId }
                        if (task != null && !task.isCompleted) {
                            deps.trackerDao().updateTask(task.copy(status = "DONE", isCompleted = true, completionDateMillis = System.currentTimeMillis()))
                        }
                        NotificationManagerCompat.from(app).cancel(ReminderKind.TASKS.notificationId)
                    }
                    // Boot, app update, clock or time-zone change: alarms must be re-armed
                    else -> ReminderScheduler.rescheduleAll(app, deps.userPreferences())
                }
            } catch (e: Exception) {
                android.util.Log.e("Reminders", "Reminder handling failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun fire(context: Context, deps: ReminderEntryPoint, kind: ReminderKind) {
        val settings = ReminderSettings.load(context)
        val prefs = deps.userPreferences()
        val todayId = DateUtils.getTodayDateId()
        val record = deps.trackerDao().getDailyRecordSync(todayId)

        when (kind) {
            ReminderKind.WATER -> {
                val goal = prefs.waterDailyGoalFlow.first()
                val intake = record?.waterIntakeMl ?: 0
                if (goal > 0 && intake >= goal) return // goal already met
                val increment = prefs.defaultWaterIncrementFlow.first()
                notify(context, kind, R.drawable.bottle, "Hydration check", "You've had $intake of $goal ml today.",
                    action = Triple("+$increment ml", ACTION_ADD_WATER, null))
            }

            ReminderKind.SUPPLEMENTS -> {
                val hour = LocalTime.now().hour
                val completed = decodeList<String>(record?.completedSupplementsJson)
                val due = decodeList<SupplementItem>(prefs.supplementsRosterFlow.first()).filter {
                    it.isScheduledOn(DateUtils.getLogicalToday()) && settings.hourForTimeOfUse(it.timeOfUse) == hour && it.id !in completed
                }
                if (due.isEmpty()) return
                val names = due.joinToString { if (it.dosage.isBlank()) it.name else "${it.name} (${it.dosage})" }
                notify(context, kind, R.drawable.pills, "Time for your ${due.first().timeOfUse.lowercase()} stack", names,
                    action = Triple("Mark taken", ACTION_SUPPLEMENTS_TAKEN, Intent().putExtra(EXTRA_IDS, due.map { it.id }.toTypedArray())))
            }

            ReminderKind.SKINCARE -> {
                val isMorning = LocalTime.now().hour == settings.skincareAmHour
                val routines = deps.trackerDao().getAllSkincareRoutines().first()
                val keywords = if (isMorning) listOf("am", "morning", "☀") else listOf("pm", "night", "evening", "🌙")
                val matching = routines.filter { r -> keywords.any { r.name.contains(it, ignoreCase = true) } }.ifEmpty { routines }
                val done = decodeList<String>(record?.completedSkincareRoutinesJson)
                if (matching.isNotEmpty() && matching.all { it.name in done }) return // already logged
                notify(context, kind, R.drawable.dropper, if (isMorning) "Morning skincare" else "Evening skincare",
                    matching.joinToString { it.name }.ifBlank { "Time for your routine." })
            }

            ReminderKind.TASKS -> {
                val due = deps.trackerDao().getDueTasks(todayId)
                if (due.isEmpty()) return
                val overdue = due.count { it.targetDate < todayId }
                val title = buildString {
                    append("${due.size - overdue} due today")
                    if (overdue > 0) append(", $overdue overdue")
                }
                val single = due.singleOrNull()?.takeIf { !it.isRecurring }
                notify(context, kind, R.drawable.checklist, title, due.take(5).joinToString("\n") { "• ${it.title}" },
                    action = single?.let { Triple("Done", ACTION_TASK_DONE, Intent().putExtra(EXTRA_TASK_ID, it.id)) })
            }

            ReminderKind.FASTING -> {
                if (!prefs.isFastingFlow.first()) return
                val hours = prefs.fastingGoalHoursFlow.first()
                notify(context, kind, R.drawable.clock, "Fasting goal reached", "You've completed your $hours-hour fast.")
            }

            ReminderKind.RECAP -> notify(context, kind, R.drawable.bell, "How was your day?", "Take a minute to log water, meals and workouts.")
        }
    }

    @SuppressLint("MissingPermission") // checked via areNotificationsEnabled
    private fun notify(
        context: Context,
        kind: ReminderKind,
        icon: Int,
        title: String,
        text: String,
        action: Triple<String, String, Intent?>? = null
    ) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val builder = NotificationCompat.Builder(context, kind.channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(open?.let { PendingIntent.getActivity(context, kind.ordinal, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) })

        action?.let { (label, actionName, extras) ->
            val intent = Intent(context, ReminderReceiver::class.java).setAction(actionName)
            extras?.extras?.let { intent.putExtras(it) }
            val pi = PendingIntent.getBroadcast(context, 100 + kind.ordinal, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            builder.addAction(0, label, pi)
        }
        nm.notify(kind.notificationId, builder.build())
    }

    companion object {
        const val ACTION_FIRE = "com.example.loom.reminders.FIRE"
        const val ACTION_ADD_WATER = "com.example.loom.reminders.ADD_WATER"
        const val ACTION_SUPPLEMENTS_TAKEN = "com.example.loom.reminders.SUPPLEMENTS_TAKEN"
        const val ACTION_TASK_DONE = "com.example.loom.reminders.TASK_DONE"
        const val EXTRA_KIND = "kind"
        const val EXTRA_IDS = "ids"
        const val EXTRA_TASK_ID = "taskId"
    }
}
