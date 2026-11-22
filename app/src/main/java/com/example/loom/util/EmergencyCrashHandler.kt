package com.example.loom.util

import android.content.Context
import android.util.Log
import com.example.loom.data.backup.BackupManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

class EmergencyCrashHandler(
    private val context: Context,
    private val backupManager: BackupManager,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        Log.e("EmergencyCrash", "FATAL CRASH DETECTED. Deploying parachute...", exception)

        // Loading the whole database while out of memory would only make things worse
        if (exception !is OutOfMemoryError) {
            try {
                // Give the database 2 seconds maximum to dump data so we never hard-freeze
                runBlocking {
                    withTimeout(2.seconds) {
                        val backupFile = File(context.cacheDir, "emergency_backup.json")
                        backupFile.outputStream().use { backupManager.writeBackup(it) }
                        Log.d("EmergencyCrash", "Parachute deployed. Data saved safely to cache.")
                    }
                }
            } catch (t: Throwable) {
                Log.e("EmergencyCrash", "Parachute deployment failed.", t)
            }
        }

        // Step aside and let Android officially crash the app so the user knows it closed
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, exception)
        } else {
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }
}
