package com.example.loom.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    // --- NEW: Dynamically updated by HomeViewModel on startup ---
    var rolloverOffsetHours: Int = 2

    // OPTIMIZATION: ThreadLocal prevents allocating a new heavy formatter every single call
    private val formatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }
    }

    fun getTodayDateId(): String {
        // Subtract the user's custom offset (e.g., 2 hours = 7,200,000 milliseconds) from the current time.
        // This ensures that any time before the cutoff still registers as the previous calendar day.
        val logicalTimeMillis = System.currentTimeMillis() - (rolloverOffsetHours * 60 * 60 * 1000L)
        return formatter.get()?.format(Date(logicalTimeMillis)) ?: ""
    }
}