package com.example.loom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey val dateId: String,
    var waterIntakeMl: Int = 0,
    var calories: Int = 0,
    var protein: Int = 0,
    var carbs: Int = 0,
    var fats: Int = 0,
    var completedSkincareRoutinesJson: String = "[]",
    var completedSkincareStepsJson: String = "[]",
    var consumedFoodsJson: String = "[]",
    var diaryEntry: String = ""
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val targetDate: String,
    var isCompleted: Boolean = false,
    var status: String = "TO DO",
    var priority: Int = 1,
    var tagsJson: String = "[]",
    var sectionId: Int = 1,
    var subtasksJson: String = "[]",
    var isRecurring: Boolean = false,
    var recurringInterval: String = "NONE",
    var completionDateMillis: Long = 0L
)

@Serializable
data class ConsumedFoodLog(
    val id: String,
    val foodName: String,
    val emoji: String,
    val servings: Float,
    val servingUnit: String = "",
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val timestamp: Long
)

@Entity(tableName = "task_sections")
data class TaskSection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val orderIndex: Int = 0
)

@Serializable
data class SubTask(
    val id: String,
    val title: String,
    var isCompleted: Boolean = false
)

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateId: String,
    val exerciseName: String,
    val sets: Int = 0,
    val reps: Int = 0,
    val weight: Float = 0f,
    val weightListJson: String = "[]",
    val isTimeBased: Boolean = false,
    val durationInSeconds: Int = 0,
    val presetName: String? = null,
    val isCompleted: Boolean = false
)

@Serializable
data class PresetExercise(
    val name: String,
    val sets: Int,
    val reps: Int,
    val weight: Float,
    val weightListJson: String = "[]",
    val isTimeBased: Boolean = false,
    val durationInSeconds: Int = 0
)

@Entity(tableName = "workout_presets")
data class WorkoutPreset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val presetName: String,
    val exercises: List<PresetExercise>
)

@Entity(tableName = "exercise_templates")
data class ExerciseTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(tableName = "diary_notes")
data class DiaryNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String = "Personal",
    val imageUri: String? = null,
    val imagesJson: String = "[]",
    val blocksJson: String = "[]",
    val isFavorite: Boolean = false,
    val isPrivate: Boolean = false,
    val isPinned: Boolean = false,
    val isChecklistMode: Boolean = false
)

@Entity(tableName = "note_tags")
data class NoteTag(
    @PrimaryKey val name: String,
    val colorHex: String
)

@Entity(tableName = "skincare_products")
data class SkincareProduct(
    @PrimaryKey val name: String
)

@Entity(tableName = "skincare_routines")
data class SkincareRoutine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val productsJson: String = "[]",
    val isPinnedToHome: Boolean = true
)

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val servingUnit: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fats: Float
)