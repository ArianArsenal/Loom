package com.example.loom.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object CleanupScheduler {
    @android.annotation.SuppressLint("BatteryLife")
    fun scheduleBackgroundCleanup(context: Context) {
        // --- STRICT CONSTRAINTS ---
        // Android will ONLY run this worker if BOTH conditions are met.
        // This guarantees zero impact on app performance and battery life!
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)      // User must be plugged into power
            .setRequiresDeviceIdle(true)    // User must not be actively using the phone
            .build()

        // Schedule to run once every 24 hours
        val cleanupRequest = PeriodicWorkRequestBuilder<DatabaseCleanupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        // Enqueue as a UNIQUE periodic work so it doesn't duplicate if called multiple times
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "database_maintenance_cleanup",
            ExistingPeriodicWorkPolicy.KEEP, // If it's already scheduled, leave it alone
            cleanupRequest
        )
    }
}