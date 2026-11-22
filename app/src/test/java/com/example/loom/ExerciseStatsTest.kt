package com.example.loom

import com.example.loom.data.local.entity.Exercise
import com.example.loom.ui.health.ExerciseStats
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseStatsTest {
    private val history = listOf( // newest first, as the DAO returns it
        Exercise(id = 3, dateId = "2026-11-20", exerciseName = "Press", sets = 3, reps = 8, weight = 0f, weightListJson = "[60.0,65.0,70.0]"),
        Exercise(id = 2, dateId = "2026-11-13", exerciseName = "Press", sets = 3, reps = 10, weight = 60f),
        Exercise(id = 1, dateId = "2026-11-13", exerciseName = "Press", sets = 1, reps = 5, weight = 62.5f)
    )

    @Test
    fun personalRecordsUsePerSetWeights() {
        val stats = ExerciseStats.from(history)
        assertEquals(70f, stats.prWeight)
        assertEquals(1800f, stats.prVolume) // 3 × 10 × 60 beats (60+65+70) × 8 = 1560
        assertEquals(3, stats.last?.id)
    }

    @Test
    fun sessionsAreOneBestPerDayOldestFirst() {
        assertEquals(listOf("2026-11-13" to 62.5f, "2026-11-20" to 70f), ExerciseStats.from(history).sessions)
    }
}
