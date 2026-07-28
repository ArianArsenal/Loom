package com.example.loom.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.Task
import com.example.loom.data.repository.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// A simple data class to hold the historical snapshot for the Time Machine
data class DailySnapshot(
    val record: DailyRecord?,
    val tasks: List<Task>,
    val exercises: List<Exercise>
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: TrackerRepository
) : ViewModel() {

    val currentMonth = MutableStateFlow(YearMonth.now())

    // Tracks which date the user tapped on
    private val _selectedDateId = MutableStateFlow<String?>(null)
    val selectedDateId: StateFlow<String?> = _selectedDateId.asStateFlow()

    // The data for the selected date
    private val _selectedDateSnapshot = MutableStateFlow<DailySnapshot?>(null)
    val selectedDateSnapshot: StateFlow<DailySnapshot?> = _selectedDateSnapshot.asStateFlow()

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

    fun selectDate(dateId: String) {
        _selectedDateId.value = dateId
        fetchSnapshot(dateId)
    }

    fun clearSelectedDate() {
        _selectedDateId.value = null
        _selectedDateSnapshot.value = null
    }

    private fun fetchSnapshot(dateId: String) {
        viewModelScope.launch {
            // Combine flows for the chosen date to build a complete snapshot
            combine(
                repository.getTasksForDate(dateId),
                repository.getExercisesForDate(dateId)
            ) { tasks, exercises ->
                // Because records are retrieved from our Monthly Map, we don't need a live flow here
                // However, if we wanted it fully reactive, we could query the DAO directly.
                // For now, let's keep it simple: we pass the tasks and exercises
                Pair(tasks, exercises)
            }.collect { (tasks, exercises) ->
                // Grab the record from the already loaded monthly map
                val record = monthlyRecords.value[dateId]
                _selectedDateSnapshot.value = DailySnapshot(record, tasks, exercises)
            }
        }
    }
}