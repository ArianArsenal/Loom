package com.example.loom.data.repository

import com.example.loom.data.local.dao.TrackerDao
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.data.local.entity.DiaryNote
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.ExerciseTemplate
import com.example.loom.data.local.entity.FoodItem
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.data.local.entity.SkincareProduct
import com.example.loom.data.local.entity.SkincareRoutine
import com.example.loom.data.local.entity.Task
import com.example.loom.data.local.entity.TaskSection
import com.example.loom.data.local.entity.WaterLog
import com.example.loom.data.local.entity.WorkoutPreset
import com.example.loom.util.DateUtils
import com.example.loom.data.local.entity.ConsumedFoodLog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

class TrackerRepository @Inject constructor(private val dao: TrackerDao) {

    // --- Daily Records ---
    // Every read-modify-write of a DailyRecord goes through editRecord under this lock, so two quick
    // taps (or Home + Health at once) can't overwrite each other with stale copies.
    private val recordLock = Mutex()

    suspend fun initializeTodayRecord() = ensureRecord(DateUtils.getTodayDateId())

    private suspend fun ensureRecord(dateId: String) {
        dao.insertDailyRecord(DailyRecord(dateId = dateId)) // IGNORE on conflict
    }

    /** Today's record; follows the logical day as it changes while the app stays open. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTodayRecord(): Flow<DailyRecord?> = DateUtils.todayDateIdFlow.flatMapLatest { dateId ->
        flow {
            ensureRecord(dateId)
            emitAll(dao.getDailyRecordFlow(dateId))
        }
    }

    /** Exercises for the logical today, switching over at the rollover hour. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTodayExercises(): Flow<List<Exercise>> = DateUtils.todayDateIdFlow.flatMapLatest { dao.getExercisesForDate(it) }

    fun getDailyRecordFlow(dateId: String): Flow<DailyRecord?> = dao.getDailyRecordFlow(dateId)
    fun getRecordsForMonth(monthPrefix: String): Flow<List<DailyRecord>> = dao.getRecordsForMonth(monthPrefix)

    /** Atomically applies [transform] to the freshest copy of a day's record, creating the day if needed. */
    suspend fun editRecord(dateId: String = DateUtils.getTodayDateId(), transform: (DailyRecord) -> DailyRecord) {
        recordLock.withLock {
            val current = dao.getDailyRecordSync(dateId) ?: DailyRecord(dateId = dateId).also { dao.insertDailyRecord(it) }
            dao.updateDailyRecord(transform(current.copy()))
        }
    }

    /** Detailed hydration log (Health tab): intake is recomputed from the logs and their hydration factors. */
    suspend fun editWaterLogs(dateId: String = DateUtils.getTodayDateId(), transform: (List<WaterLog>) -> List<WaterLog>) {
        editRecord(dateId) { record ->
            val logs = transform(decodeList<WaterLog>(record.waterLogsJson))
            val netHydration = logs.sumOf { (it.amountMl * it.hydrationFactor).toDouble() }
            record.copy(waterLogsJson = Json.encodeToString(logs), waterIntakeMl = netHydration.toInt())
        }
    }

    // Keep this for backward compatibility and Home tab Quick Add
    suspend fun addWater(amountMl: Int, targetDateId: String = DateUtils.getTodayDateId()) {
        // Find matching container name/icon based on volume, or fallback nicely
        val containerName = when (amountMl) {
            250 -> "Glass"
            600 -> "Shaker"
            1500 -> "Bottle"
            else -> "${amountMl}ml Drink"
        }
        val iconKey = when (amountMl) {
            250 -> "LocalDrink"
            600 -> "LocalCafe"
            1500 -> "WaterDrop"
            else -> "LocalDrink"
        }

        val newLog = WaterLog(
            id = UUID.randomUUID().toString(),
            containerName = containerName,
            emoji = iconKey,
            amountMl = amountMl,
            hydrationFactor = 1.0f,
            timestamp = System.currentTimeMillis()
        )
        editRecord(targetDateId) { record ->
            val currentLogs = decodeList<WaterLog>(record.waterLogsJson)
            record.copy(waterLogsJson = Json.encodeToString(currentLogs + newLog), waterIntakeMl = record.waterIntakeMl + amountMl)
        }
    }

    suspend fun removeWater(amountMl: Int, targetDateId: String = DateUtils.getTodayDateId()) {
        editRecord(targetDateId) { record ->
            val currentLogs = decodeList<WaterLog>(record.waterLogsJson)
            if (currentLogs.isNotEmpty()) {
                val removedLog = currentLogs.last()
                record.copy(
                    waterLogsJson = Json.encodeToString(currentLogs.dropLast(1)),
                    waterIntakeMl = (record.waterIntakeMl - (removedLog.amountMl * removedLog.hydrationFactor).toInt()).coerceAtLeast(0)
                )
            } else {
                record.copy(waterIntakeMl = (record.waterIntakeMl - amountMl).coerceAtLeast(0))
            }
        }
    }

    suspend fun addConsumedFood(log: ConsumedFoodLog, dateId: String = DateUtils.getTodayDateId()) {
        editRecord(dateId) { record ->
            val logs = decodeList<ConsumedFoodLog>(record.consumedFoodsJson)
            record.copy(
                calories = record.calories + log.calories,
                protein = record.protein + log.protein,
                carbs = record.carbs + log.carbs,
                fats = record.fats + log.fats,
                consumedFoodsJson = Json.encodeToString(logs + log)
            )
        }
    }

    suspend fun removeConsumedFood(logId: String, dateId: String = DateUtils.getTodayDateId()) {
        editRecord(dateId) { record ->
            val logs = decodeList<ConsumedFoodLog>(record.consumedFoodsJson)
            val removed = logs.find { it.id == logId }
            if (removed == null) record else record.copy(
                calories = (record.calories - removed.calories).coerceAtLeast(0),
                protein = (record.protein - removed.protein).coerceAtLeast(0),
                carbs = (record.carbs - removed.carbs).coerceAtLeast(0),
                fats = (record.fats - removed.fats).coerceAtLeast(0),
                consumedFoodsJson = Json.encodeToString(logs.filter { it.id != logId })
            )
        }
    }

    suspend fun setNutrition(cals: Int, pro: Int, carbs: Int, fats: Int, targetDateId: String = DateUtils.getTodayDateId()) {
        editRecord(targetDateId) { record ->
            record.copy(
                calories = cals.coerceAtLeast(0),
                protein = pro.coerceAtLeast(0),
                carbs = carbs.coerceAtLeast(0),
                fats = fats.coerceAtLeast(0),
                consumedFoodsJson = if (cals == 0 && pro == 0 && carbs == 0 && fats == 0) "[]" else record.consumedFoodsJson
            )
        }
    }

    // --- FOOD PANTRY ---
    fun searchFoodItems(query: String): Flow<List<FoodItem>> = dao.searchFoodItemsGrouped(query) // --- FIX: Point to the new optimized DAO method! ---
    suspend fun insertFoodItem(food: FoodItem) = dao.insertFoodItem(food)
    suspend fun deleteFoodItem(food: FoodItem) = dao.deleteFoodItem(food)


    // --- Tasks Engine ---
    fun getAllTasks(): Flow<List<Task>> = dao.getAllTasks()

    /** Tasks due on, or completed during, the given calendar dates (inclusive). */
    fun getTasksBetween(from: java.time.LocalDate, to: java.time.LocalDate): Flow<List<Task>> {
        val zone = java.time.ZoneId.systemDefault()
        return dao.getTasksInRange(
            from.toString(), to.toString(),
            from.atStartOfDay(zone).toInstant().toEpochMilli(),
            to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        )
    }
    suspend fun insertTask(task: Task) = dao.insertTask(task)
    suspend fun updateTask(task: Task) = dao.updateTask(task)
    suspend fun deleteTask(task: Task) = dao.deleteTask(task)

    fun getAllTaskSections(): Flow<List<TaskSection>> = dao.getAllTaskSections()
    suspend fun insertTaskSection(section: TaskSection) = dao.insertTaskSection(section)
    suspend fun deleteTaskSection(section: TaskSection) = dao.deleteTaskSection(section)

    suspend fun prepopulateTaskSectionsIfEmpty() {
        if (dao.getTaskSectionCount() == 0) {
            val defaults = listOf(
                TaskSection(name = "General", orderIndex = 0),
                TaskSection(name = "University", orderIndex = 1),
                TaskSection(name = "Dev Projects", orderIndex = 2),
                TaskSection(name = "Fitness & Health", orderIndex = 3)
            )
            defaults.forEach { dao.insertTaskSection(it) }
        }
    }


    // --- Exercises ---
    fun getExercisesForDate(date: String): Flow<List<Exercise>> = dao.getExercisesForDate(date)

    suspend fun insertExercise(
        name: String, sets: Int, reps: Int, weight: Float,
        weightListJson: String = "[]", isTimeBased: Boolean, duration: Int,
        presetName: String? = null, targetDateId: String = DateUtils.getTodayDateId(),
        isCompleted: Boolean = false
    ) {
        dao.insertExercise(
            Exercise(
                dateId = targetDateId,
                exerciseName = name,
                sets = sets,
                reps = reps,
                weight = weight,
                weightListJson = weightListJson,
                isTimeBased = isTimeBased,
                durationInSeconds = duration,
                presetName = presetName,
                isCompleted = isCompleted
            )
        )
    }

    suspend fun deleteExercise(exercise: Exercise) = dao.deleteExercise(exercise)
    suspend fun updateExercise(exercise: Exercise) { dao.updateExercise(exercise) }

    // --- Presets ---
    fun getAllPresets(): Flow<List<WorkoutPreset>> = dao.getAllPresets()
    suspend fun insertPreset(preset: WorkoutPreset) = dao.insertPreset(preset)
    suspend fun deletePreset(preset: WorkoutPreset) = dao.deletePreset(preset)

    // --- Exercise Templates ---
    fun getAllTemplates(): Flow<List<ExerciseTemplate>> = dao.getAllTemplates()

    suspend fun prepopulateTemplatesIfEmpty() {
        if (dao.getTemplateCount() == 0) {
            val defaults = listOf(
                "Bench Press", "Incline Bench Press", "Decline Bench Press", "Push-ups", "Dumbbell Flyes",
                "Squat", "Front Squat", "Leg Press", "Lunges", "Calf Raises", "Leg Extensions", "Leg Curls",
                "Deadlift", "Romanian Deadlift", "Pull-ups", "Lat Pulldown", "Barbell Row", "Seated Cable Row",
                "Overhead Press", "Lateral Raises", "Front Raises", "Face Pulls",
                "Dumbbell Curls", "Hammer Curls", "Preacher Curls",
                "Tricep Pushdown", "Skull Crushers", "Overhead Tricep Extension",
                "Crunches", "Plank", "Russian Twists", "Leg Raises"
            )
            defaults.forEach { dao.insertTemplate(ExerciseTemplate(name = it)) }
        }
    }

    suspend fun insertTemplateIfNotExists(name: String) {
        val existing = dao.getTemplateByName(name)
        if (existing == null) dao.insertTemplate(ExerciseTemplate(name = name))
    }

    suspend fun updateTemplate(template: ExerciseTemplate) = dao.insertTemplate(template)
    suspend fun deleteTemplate(template: ExerciseTemplate) = dao.deleteTemplate(template)

    // --- Diary Notes & Tags ---
    fun getAllDiaryNotes(): Flow<List<DiaryNote>> = dao.getAllDiaryNotes()
    suspend fun insertDiaryNoteAndGetId(note: DiaryNote): Long = dao.insertDiaryNote(note)
    suspend fun deleteDiaryNote(note: DiaryNote) = dao.deleteDiaryNote(note)
    fun getAllNoteTags(): Flow<List<NoteTag>> = dao.getAllNoteTags()
    suspend fun insertNoteTag(name: String, colorHex: String) = dao.insertNoteTag(NoteTag(name, colorHex))

    suspend fun prepopulateTagsIfEmpty() {
        if (dao.getNoteTagCount() == 0) {
            val defaultTags = listOf(
                NoteTag("Personal", "#4CAF50"), NoteTag("University", "#2196F3"),
                NoteTag("Code", "#9C27B0"), NoteTag("Gym", "#F44336")
            )
            defaultTags.forEach { dao.insertNoteTag(it) }
        }
    }
    suspend fun deleteNoteTag(tag: NoteTag) = dao.deleteNoteTag(tag)

    // --- Skincare Engine ---
    fun getAllSkincareRoutines(): Flow<List<SkincareRoutine>> = dao.getAllSkincareRoutines()
    fun getPinnedSkincareRoutines(): Flow<List<SkincareRoutine>> = dao.getPinnedSkincareRoutines()
    suspend fun insertSkincareRoutine(routine: SkincareRoutine) = dao.insertSkincareRoutine(routine)
    suspend fun deleteSkincareRoutine(routine: SkincareRoutine) = dao.deleteSkincareRoutine(routine)

    fun getAllSkincareProducts(): Flow<List<SkincareProduct>> = dao.getAllSkincareProducts()
    suspend fun insertSkincareProduct(product: SkincareProduct) = dao.insertSkincareProduct(product)

    suspend fun prepopulateSkincareProductsIfEmpty() {
        if (dao.getSkincareProductCount() == 0) {
            val defaults = listOf(
                "Cleanser", "Vitamin C", "Hyaluronic Acid",
                "Niacinamide", "Salicylic Acid", "Moisturizer",
                "Sunscreen", "Retinol", "Toner", "Eye Cream"
            )
            defaults.forEach { dao.insertSkincareProduct(SkincareProduct(name = it)) }
        }
    }


    suspend fun updateDailyRecord(record: DailyRecord) = dao.updateDailyRecord(record)

    fun getExercisesForMonth(monthPrefix: String): Flow<List<Exercise>> = dao.getExercisesForMonth(monthPrefix)
}

@PublishedApi
internal val lenientListJson = Json { ignoreUnknownKeys = true }

/** Decodes a JSON list column, treating corrupt or empty values as an empty list. */
inline fun <reified T> decodeList(json: String?): List<T> =
    try { lenientListJson.decodeFromString<List<T>>(json ?: "[]") } catch (_: Exception) { emptyList() }

/** Helpers for the id lists stored on a DailyRecord (supplements, skincare routines/steps). */
fun toggleJsonId(json: String, id: String): String {
    val list = decodeList<String>(json)
    return Json.encodeToString(if (id in list) list - id else list + id)
}

fun addJsonIds(json: String, ids: Collection<String>): String {
    val list = decodeList<String>(json)
    return Json.encodeToString(list + ids.filter { it !in list })
}

fun removeJsonIds(json: String, ids: Collection<String>): String =
    Json.encodeToString(decodeList<String>(json).filter { it !in ids })
