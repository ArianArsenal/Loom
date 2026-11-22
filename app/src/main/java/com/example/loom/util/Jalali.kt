package com.example.loom.util

import java.time.LocalDate

/** A date in the Solar Hijri (Jalali) calendar. Months are 1-based (1 = Farvardin). */
data class JalaliDate(val year: Int, val month: Int, val day: Int)

/**
 * Gregorian <-> Jalali conversion (the jalaali algorithm, valid for Jalali years -61..3177).
 * Only used for display: dates are always stored as ISO Gregorian strings.
 */
object Jalali {
    private val breaks = intArrayOf(-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178)

    val monthNames = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")

    private class Cal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): Cal {
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        require(jy >= jp && jy < breaks.last()) { "Jalali year out of range: $jy" }
        var jump = 0
        for (i in 1 until breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += jump / 33 * 8 + (jump % 33) / 4
            jp = jm
        }
        var n = jy - jp
        leapJ += n / 33 * 8 + ((n % 33) + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + (jump + 4) / 33 * 33
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return Cal(leap, gy, march)
    }

    fun toGregorian(year: Int, month: Int, day: Int): LocalDate {
        val cal = jalCal(year)
        val start = LocalDate.of(cal.gy, 3, cal.march).toEpochDay()
        return LocalDate.ofEpochDay(start + (month - 1) * 31 - (month / 7) * (month - 7) + day - 1)
    }

    fun fromGregorian(date: LocalDate): JalaliDate {
        val epochDay = date.toEpochDay()
        var jy = date.year - 621
        val cal = jalCal(jy)
        val firstDay = LocalDate.of(cal.gy, 3, cal.march).toEpochDay()
        var k = (epochDay - firstDay).toInt()
        if (k >= 0) {
            if (k <= 185) return JalaliDate(jy, 1 + k / 31, k % 31 + 1)
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (cal.leap == 1) k += 1
        }
        return JalaliDate(jy, 7 + k / 30, k % 30 + 1)
    }

    fun monthLength(year: Int, month: Int): Int = when {
        month <= 6 -> 31
        month <= 11 -> 30
        else -> (toGregorian(year + 1, 1, 1).toEpochDay() - toGregorian(year, 12, 1).toEpochDay()).toInt()
    }

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun persianDigits(text: String): String = text.map { if (it in '0'..'9') persianDigits[it - '0'] else it }.joinToString("")
}
