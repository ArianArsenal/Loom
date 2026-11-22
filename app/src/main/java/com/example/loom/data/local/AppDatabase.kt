package com.example.loom.data.local

import androidx.room.Database
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
import com.example.loom.data.local.entity.SandboxBlockEntity

@Database(
    entities = [
        DailyRecord::class, Task::class, TaskSection::class, Exercise::class,
        WorkoutPreset::class, ExerciseTemplate::class,
        DiaryNote::class, NoteTag::class,
        SkincareProduct::class, SkincareRoutine::class,
        FoodItem::class,
        com.example.loom.data.local.entity.FoodItemFts::class,
        NotionSpaceEntity::class,
        SandboxBlockEntity::class
    ],
    version = 34,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val trackerDao: TrackerDao
    abstract val sandboxDao: SandboxDao

    companion object {
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

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notion_spaces` ADD COLUMN `isPrivate` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `food_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `servingUnit` TEXT NOT NULL, 
                        `calories` REAL NOT NULL, 
                        `protein` REAL NOT NULL, 
                        `carbs` REAL NOT NULL, 
                        `fats` REAL NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `food_items_new` (`id`, `name`, `servingUnit`, `calories`, `protein`, `carbs`, `fats`)
                    SELECT `id`, `name`, `servingUnit`, CAST(`calories` AS REAL), CAST(`protein` AS REAL), CAST(`carbs` AS REAL), CAST(`fats` AS REAL) 
                    FROM `food_items`
                """.trimIndent())

                db.execSQL("DROP TABLE `food_items`")
                db.execSQL("ALTER TABLE `food_items_new` RENAME TO `food_items`")
            }
        }

        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `food_items` ADD COLUMN `isUserCreated` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `skincare_products` ADD COLUMN `brand` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `skincare_products` ADD COLUMN `details` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `skincare_products` ADD COLUMN `timeOfUseEmoji` TEXT NOT NULL DEFAULT '☀️/🌙'")
                db.execSQL("ALTER TABLE `skincare_products` ADD COLUMN `frequency` TEXT NOT NULL DEFAULT 'Daily'")
            }
        }

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_records` ADD COLUMN `waterLogsJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_records` ADD COLUMN `weightLogKg` REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_records` ADD COLUMN `sleepHours` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `daily_records` ADD COLUMN `sleepRating` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `daily_records` ADD COLUMN `measurementsJson` TEXT NOT NULL DEFAULT '{}'")
            }
        }

        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_records` ADD COLUMN `completedSupplementsJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notion_spaces` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notion_blocks` (
                        `id` TEXT NOT NULL, `spaceId` TEXT NOT NULL, `parentId` TEXT, `orderIndex` INTEGER NOT NULL, 
                        `type` TEXT NOT NULL, `content` TEXT NOT NULL, `textFormatsJson` TEXT NOT NULL, 
                        `secondaryContent` TEXT NOT NULL, `icon` TEXT NOT NULL, `url` TEXT NOT NULL, 
                        `isChecked` INTEGER NOT NULL, `isExpanded` INTEGER NOT NULL, `progress` REAL NOT NULL, 
                        `tableDataJson` TEXT NOT NULL, `colWidthsJson` TEXT NOT NULL, `rowHeightsJson` TEXT NOT NULL, 
                        `calendarEventsJson` TEXT NOT NULL, `kanbanColumnsJson` TEXT NOT NULL, 
                        `sketchStrokesJson` TEXT NOT NULL, `galleryItemsJson` TEXT NOT NULL, 
                        `pinnedTab` TEXT NOT NULL, `isLocked` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), FOREIGN KEY(`spaceId`) REFERENCES `notion_spaces`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notion_blocks_spaceId` ON `notion_blocks` (`spaceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notion_blocks_parentId` ON `notion_blocks` (`parentId`)")

                val cursor = db.query("SELECT id, blocksJson FROM notion_spaces")
                while (cursor.moveToNext()) {
                    val spaceId = cursor.getString(0)
                    val blocksJson = cursor.getString(1)
                    try {
                        val blocks = kotlinx.serialization.json.Json.decodeFromString<List<com.example.loom.data.models.SandboxBlock>>(blocksJson)
                        val flatEntities = com.example.loom.ui.sandbox.flattenTree(blocks, spaceId)
                        flatEntities.forEach { b ->
                            val values = android.content.ContentValues().apply {
                                put("id", b.id); put("spaceId", b.spaceId); put("parentId", b.parentId); put("orderIndex", b.orderIndex)
                                put("type", b.type); put("content", b.content); put("textFormatsJson", b.textFormatsJson)
                                put("secondaryContent", b.secondaryContent); put("icon", b.icon); put("url", b.url)
                                put("isChecked", if (b.isChecked) 1 else 0); put("isExpanded", if (b.isExpanded) 1 else 0)
                                put("progress", b.progress); put("tableDataJson", b.tableDataJson); put("colWidthsJson", b.colWidthsJson)
                                put("rowHeightsJson", b.rowHeightsJson); put("calendarEventsJson", b.calendarEventsJson)
                                put("kanbanColumnsJson", b.kanbanColumnsJson); put("sketchStrokesJson", b.sketchStrokesJson)
                                put("galleryItemsJson", b.galleryItemsJson); put("pinnedTab", b.pinnedTab); put("isLocked", if (b.isLocked) 1 else 0)
                            }
                            db.insert("notion_blocks", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values)
                        }
                    } catch (_: Exception) { android.util.Log.e("Migration", "Failed parsing block for $spaceId") }
                }
                cursor.close()

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notion_spaces_new` (
                        `id` TEXT NOT NULL, `title` TEXT NOT NULL, `icon` TEXT NOT NULL, `bannerUrl` TEXT NOT NULL, 
                        `updatedAt` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, `tagsJson` TEXT NOT NULL, 
                        `isPrivate` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("INSERT INTO notion_spaces_new (id, title, icon, bannerUrl, updatedAt, isPinned, tagsJson, isPrivate, isDeleted) SELECT id, title, icon, bannerUrl, updatedAt, isPinned, tagsJson, isPrivate, isDeleted FROM notion_spaces")

                db.execSQL("DROP TABLE notion_spaces")
                db.execSQL("ALTER TABLE notion_spaces_new RENAME TO notion_spaces")
            }
        }

        // v33: indexes for the hot queries, and block ids scoped per space (PRIMARY KEY(spaceId, id))
        // so a block id reused in another space can never overwrite that space's row.
        private val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val blockColumns = "`id`, `spaceId`, `parentId`, `orderIndex`, `type`, `content`, `textFormatsJson`, " +
                    "`secondaryContent`, `icon`, `url`, `isChecked`, `isExpanded`, `progress`, `tableDataJson`, " +
                    "`colWidthsJson`, `rowHeightsJson`, `calendarEventsJson`, `kanbanColumnsJson`, `sketchStrokesJson`, " +
                    "`galleryItemsJson`, `pinnedTab`, `isLocked`"
                db.execSQL("CREATE TABLE IF NOT EXISTS `notion_blocks_new` (`id` TEXT NOT NULL, `spaceId` TEXT NOT NULL, `parentId` TEXT, `orderIndex` INTEGER NOT NULL, `type` TEXT NOT NULL, `content` TEXT NOT NULL, `textFormatsJson` TEXT NOT NULL, `secondaryContent` TEXT NOT NULL, `icon` TEXT NOT NULL, `url` TEXT NOT NULL, `isChecked` INTEGER NOT NULL, `isExpanded` INTEGER NOT NULL, `progress` REAL NOT NULL, `tableDataJson` TEXT NOT NULL, `colWidthsJson` TEXT NOT NULL, `rowHeightsJson` TEXT NOT NULL, `calendarEventsJson` TEXT NOT NULL, `kanbanColumnsJson` TEXT NOT NULL, `sketchStrokesJson` TEXT NOT NULL, `galleryItemsJson` TEXT NOT NULL, `pinnedTab` TEXT NOT NULL, `isLocked` INTEGER NOT NULL, PRIMARY KEY(`spaceId`, `id`), FOREIGN KEY(`spaceId`) REFERENCES `notion_spaces`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT OR IGNORE INTO `notion_blocks_new` ($blockColumns) SELECT $blockColumns FROM `notion_blocks`")
                db.execSQL("DROP TABLE `notion_blocks`")
                db.execSQL("ALTER TABLE `notion_blocks_new` RENAME TO `notion_blocks`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notion_blocks_spaceId` ON `notion_blocks` (`spaceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notion_blocks_parentId` ON `notion_blocks` (`parentId`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_targetDate` ON `tasks` (`targetDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_isCompleted_completionDateMillis` ON `tasks` (`isCompleted`, `completionDateMillis`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_dateId` ON `exercises` (`dateId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_notes_timestamp` ON `diary_notes` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_skincare_routines_isPinnedToHome` ON `skincare_routines` (`isPinnedToHome`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_items_name` ON `food_items` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_items_isUserCreated` ON `food_items` (`isUserCreated`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notion_spaces_isDeleted` ON `notion_spaces` (`isDeleted`)")
            }
        }

        // v34: full-text index over food names, kept in sync with food_items by triggers
        private val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `food_items_fts` USING FTS4(`name` TEXT NOT NULL, content=`food_items`)")
                FOOD_FTS_TRIGGERS.forEach { db.execSQL(it) }
                db.execSQL("INSERT INTO `food_items_fts`(`food_items_fts`) VALUES('rebuild')")
            }
        }

        private val FOOD_FTS_TRIGGERS = listOf(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_food_items_fts_BEFORE_UPDATE BEFORE UPDATE ON `food_items` BEGIN DELETE FROM `food_items_fts` WHERE `docid`=OLD.`rowid`; END",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_food_items_fts_BEFORE_DELETE BEFORE DELETE ON `food_items` BEGIN DELETE FROM `food_items_fts` WHERE `docid`=OLD.`rowid`; END",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_food_items_fts_AFTER_UPDATE AFTER UPDATE ON `food_items` BEGIN INSERT INTO `food_items_fts`(`docid`, `name`) VALUES (NEW.`rowid`, NEW.`name`); END",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_food_items_fts_AFTER_INSERT AFTER INSERT ON `food_items` BEGIN INSERT INTO `food_items_fts`(`docid`, `name`) VALUES (NEW.`rowid`, NEW.`name`); END"
        )

        /**
         * A fresh install copies the bundled foods in before the sync triggers exist, leaving the
         * index empty: build it once whenever it has no segments but there are foods.
         */
        val ensureFoodIndex = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                try {
                    val indexed = db.query("SELECT count(*) FROM `food_items_fts_segdir`").use { it.moveToFirst(); it.getLong(0) }
                    val foods = db.query("SELECT count(*) FROM `food_items`").use { it.moveToFirst(); it.getLong(0) }
                    if (indexed == 0L && foods > 0L) db.execSQL("INSERT INTO `food_items_fts`(`food_items_fts`) VALUES('rebuild')")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Food index check failed", e)
                }
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

        // Expose your migrations cleanly to your Hilt Module
        val MIGRATIONS = arrayOf(
            MIGRATION_TO_17, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19,
            MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23,
            MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27,
            MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31,
            MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34
        )
    }
}