package com.example.loom.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.loom.data.repository.TrackerRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // --- HILT ENTRY POINT ---
    // This allows us to safely inject the repository into a WorkManager class
    // without needing to add extra Hilt-Work Gradle dependencies!
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun trackerRepository(): TrackerRepository
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch the Repository via Hilt
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                WorkerEntryPoint::class.java
            )
            val repository = entryPoint.trackerRepository()

            // 2. Execute the heavy database cleanup
            repository.cleanupOldDoneTasks()

            Result.success()
        } catch (_: Exception) {
            // If the database is locked or busy, tell WorkManager to try again later
            Result.retry()
        }
    }
}