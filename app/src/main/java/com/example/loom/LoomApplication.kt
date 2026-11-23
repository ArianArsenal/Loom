package com.example.loom

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.loom.data.backup.BackupManager
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.example.loom.util.EmergencyCrashHandler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LoomApplication : Application(), ImageLoaderFactory {

    // Inject the databases globally so the Crash Handler can reach them
    @Inject lateinit var backupManager: BackupManager
    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var trackerRepository: com.example.loom.data.repository.TrackerRepository

    override fun onCreate() {
        super.onCreate()

        ReminderScheduler.createChannels(this)
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        appScope.launch {
            ReminderScheduler.rescheduleAll(this@LoomApplication, userPreferences)
        }
        com.example.loom.widget.LoomWidgets.keepUpdated(this, appScope)
        // Health Connect: sync on each start while the app is in use (if the user turned it on)
        appScope.launch {
            runCatching { com.example.loom.data.health.HealthConnectSync.syncIfEnabled(this@LoomApplication, trackerRepository) }
        }

        // Grab the OS default crash handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        // Attach our custom parachute
        Thread.setDefaultUncaughtExceptionHandler(
            EmergencyCrashHandler(
                context = this,
                backupManager = backupManager,
                defaultHandler = defaultHandler
            )
        )
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20) // Limit RAM usage to 20% to prevent OutOfMemory crashes
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // Limit disk cache to 2% of total device storage
                    .build()
            }
            .crossfade(true) // Smooth UI transitions
            .respectCacheHeaders(false) // Force caching even for strict network images
            .build()
    }
}