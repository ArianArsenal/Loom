package com.example.loom.data.backup

import android.content.Context
import androidx.annotation.Keep
import androidx.room.withTransaction
import com.example.loom.data.local.AppDatabase
import com.example.loom.data.local.dao.SandboxDao
import com.example.loom.data.local.dao.TrackerDao
import com.example.loom.data.local.entity.NotionSpaceEntity
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.ui.home.LoomBackup
import com.example.loom.ui.home.SpaceBackupDto
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** A single stored setting, typed so it can be written back losslessly. */
@Keep
data class PrefBackup(val key: String, val type: String, val value: String) {
    fun decodeSet(): Set<String> = try { Json.decodeFromString<Set<String>>(value) } catch (_: Exception) { emptySet() }

    companion object {
        fun of(key: String, value: Any?): PrefBackup? = when (value) {
            is Int -> PrefBackup(key, "int", value.toString())
            is Long -> PrefBackup(key, "long", value.toString())
            is Float -> PrefBackup(key, "float", value.toString())
            is Double -> PrefBackup(key, "double", value.toString())
            is Boolean -> PrefBackup(key, "bool", value.toString())
            is String -> PrefBackup(key, "string", value)
            is Set<*> -> PrefBackup(key, "stringSet", Json.encodeToString(value.map { it.toString() }.toSet()))
            else -> null
        }
    }
}

/**
 * Builds and restores full Loom backups. Shared by Settings export/import and the crash handler
 * so every path writes the same complete format.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val trackerDao: TrackerDao,
    private val sandboxDao: SandboxDao,
    private val userPreferences: UserPreferences
) {
    suspend fun createBackup(): LoomBackup {
        // Include trashed spaces too: import wipes the whole spaces table
        val spaces = sandboxDao.getAllSpacesOnce().map { it.toDomainModel() }.map { space ->
            SpaceBackupDto(
                id = space.id, title = space.title, icon = space.icon, bannerUrl = space.bannerUrl,
                updatedAt = space.updatedAt, isPinned = space.isPinned,
                tagsJson = Json.encodeToString(space.tags),
                isPrivate = space.isPrivate, isDeleted = space.isDeleted,
                blocks = space.blocks
            )
        }

        return LoomBackup(
            backupVersion = CURRENT_BACKUP_VERSION,
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
            notionSpaces = spaces,
            targetCalories = userPreferences.targetCaloriesFlow.first(),
            targetProtein = userPreferences.targetProteinFlow.first(),
            targetCarbs = userPreferences.targetCarbsFlow.first(),
            targetFats = userPreferences.targetFatsFlow.first(),
            defaultWaterIncrement = userPreferences.defaultWaterIncrementFlow.first(),
            appTheme = userPreferences.appThemeFlow.first(),
            isBiometricEnabled = userPreferences.biometricEnabledFlow.first(),
            dayRolloverHour = userPreferences.dayRolloverHourFlow.first(),
            preferences = userPreferences.snapshot(),
            templatePrefs = sharedPrefsSnapshot(TEMPLATE_PREFS)
        )
    }

    /** Streams the backup as JSON instead of building one giant string in memory. */
    suspend fun writeBackup(out: OutputStream) {
        val data = createBackup()
        out.bufferedWriter().use { writer -> prettyGson.toJson(data, writer) }
    }

    /**
     * Parses and validates [json] first, then replaces all data in one transaction.
     * Any failure leaves the existing data untouched. A safety copy of the current data is
     * written to filesDir/backups before anything is replaced.
     */
    suspend fun restore(json: String) {
        val backup = try {
            Gson().fromJson(json, LoomBackup::class.java)
        } catch (_: JsonParseException) {
            throw IllegalArgumentException("Not a valid Loom backup file")
        } ?: throw IllegalArgumentException("Backup file is empty")

        // Gson bypasses Kotlin null-safety, so treat every list as possibly null / containing nulls
        val dailyRecords = backup.dailyRecords.orEmpty().filterNotNull()
        val tasks = backup.tasks.orEmpty().filterNotNull()
        val taskSections = backup.taskSections.orEmpty().filterNotNull()
        val exercises = backup.exercises.orEmpty().filterNotNull()
        val presets = backup.workoutPresets.orEmpty().filterNotNull()
        val templates = backup.exerciseTemplates.orEmpty().filterNotNull()
        val diaryNotes = backup.diaryNotes.orEmpty().filterNotNull()
        val noteTags = backup.noteTags.orEmpty().filterNotNull()
        val skincareProducts = backup.skincareProducts.orEmpty().filterNotNull()
        val skincareRoutines = backup.skincareRoutines.orEmpty().filterNotNull()
        val foodItems = backup.foodItems.orEmpty().filterNotNull().map { it.copy(isUserCreated = true) }
        val spaces = backup.notionSpaces.orEmpty().filterNotNull().map { it.toDomainModel() }

        val totalItems = dailyRecords.size + tasks.size + exercises.size + diaryNotes.size + spaces.size +
            presets.size + skincareRoutines.size + foodItems.size
        require(totalItems > 0 || !backup.preferences.isNullOrEmpty()) { "Backup contains no data" }

        writeSafetyCopy("pre-import")

        // All-or-nothing: a bad row (e.g. a missing NOT NULL field) rolls the whole import back
        db.withTransaction {
            trackerDao.wipeDailyRecords()
            trackerDao.wipeTasks()
            trackerDao.wipeTaskSections()
            trackerDao.wipeExercises()
            trackerDao.wipeWorkoutPresets()
            trackerDao.wipeExerciseTemplates()
            trackerDao.wipeDiaryNotes()
            trackerDao.wipeNoteTags()
            trackerDao.wipeSkincareProducts()
            trackerDao.wipeSkincareRoutines()
            trackerDao.wipeUserCreatedFoodItems()
            sandboxDao.wipeAllSpaces()

            trackerDao.insertDailyRecords(dailyRecords)
            trackerDao.insertTasks(tasks)
            trackerDao.insertTaskSections(taskSections)
            trackerDao.insertExercises(exercises)
            trackerDao.insertPresets(presets)
            trackerDao.insertTemplates(templates)
            trackerDao.insertDiaryNotes(diaryNotes)
            trackerDao.insertNoteTags(noteTags)
            trackerDao.insertSkincareProducts(skincareProducts)
            trackerDao.insertSkincareRoutines(skincareRoutines)
            trackerDao.insertFoodItems(foodItems)

            spaces.forEach { space ->
                val blockEntities = com.example.loom.ui.sandbox.flattenTree(space.blocks, space.id)
                sandboxDao.saveSpaceWithBlocks(NotionSpaceEntity.fromDomainModel(space), blockEntities)
            }
        }

        // Settings are restored only after the database commit succeeded
        val prefs = backup.preferences?.filterNotNull()
        if (!prefs.isNullOrEmpty()) {
            userPreferences.restoreSnapshot(prefs)
        } else {
            // Legacy (v1) backups only carried a handful of settings
            userPreferences.saveTargets(backup.targetCalories, backup.targetProtein, backup.targetCarbs, backup.targetFats)
            userPreferences.saveDefaultWaterIncrement(backup.defaultWaterIncrement)
            @Suppress("USELESS_ELVIS") // Gson may leave it null
            userPreferences.saveAppTheme(backup.appTheme ?: "System")
            userPreferences.setBiometricEnabled(backup.isBiometricEnabled)
            userPreferences.saveDayRolloverHour(backup.dayRolloverHour)
        }
        backup.templatePrefs?.let { restoreSharedPrefs(TEMPLATE_PREFS, it.filterNotNull()) }
    }

    /** Writes the current data to filesDir/backups, keeping only the newest few copies per label. */
    suspend fun writeSafetyCopy(label: String): File? = try {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(dir, "$label-${System.currentTimeMillis()}.json")
        file.outputStream().use { writeBackup(it) }
        dir.listFiles { f -> f.name.startsWith("$label-") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_SAFETY_COPIES)
            ?.forEach { it.delete() }
        file
    } catch (e: Exception) {
        android.util.Log.e("BackupManager", "Safety copy failed", e)
        null
    }

    private fun sharedPrefsSnapshot(name: String): List<PrefBackup> =
        context.getSharedPreferences(name, Context.MODE_PRIVATE).all.mapNotNull { (k, v) -> PrefBackup.of(k, v) }

    private fun restoreSharedPrefs(name: String, entries: List<PrefBackup>) {
        val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
        entries.forEach { e ->
            when (e.type) {
                "string" -> editor.putString(e.key, e.value)
                "stringSet" -> editor.putStringSet(e.key, e.decodeSet())
                "int" -> e.value.toIntOrNull()?.let { editor.putInt(e.key, it) }
                "long" -> e.value.toLongOrNull()?.let { editor.putLong(e.key, it) }
                "float" -> e.value.toFloatOrNull()?.let { editor.putFloat(e.key, it) }
                "bool" -> editor.putBoolean(e.key, e.value.toBoolean())
            }
        }
        editor.apply()
    }

    companion object {
        const val CURRENT_BACKUP_VERSION = 2
        const val TEMPLATE_PREFS = "loom_templates"
        private const val MAX_SAFETY_COPIES = 3
        private val prettyGson: Gson = GsonBuilder().setPrettyPrinting().create()
    }
}
