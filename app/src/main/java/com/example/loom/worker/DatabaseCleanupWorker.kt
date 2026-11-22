package com.example.loom.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.loom.data.backup.MediaCleaner
import com.example.loom.data.local.dao.SandboxDao
import com.example.loom.data.local.dao.TrackerDao
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
        fun sandboxDao(): SandboxDao
        fun trackerDao(): TrackerDao
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch the Repository via Hilt
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                WorkerEntryPoint::class.java
            )
            // Done tasks used to be deleted here after 24h; they are now kept (hidden on Home instead)
            // because Calendar history and monthly stats rely on them.

            // Remove copied media that no block or note references any more
            MediaCleaner.collectGarbage(applicationContext, entryPoint.sandboxDao(), entryPoint.trackerDao())

            Result.success()
        } catch (_: Exception) {
            // If the database is locked or busy, tell WorkManager to try again later
            Result.retry()
        }
    }
}