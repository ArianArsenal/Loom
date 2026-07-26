package com.example.loom.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.loom.data.local.dao.TrackerDao
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.Task
import com.example.loom.data.local.entity.TaskSection
import com.example.loom.data.local.entity.WorkoutPreset
import com.example.loom.data.local.entity.ExerciseTemplate
import com.example.loom.data.local.entity.DiaryNote
import com.example.loom.data.local.entity.NoteTag

@Database(
    entities = [
        DailyRecord::class, Task::class, TaskSection::class, Exercise::class,
        WorkoutPreset::class, ExerciseTemplate::class,
        DiaryNote::class, NoteTag::class
    ],
    version = 10, // Bumped to 10 for the new Kanban Engine
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val trackerDao: TrackerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "life_tracker_db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}