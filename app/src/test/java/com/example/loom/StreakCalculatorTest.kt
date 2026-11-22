package com.example.loom

import com.example.loom.ui.health.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 11, 23)

    private fun streakOf(vararg days: Boolean?): Pair<Int, Int> {
        val from = today.minusDays((days.size - 1).toLong())
        val s = StreakCalculator.compute({ d -> days[(d.toEpochDay() - from.toEpochDay()).toInt()] }, from, today)
        return s.current to s.best
    }

    @Test fun consecutiveDaysEndingToday() = assertEquals(3 to 3, streakOf(false, true, true, true))

    @Test fun todayNotDoneYetKeepsStreak() = assertEquals(2 to 2, streakOf(true, true, false))

    @Test fun missedDayBreaksStreakButKeepsBest() = assertEquals(1 to 3, streakOf(true, true, true, false, true))

    @Test fun notApplicableDaysNeitherCountNorBreak() = assertEquals(2 to 2, streakOf(true, null, true))

    @Test fun nothingDone() = assertEquals(0 to 0, streakOf(false, false))
}
