package com.example.loom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey val dateId: String,
    var waterIntake: Int = 0,
    var calories: Int = 0,
    var protein: Int = 0,
    var amSkincareDone: Boolean = false,
    var pmSkincareDone: Boolean = false,
    var diaryEntry: String = ""
)

// --- UPGRADED TASK ENGINE ENTITIES ---

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val targetDate: String,
    var isCompleted: Boolean = false, // Syncs with status "DONE"
    var status: String = "TO DO", // "TO DO", "IN PROGRESS", "DONE"
    var priority: Int = 1, // 1 (Low) to 5 (Critical)
    var tagsJson: String = "[]", // List of Hex colors or Tag Names
    var sectionId: Int = 1, // Links to TaskSection
    var subtasksJson: String = "[]", // Serialized list of SubTasks
    var isRecurring: Boolean = false,
    var recurringInterval: String = "NONE" // "DAILY", "WEEKLY", "MONTHLY"
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

// --- HEALTH & DIARY ENTITIES ---

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateId: String,
    val exerciseName: String,
    val sets: Int = 0,
    val reps: Int = 0,
    val weight: Int = 0,
    val isTimeBased: Boolean = false,
    val durationInSeconds: Int = 0,
    val presetName: String? = null
)

@Serializable
data class PresetExercise(
    val name: String,
    val sets: Int,
    val reps: Int,
    val weight: Int,
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