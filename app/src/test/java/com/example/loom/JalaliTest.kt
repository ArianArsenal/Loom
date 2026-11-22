package com.example.loom

import com.example.loom.util.Jalali
import com.example.loom.util.JalaliDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class JalaliTest {
    @Test fun nowruzDates() {
        assertEquals(LocalDate.of(2021, 3, 21), Jalali.toGregorian(1400, 1, 1))
        assertEquals(LocalDate.of(2023, 3, 21), Jalali.toGregorian(1402, 1, 1))
        assertEquals(LocalDate.of(2024, 3, 20), Jalali.toGregorian(1403, 1, 1))
    }

    @Test fun fromGregorian() {
        assertEquals(JalaliDate(1403, 1, 1), Jalali.fromGregorian(LocalDate.of(2024, 3, 20)))
        assertEquals(JalaliDate(1402, 12, 29), Jalali.fromGregorian(LocalDate.of(2024, 3, 19)))
        assertEquals(JalaliDate(1404, 9, 1), Jalali.fromGregorian(LocalDate.of(2025, 11, 22)))
    }

    @Test fun roundTripEveryDayForThreeYears() {
        var d = LocalDate.of(2024, 1, 1)
        while (d.isBefore(LocalDate.of(2027, 1, 1))) {
            val j = Jalali.fromGregorian(d)
            assertEquals(d, Jalali.toGregorian(j.year, j.month, j.day))
            d = d.plusDays(1)
        }
    }

    @Test fun monthLengths() {
        assertEquals(31, Jalali.monthLength(1403, 1))
        assertEquals(30, Jalali.monthLength(1403, 7))
        assertEquals(30, Jalali.monthLength(1403, 12)) // 1403 is a leap year
        assertEquals(29, Jalali.monthLength(1402, 12))
    }

    @Test fun digits() = assertEquals("۱۴۰۵/۹/۲", Jalali.persianDigits("1405/9/2"))
}
