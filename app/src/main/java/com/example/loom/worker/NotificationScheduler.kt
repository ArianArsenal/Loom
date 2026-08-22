package com.example.loom.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleDailyReminder(context: Context, hourOfDay: Int, minute: Int) {
        val now = LocalDateTime.now()
        var target = now.withHour(hourOfDay).withMinute(minute).withSecond(0).withNano(0)

        // --- OPTIMIZATION: Using fast, modern java.time instead of the heavy Calendar API ---
        if (now.isAfter(target) || now.isEqual(target)) {
            target = target.plusDays(1)
        }

        val initialDelayMillis = ChronoUnit.MILLIS.between(now, target)

        // Set up the work to repeat every 24 hours, starting after the initial delay
        val dailyWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.UPDATE, // Replaces any existing schedule with the same name
            dailyWorkRequest
        )
    }
}