package com.example.loom.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.loom.data.local.dao.TrackerDao
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.Task
import com.example.loom.data.local.entity.TaskSection
import com.example.loom.data.local.entity.WorkoutPreset
import com.example.loom.data.local.entity.ExerciseTemplate
import com.example.loom.data.local.entity.DiaryNote
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.data.local.entity.SkincareProduct
import com.example.loom.data.local.entity.SkincareRoutine
import com.example.loom.data.local.entity.FoodItem

@Database(
    entities = [
        DailyRecord::class, Task::class, TaskSection::class, Exercise::class,
        WorkoutPreset::class, ExerciseTemplate::class,
        DiaryNote::class, NoteTag::class,
        SkincareProduct::class, SkincareRoutine::class,
        FoodItem::class
    ],
    version = 18, // <-- BUMPED TO 18
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val trackerDao: TrackerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Safe migration from 16 to 17 (and 15 to 17 just in case)
        private val MIGRATION_TO_17 = object : Migration(15, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                executeMigration(db)
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safely add the new column with a default value of 0 (false)
                db.execSQL("ALTER TABLE exercises ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                executeMigration(db)
            }
        }

        private fun executeMigration(db: SupportSQLiteDatabase) {
            // 1. Create exactly what Room expects
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `exercises_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `dateId` TEXT NOT NULL, 
                    `exerciseName` TEXT NOT NULL, 
                    `sets` INTEGER NOT NULL, 
                    `reps` INTEGER NOT NULL, 
                    `weight` REAL NOT NULL, 
                    `weightListJson` TEXT NOT NULL, 
                    `isTimeBased` INTEGER NOT NULL, 
                    `durationInSeconds` INTEGER NOT NULL, 
                    `presetName` TEXT
                )
            """.trimIndent())

            // 2. Copy the data (casting weight to REAL/Float)
            db.execSQL("""
                INSERT INTO exercises_new (id, dateId, exerciseName, sets, reps, weight, weightListJson, isTimeBased, durationInSeconds, presetName)
                SELECT id, dateId, exerciseName, sets, reps, CAST(weight AS REAL), '[]', isTimeBased, durationInSeconds, presetName FROM exercises
            """.trimIndent())

            // 3. Swap the tables
            db.execSQL("DROP TABLE exercises")
            db.execSQL("ALTER TABLE exercises_new RENAME TO exercises")
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "life_tracker_db"
                )
                    .addMigrations(MIGRATION_TO_17, MIGRATION_16_17, MIGRATION_17_18) // <-- ADD IT HERE
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}