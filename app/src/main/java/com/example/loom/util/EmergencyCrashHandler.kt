package com.example.loom.util

import android.content.Context
import android.util.Log
import com.example.loom.data.local.dao.SandboxDao
import com.example.loom.data.local.dao.TrackerDao
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.ui.home.LoomBackup
import com.example.loom.ui.home.SpaceBackupDto
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration.Companion.seconds

class EmergencyCrashHandler(
    private val context: Context,
    private val trackerDao: TrackerDao,
    private val sandboxDao: SandboxDao,
    private val userPreferences: UserPreferences,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        Log.e("EmergencyCrash", "FATAL CRASH DETECTED. Deploying parachute...", exception)

        try {
            // Block the thread and give the database 2 seconds maximum to dump data
            // If it takes longer than 2 seconds, we abort to prevent a hard freeze
            runBlocking {
                withTimeout(2.seconds) {
                    val activeSpaces = sandboxDao.getActiveSpaces().first().map { it.toDomainModel() }
                    val backupSpaces = activeSpaces.map { space ->
                        SpaceBackupDto(
                            id = space.id, title = space.title, icon = space.icon, bannerUrl = space.bannerUrl,
                            updatedAt = space.updatedAt, isPinned = space.isPinned,
                            tagsJson = Json.encodeToString(space.tags),
                            isPrivate = space.isPrivate, isDeleted = space.isDeleted,
                            blocks = space.blocks
                        )
                    }

                    val backup = LoomBackup(
                        dailyRecords = trackerDao.getAllDailyRecords().first(),
                        tasks = trackerDao.getAllTasks().first(),
                        taskSections = trackerDao.getAllTaskSections().first(),
                        exercises = trackerDao.getAllExercises().first(),
                        workoutPresets = trackerDao.getAllPresets().first(),
                        exerciseTemplates = trackerDao.getAllTemplates().first(),
                        diaryNotes = trackerDao.getAllDiaryNotes().first(),
                        noteTags = trackerDao.getAllNoteTags().first(),
                        skincareProducts = trackerDao.getAllSkincareProducts().first(),
                        skincareRoutines = trackerDao.getAllSkincareRoutines().first(),
                        foodItems = trackerDao.getUserCreatedFoodItems().first(),
                        notionSpaces = backupSpaces,
                        targetCalories = userPreferences.targetCaloriesFlow.first(),
                        targetProtein = userPreferences.targetProteinFlow.first(),
                        targetCarbs = userPreferences.targetCarbsFlow.first(),
                        targetFats = userPreferences.targetFatsFlow.first(),
                        defaultWaterIncrement = userPreferences.defaultWaterIncrementFlow.first(),
                        appTheme = userPreferences.appThemeFlow.first(),
                        isBiometricEnabled = userPreferences.biometricEnabledFlow.first()
                    )

                    val jsonStr = GsonBuilder().setPrettyPrinting().create().toJson(backup)

                    // Write to the app's hidden internal cache directory
                    val backupFile = File(context.cacheDir, "emergency_backup.json")
                    backupFile.writeText(jsonStr)
                    Log.d("EmergencyCrash", "Parachute deployed. Data saved safely to cache.")
                }
            }
        } catch (e: Exception) {
            Log.e("EmergencyCrash", "Parachute deployment failed.", e)
        } finally {
            // Step aside and let Android officially crash the app so the user knows it closed
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}