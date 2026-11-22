package com.example.loom

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.loom.data.backup.BackupManager
import com.example.loom.data.local.AppDatabase
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.data.local.entity.NotionSpaceEntity
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import com.example.loom.ui.sandbox.flattenTree
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Exports a ZIP backup (JSON + media) from an in-memory database and restores it, checking that
 * media files come back and image urls are re-pointed at this install. Uses an in-memory DB so the
 * app's real data is untouched (settings are written back with their own current values).
 */
@RunWith(AndroidJUnit4::class)
class BackupZipRoundTripTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var backupManager: BackupManager
    private lateinit var mediaFile: File
    private lateinit var userPreferences: UserPreferences
    private var savedPrefs = emptyList<com.example.loom.data.backup.PrefBackup>()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        userPreferences = UserPreferences(context)
        // Restores rewrite app settings: remember them and put them back afterwards
        savedPrefs = runBlocking { userPreferences.snapshot() }
        backupManager = BackupManager(context, db, db.trackerDao, db.sandboxDao, userPreferences)
        mediaFile = File(context.filesDir, "media_roundtrip_test.png").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
    }

    @After
    fun tearDown() {
        db.close()
        mediaFile.delete()
        runBlocking { userPreferences.restoreSnapshot(savedPrefs) }
    }

    @Test
    fun zipRoundTripRestoresDataAndMedia() = runBlocking {
        // A space whose image block points at a media file, as written by another install of the app
        val foreignUrl = "file:///data/user/0/com.example.loom/files/${mediaFile.name}"
        val space = SandboxSpace(id = "zip-space", title = "Zip test", blocks = listOf(
            SandboxBlock(id = "b1", type = BlockType.IMAGE, url = foreignUrl),
            SandboxBlock(id = "b2", type = BlockType.TEXT, content = "hello")
        ))
        db.sandboxDao.saveSpaceWithBlocks(NotionSpaceEntity.fromDomainModel(space), flattenTree(space.blocks, space.id))
        db.trackerDao.insertDailyRecord(DailyRecord(dateId = "2026-11-01", waterIntakeMl = 750))

        val zipBytes = ByteArrayOutputStream().also { backupManager.writeZip(it) }.toByteArray()
        assertTrue("ZIP magic", zipBytes[0] == 'P'.code.toByte() && zipBytes[1] == 'K'.code.toByte())

        // Simulate a fresh install: media gone, database emptied
        mediaFile.delete()
        db.sandboxDao.wipeAllSpaces()
        db.trackerDao.wipeDailyRecords()

        val skipped = backupManager.restoreFrom(ByteArrayInputStream(zipBytes))
        assertEquals(0, skipped)

        assertTrue("media restored", mediaFile.exists())
        assertEquals(listOf<Byte>(1, 2, 3, 4), mediaFile.readBytes().toList())

        val restored = db.sandboxDao.getAllSpacesOnce().single { it.space.id == "zip-space" }.toDomainModel()
        assertEquals(2, restored.blocks.size)
        val imageUrl = restored.blocks.first { it.id == "b1" }.url
        assertEquals("file://${context.filesDir.absolutePath}/${mediaFile.name}", imageUrl)
        assertEquals(750, db.trackerDao.getDailyRecordSync("2026-11-01")?.waterIntakeMl)
    }

    @Test
    fun plainJsonBackupStillImports() = runBlocking {
        val json = """{ "dailyRecords": [ { "dateId": "2026-10-10", "waterIntakeMl": 300 } ] }"""
        backupManager.restoreFrom(ByteArrayInputStream(json.toByteArray()))
        assertEquals(300, db.trackerDao.getDailyRecordSync("2026-10-10")?.waterIntakeMl)
    }
}
