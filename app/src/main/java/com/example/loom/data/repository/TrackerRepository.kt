package com.example.loom.data.repository

import com.example.loom.data.local.dao.TrackerDao
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.data.local.entity.DiaryNote
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.ExerciseTemplate
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.data.local.entity.SkincareProduct
import com.example.loom.data.local.entity.SkincareRoutine
import com.example.loom.data.local.entity.Task
import com.example.loom.data.local.entity.TaskSection
import com.example.loom.data.local.entity.WorkoutPreset
import com.example.loom.util.DateUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TrackerRepository @Inject constructor(private val dao: TrackerDao) {

    // --- Daily Records ---
    suspend fun initializeTodayRecord() {
        val todayId = DateUtils.getTodayDateId()
        val existingRecord = dao.getDailyRecordSync(todayId)

        if (existingRecord == null) {
            val newRecord = DailyRecord(dateId = todayId)
            dao.insertDailyRecord(newRecord)
        }
    }

    fun getTodayRecord(): Flow<DailyRecord?> = dao.getDailyRecordFlow(DateUtils.getTodayDateId())

    fun getDailyRecordFlow(dateId: String): Flow<DailyRecord?> = dao.getDailyRecordFlow(dateId)
    fun getRecordsForMonth(monthPrefix: String): Flow<List<DailyRecord>> = dao.getRecordsForMonth(monthPrefix)

    suspend fun addWater(amountMl: Int, targetDateId: String = DateUtils.getTodayDateId()) {
        val record = dao.getDailyRecordSync(targetDateId)
        if (record != null) {
            record.waterIntakeMl += amountMl
            dao.updateDailyRecord(record)
        } else {
            // If no record exists for that past day, create one
            val newRecord = DailyRecord(dateId = targetDateId, waterIntakeMl = amountMl)
            dao.insertDailyRecord(newRecord)
        }
    }

    suspend fun removeWater(amountMl: Int, targetDateId: String = DateUtils.getTodayDateId()) {
        val record = dao.getDailyRecordSync(targetDateId)
        if (record != null && record.waterIntakeMl > 0) {
            record.waterIntakeMl = (record.waterIntakeMl - amountMl).coerceAtLeast(0)
            dao.updateDailyRecord(record)
        }
    }
    suspend fun updateDiaryNote(note: String) {
        val record = dao.getDailyRecordSync(DateUtils.getTodayDateId())
        if (record != null) {
            record.diaryEntry = note
            dao.updateDailyRecord(record)
        }
    }

    suspend fun logConsumedFood(cals: Int, pro: Int, carbs: Int, fats: Int, logsJson: String) {
        val record = dao.getDailyRecordSync(DateUtils.getTodayDateId())
        if (record != null) {
            // FIX: .coerceAtLeast(0) completely prevents negative macros
            record.calories = (record.calories + cals).coerceAtLeast(0)
            record.protein = (record.protein + pro).coerceAtLeast(0)
            record.carbs = (record.carbs + carbs).coerceAtLeast(0)
            record.fats = (record.fats + fats).coerceAtLeast(0)
            record.consumedFoodsJson = logsJson
            dao.updateDailyRecord(record)
        }
    }

    suspend fun setNutrition(cals: Int, pro: Int, carbs: Int, fats: Int, targetDateId: String = DateUtils.getTodayDateId()) {
        val record = dao.getDailyRecordSync(targetDateId)
        if (record != null) {
            record.calories = cals.coerceAtLeast(0)
            record.protein = pro.coerceAtLeast(0)
            record.carbs = carbs.coerceAtLeast(0)
            record.fats = fats.coerceAtLeast(0)

            // FIX: Auto-clear food list if macros are explicitly reset to 0 in the Calendar
            if (cals == 0 && pro == 0 && carbs == 0 && fats == 0) {
                record.consumedFoodsJson = "[]"
            }

            dao.updateDailyRecord(record)
        } else {
            val newRecord = DailyRecord(dateId = targetDateId, calories = cals, protein = pro, carbs = carbs, fats = fats)
            dao.insertDailyRecord(newRecord)
        }
    }

    // --- FOOD PANTRY ---
    fun getAllFoodItems(): Flow<List<com.example.loom.data.local.entity.FoodItem>> = dao.getAllFoodItems()
    suspend fun insertFoodItem(food: com.example.loom.data.local.entity.FoodItem) = dao.insertFoodItem(food)
    suspend fun deleteFoodItem(food: com.example.loom.data.local.entity.FoodItem) = dao.deleteFoodItem(food)

    suspend fun prepopulateFoodPantryIfEmpty() {
        if (dao.getFoodItemCount() == 0) {
            val defaults = listOf(
                com.example.loom.data.local.entity.FoodItem(emoji = "🍛", name = "Persian Rice (Kateh)", servingUnit = "Plate", calories = 250, protein = 5, carbs = 55, fats = 1),
                com.example.loom.data.local.entity.FoodItem(emoji = "🍗", name = "Chicken Breast", servingUnit = "100g", calories = 165, protein = 31, carbs = 0, fats = 3),
                com.example.loom.data.local.entity.FoodItem(emoji = "🥚", name = "Boiled Egg", servingUnit = "1 Egg", calories = 70, protein = 6, carbs = 0, fats = 5),
                com.example.loom.data.local.entity.FoodItem(emoji = "🥩", name = "Beef Stew (Ghormeh Sabzi)", servingUnit = "Bowl", calories = 400, protein = 25, carbs = 15, fats = 28),
                com.example.loom.data.local.entity.FoodItem(emoji = "🥛", name = "Whey Protein Shake", servingUnit = "Scoop", calories = 120, protein = 24, carbs = 3, fats = 1)
            )
            defaults.forEach { dao.insertFoodItem(it) }
        }
    }



    suspend fun updateCompletedSkincareRoutines(routinesJson: String) {
        val record = dao.getDailyRecordSync(DateUtils.getTodayDateId())
        if (record != null) {
            record.completedSkincareRoutinesJson = routinesJson
            dao.updateDailyRecord(record)
        }
    }

    // --- Tasks Engine ---
    fun getAllTasks(): Flow<List<Task>> = dao.getAllTasks()
    fun getTasksForDate(date: String): Flow<List<Task>> = dao.getTasksForDate(date)
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

    suspend fun cleanupOldDoneTasks() {
        val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        dao.deleteOldDoneTasks(twentyFourHoursAgo)
    }

    // --- Exercises ---
    fun getExercisesForDate(date: String): Flow<List<Exercise>> = dao.getExercisesForDate(date)

    suspend fun insertExercise(
        name: String, sets: Int, reps: Int, weight: Float,
        weightListJson: String = "[]", isTimeBased: Boolean, duration: Int,
        presetName: String? = null, targetDateId: String = DateUtils.getTodayDateId(),
        isCompleted: Boolean = false // FIX: Reverted default back to false
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

    suspend fun updateExercise(exercise: Exercise) {
        dao.updateExercise(exercise)
    }

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

    // --- NEW: Skincare Engine ---
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

    suspend fun updateCompletedSkincareSteps(stepsJson: String) {
        val record = dao.getDailyRecordSync(DateUtils.getTodayDateId())
        if (record != null) {
            record.completedSkincareStepsJson = stepsJson
            dao.updateDailyRecord(record)
        }
    }

    suspend fun deleteSkincareProduct(product: SkincareProduct) = dao.deleteSkincareProduct(product)

    fun getExercisesForMonth(monthPrefix: String): Flow<List<Exercise>> = dao.getExercisesForMonth(monthPrefix)
}