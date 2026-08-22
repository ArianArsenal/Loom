package com.example.loom.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    // --- OPTIMIZATION: ThreadLocal prevents allocating a new heavy formatter every single call ---
    private val formatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }
    }

    fun getTodayDateId(): String {
        // Subtract exactly 2 hours (7,200,000 milliseconds) from the current time.
        // This ensures that any time between 00:00 and 01:59 AM still registers as the previous day.
        val logicalTimeMillis = System.currentTimeMillis() - (2 * 60 * 60 * 1000L)
        return formatter.get()?.format(Date(logicalTimeMillis)) ?: ""
    }
}