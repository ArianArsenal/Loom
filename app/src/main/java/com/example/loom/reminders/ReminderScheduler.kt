package com.example.loom.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.loom.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId

enum class ReminderKind(val channelId: String, val channelName: String, val notificationId: Int) {
    WATER("reminder_water", "Water reminders", 2001),
    SUPPLEMENTS("reminder_supplements", "Supplement reminders", 2002),
    SKINCARE("reminder_skincare", "Skincare reminders", 2003),
    TASKS("reminder_tasks", "Task deadlines", 2004),
    FASTING("reminder_fasting", "Fasting", 2005),
    RECAP("reminder_recap", "Daily recap", 2006)
}

/**
 * One alarm per reminder kind, always armed for that kind's next occurrence. Each firing re-arms
 * the next one, so a reminder that isn't needed (goal met, already logged) is simply skipped.
 * Exact alarms are used when the user allowed them, otherwise inexact (may be a few minutes late).
 */
object ReminderScheduler {

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        ReminderKind.entries.forEach { kind ->
            nm.createNotificationChannel(NotificationChannel(kind.channelId, kind.channelName, NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    suspend fun rescheduleAll(context: Context, userPreferences: UserPreferences) {
        val s = ReminderSettings.load(context)
        val now = LocalDateTime.now()

        schedule(context, ReminderKind.WATER, if (s.waterEnabled) {
            val hours = (s.waterStartHour..s.waterEndHour step s.waterIntervalHours.coerceAtLeast(1)).map { it to 0 }
            nextOccurrence(now, hours)
        } else null)

        schedule(context, ReminderKind.SUPPLEMENTS, if (s.supplementsEnabled) {
            nextOccurrence(now, listOf(s.morningHour, s.afternoonHour, s.eveningHour, s.beforeBedHour).distinct().map { it to 0 })
        } else null)

        schedule(context, ReminderKind.SKINCARE, if (s.skincareEnabled) {
            nextOccurrence(now, listOf(s.skincareAmHour to 0, s.skincarePmHour to 0))
        } else null)

        schedule(context, ReminderKind.TASKS, if (s.tasksEnabled) nextOccurrence(now, listOf(s.tasksHour to 0)) else null)

        schedule(context, ReminderKind.RECAP, if (s.recapEnabled) nextOccurrence(now, listOf(s.recapHour to s.recapMinute)) else null)

        val fastingEnd = if (s.fastingEnabled && userPreferences.isFastingFlow.first()) {
            val start = userPreferences.fastingStartTimeFlow.first()
            val goalHours = userPreferences.fastingGoalHoursFlow.first()
            (start + goalHours * 3_600_000L).takeIf { start > 0 && it > System.currentTimeMillis() }
        } else null
        schedule(context, ReminderKind.FASTING, fastingEnd)
    }

    /** Next time today or tomorrow matching one of [times] (hour, minute), strictly after [now]. */
    private fun nextOccurrence(now: LocalDateTime, times: List<Pair<Int, Int>>): Long? {
        val candidates = times.filter { (h, m) -> h in 0..23 && m in 0..59 }.flatMap { (h, m) ->
            val today = now.toLocalDate().atTime(h, m)
            listOf(today, today.plusDays(1))
        }.filter { it.isAfter(now) }
        return candidates.minOrNull()?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    private fun schedule(context: Context, kind: ReminderKind, triggerAtMillis: Long?) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = firePendingIntent(context, kind)
        if (triggerAtMillis == null) {
            am.cancel(pi)
            return
        }
        try {
            if (canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (_: SecurityException) {
            // Exact-alarm permission revoked between the check and the call
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun firePendingIntent(context: Context, kind: ReminderKind): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE)
            .putExtra(ReminderReceiver.EXTRA_KIND, kind.name)
        return PendingIntent.getBroadcast(context, kind.ordinal, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
