package com.example.loom.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.loom.data.local.dao.SandboxDao
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
import com.example.loom.data.local.entity.NotionSpaceEntity

@Database(
    entities = [
        DailyRecord::class, Task::class, TaskSection::class, Exercise::class,
        WorkoutPreset::class, ExerciseTemplate::class,
        DiaryNote::class, NoteTag::class,
        SkincareProduct::class, SkincareRoutine::class,
        FoodItem::class,
        NotionSpaceEntity::class
    ],
    version = 23, // <-- BUMPED TO 23
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val trackerDao: TrackerDao
    abstract val sandboxDao: SandboxDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Existing Migrations
        private val MIGRATION_TO_17 = object : Migration(15, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                executeMigration(db)
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                executeMigration(db)
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `space_schemas` (
                        `id` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `icon` TEXT NOT NULL, 
                        `defaultView` TEXT NOT NULL, 
                        `properties` TEXT NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `space_items` (
                        `id` TEXT NOT NULL, 
                        `spaceId` TEXT NOT NULL, 
                        `values` TEXT NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `space_schemas`")
                db.execSQL("DROP TABLE IF EXISTS `space_items`")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notion_spaces` (
                        `id` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `icon` TEXT NOT NULL, 
                        `blocksJson` TEXT NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notion_spaces` ADD COLUMN `bannerUrl` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `notion_spaces` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notion_spaces` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `notion_spaces` ADD COLUMN `tagsJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        // NEW MIGRATION: Adds Privacy Lock
        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // In SQLite, Booleans are stored as INTEGER (0 = false, 1 = true)
                db.execSQL("ALTER TABLE `notion_spaces` ADD COLUMN `isPrivate` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private fun executeMigration(db: SupportSQLiteDatabase) {
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

            db.execSQL("""
                INSERT INTO exercises_new (id, dateId, exerciseName, sets, reps, weight, weightListJson, isTimeBased, durationInSeconds, presetName)
                SELECT id, dateId, exerciseName, sets, reps, CAST(weight AS REAL), '[]', isTimeBased, durationInSeconds, presetName FROM exercises
            """.trimIndent())

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
                    .addMigrations(
                        MIGRATION_TO_17,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_22_23 // <-- Added new migration here
                    )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}