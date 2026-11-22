package com.example.loom.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

object DateUtils {
    // --- Updated by HomeViewModel from the "New day starts at" setting ---
    private val rolloverHours = MutableStateFlow(2)

    var rolloverOffsetHours: Int
        get() = rolloverHours.value
        set(value) { rolloverHours.value = value }

    /** The "logical" today: times before the rollover hour still count as the previous day. */
    fun getLogicalToday(): LocalDate =
        LocalDateTime.now().minusHours(rolloverOffsetHours.toLong()).toLocalDate()

    /** Epoch millis at which the current logical day began (today's date at the rollover hour). */
    fun logicalDayStartMillis(): Long =
        getLogicalToday().atTime(rolloverOffsetHours, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    // ISO yyyy-MM-dd with ASCII digits regardless of device locale: these strings are database keys
    fun getTodayDateId(): String = getLogicalToday().toString()

    /**
     * Emits the logical date id now and again whenever it changes (midnight + rollover, clock or
     * time zone changes, or a new rollover setting), so screens left open overnight move to the new day.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayDateIdFlow: Flow<String> = rolloverHours.flatMapLatest {
        flow {
            while (true) {
                emit(getTodayDateId())
                delay(30.seconds)
            }
        }
    }.distinctUntilChanged()
}
