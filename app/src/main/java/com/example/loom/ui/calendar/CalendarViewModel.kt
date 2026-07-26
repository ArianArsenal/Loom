package com.example.loom.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.data.repository.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: TrackerRepository
) : ViewModel() {

    val currentMonth = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyRecords: StateFlow<Map<String, DailyRecord>> = currentMonth
        .flatMapLatest { yearMonth ->
            val prefix = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            repository.getRecordsForMonth(prefix)
        }
        .map { list -> list.associateBy { it.dateId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun nextMonth() {
        currentMonth.value = currentMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        currentMonth.value = currentMonth.value.minusMonths(1)
    }
}