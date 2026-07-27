package com.example.loom.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    fun getTodayDateId(): String {
        // Subtract exactly 2 hours (7,200,000 milliseconds) from the current time.
        // This ensures that any time between 00:00 and 01:59 AM still registers as the previous day.
        val logicalTimeMillis = System.currentTimeMillis() - (2 * 60 * 60 * 1000L)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date(logicalTimeMillis))
    }
}