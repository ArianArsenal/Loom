package com.example.loom

import com.example.loom.data.backup.BackupJsonNormalizer
import com.example.loom.ui.home.LoomBackup
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackupJsonNormalizerTest {

    // Shaped like a backup exported by an old release build: obfuscated preset-exercise keys,
    // a task missing fields that have defaults, and an invalid daily record without its date.
    private val oldReleaseBackup = """
        {
          "dailyRecords": [
            { "dateId": "2026-11-01", "waterIntakeMl": 500 },
            { "waterIntakeMl": 250 }
          ],
          "tasks": [ { "id": 3, "title": "Pay rent", "targetDate": "2026-11-30" } ],
          "workoutPresets": [
            { "id": 1, "presetName": "Push", "exercises": [
              { "a": "Incline Dumbbell Press", "b": 3, "c": 10, "d": 60.0, "e": "[10.0,12.5,15.0]", "f": false, "g": 60 },
              { "b": 3 }
            ] }
          ],
          "notionSpaces": [ { "id": "s1", "blocks": [] }, { "title": "no id" } ]
        }
    """.trimIndent()

    private fun restore(json: String): Pair<LoomBackup, Int> {
        val result = BackupJsonNormalizer.normalize(JsonParser.parseString(json).asJsonObject)
        return Gson().fromJson(result.backup, LoomBackup::class.java) to result.skippedItems
    }

    @Test
    fun obfuscatedPresetExercisesAreMappedBack() {
        val (backup, _) = restore(oldReleaseBackup)
        val exercise = backup.workoutPresets.single().exercises.single()
        assertEquals("Incline Dumbbell Press", exercise.name)
        assertEquals(3, exercise.sets)
        assertEquals(10, exercise.reps)
        assertEquals(60f, exercise.weight)
        assertEquals("[10.0,12.5,15.0]", exercise.weightListJson)
        assertEquals(false, exercise.isTimeBased)
        assertEquals(60, exercise.durationInSeconds)
    }

    @Test
    fun missingFieldsGetDefaultsInsteadOfNull() {
        val (backup, _) = restore(oldReleaseBackup)
        val task = backup.tasks.single()
        assertNotNull(task.status)
        assertNotNull(task.subtasksJson)
        assertEquals("TO DO", task.status)
        val record = backup.dailyRecords.single()
        assertEquals("2026-11-01", record.dateId)
        assertNotNull(record.waterLogsJson)
        assertEquals("[]", record.completedSupplementsJson)
        assertEquals("", backup.notionSpaces.single().title)
    }

    @Test
    fun invalidRecordsAreSkippedAndCounted() {
        val (backup, skipped) = restore(oldReleaseBackup)
        assertEquals(1, backup.dailyRecords.size)
        assertEquals(1, backup.notionSpaces.size)
        // record without dateId + preset exercise without name + space without id
        assertEquals(3, skipped)
    }
}
