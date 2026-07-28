package com.example.loom.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import com.example.loom.data.local.entity.WorkoutPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    // --- Daily Records ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyRecord(record: DailyRecord): Long

    @Update
    suspend fun updateDailyRecord(record: DailyRecord)

    @Query("SELECT * FROM daily_records WHERE dateId = :dateId")
    fun getDailyRecordFlow(dateId: String): Flow<DailyRecord?>

    @Query("SELECT * FROM daily_records WHERE dateId = :dateId")
    suspend fun getDailyRecordSync(dateId: String): DailyRecord?

    @Query("SELECT * FROM daily_records WHERE dateId LIKE :monthPrefix || '%'")
    fun getRecordsForMonth(monthPrefix: String): Flow<List<DailyRecord>>

    // --- Tasks Engine ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE targetDate = :date ORDER BY isCompleted ASC, priority DESC")
    fun getTasksForDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY priority DESC, targetDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    // --- Task Sections ---
    @Query("SELECT * FROM task_sections ORDER BY orderIndex ASC")
    fun getAllTaskSections(): Flow<List<TaskSection>>

    @Query("SELECT COUNT(*) FROM task_sections")
    suspend fun getTaskSectionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskSection(section: TaskSection)

    @Delete
    suspend fun deleteTaskSection(section: TaskSection)

    // --- Exercises ---
    @Query("SELECT * FROM exercises WHERE dateId = :date ORDER BY id ASC")
    fun getExercisesForDate(date: String): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    // --- Presets ---
    @Query("SELECT * FROM workout_presets ORDER BY presetName ASC")
    fun getAllPresets(): Flow<List<WorkoutPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: WorkoutPreset)

    @Delete
    suspend fun deletePreset(preset: WorkoutPreset)

    // --- Exercise Templates ---
    @Query("SELECT * FROM exercise_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<ExerciseTemplate>>

    @Query("SELECT COUNT(*) FROM exercise_templates")
    suspend fun getTemplateCount(): Int

    @Query("SELECT * FROM exercise_templates WHERE name = :name LIMIT 1")
    suspend fun getTemplateByName(name: String): ExerciseTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ExerciseTemplate)

    @Delete
    suspend fun deleteTemplate(template: ExerciseTemplate)

    // --- Diary Notes & Tags ---
    @Query("SELECT * FROM diary_notes ORDER BY timestamp DESC")
    fun getAllDiaryNotes(): Flow<List<DiaryNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryNote(note: DiaryNote): Long

    @Delete
    suspend fun deleteDiaryNote(note: DiaryNote)

    @Query("SELECT * FROM note_tags ORDER BY name ASC")
    fun getAllNoteTags(): Flow<List<NoteTag>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTag(tag: NoteTag)

    @Query("SELECT COUNT(*) FROM note_tags")
    suspend fun getNoteTagCount(): Int

    @Delete
    suspend fun deleteNoteTag(tag: NoteTag)

    @Query("DELETE FROM tasks WHERE status = 'DONE' AND completionDateMillis > 0 AND completionDateMillis < :thresholdMillis")
    suspend fun deleteOldDoneTasks(thresholdMillis: Long)

    // --- NEW: Skincare Engine ---
    @Query("SELECT * FROM skincare_routines")
    fun getAllSkincareRoutines(): Flow<List<SkincareRoutine>>

    @Query("SELECT * FROM skincare_routines WHERE isPinnedToHome = 1")
    fun getPinnedSkincareRoutines(): Flow<List<SkincareRoutine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkincareRoutine(routine: SkincareRoutine)

    @Delete
    suspend fun deleteSkincareRoutine(routine: SkincareRoutine)

    @Query("SELECT * FROM skincare_products ORDER BY name ASC")
    fun getAllSkincareProducts(): Flow<List<SkincareProduct>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSkincareProduct(product: SkincareProduct)

    @Query("SELECT COUNT(*) FROM skincare_products")
    suspend fun getSkincareProductCount(): Int

    @Delete
    suspend fun deleteSkincareProduct(product: SkincareProduct)

    // --- NEW: Food Pantry ---
    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun getAllFoodItems(): Flow<List<FoodItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(food: FoodItem)

    @Delete
    suspend fun deleteFoodItem(food: FoodItem)

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun getFoodItemCount(): Int

    @Query("SELECT * FROM exercises WHERE dateId LIKE :monthPrefix || '%'")
    fun getExercisesForMonth(monthPrefix: String): Flow<List<Exercise>>
}