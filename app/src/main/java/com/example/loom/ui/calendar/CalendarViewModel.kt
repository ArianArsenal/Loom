package com.example.loom.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.ExerciseTemplate
import com.example.loom.data.local.entity.PresetExercise
import com.example.loom.data.local.entity.Task
import com.example.loom.data.local.entity.WorkoutPreset
import com.example.loom.data.repository.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

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

    private val _selectedDateId = MutableStateFlow<String?>(null)
    val selectedDateId: StateFlow<String?> = _selectedDateId.asStateFlow()

    private val _selectedDateSnapshot = MutableStateFlow<DailySnapshot?>(null)
    val selectedDateSnapshot: StateFlow<DailySnapshot?> = _selectedDateSnapshot.asStateFlow()

    val allPresets: StateFlow<List<WorkoutPreset>> = repository.getAllPresets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val exerciseTemplates: StateFlow<List<ExerciseTemplate>> = repository.getAllTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyRecords: StateFlow<Map<String, DailyRecord>> = currentMonth
        .flatMapLatest { yearMonth ->
            val prefix = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            repository.getRecordsForMonth(prefix)
        }
        .map { list -> list.associateBy { it.dateId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeDays: StateFlow<Set<String>> = currentMonth
        .flatMapLatest { yearMonth ->
            val prefix = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            combine(
                repository.getRecordsForMonth(prefix),
                repository.getExercisesForMonth(prefix),
                repository.getAllTasks()
            ) { records, exercises, tasks ->
                val activeSet = mutableSetOf<String>()

                records.forEach { record ->
                    // FIX 1: Strip all whitespaces, newlines, and tabs to completely prevent Ghost Highlights
                    val cleanFoods = record.consumedFoodsJson.replace("\\s".toRegex(), "")
                    val hasFood = cleanFoods != "" && cleanFoods != "[]" && cleanFoods != "null"

                    val cleanRoutines = record.completedSkincareRoutinesJson.replace("\\s".toRegex(), "")
                    val hasRoutine = cleanRoutines != "" && cleanRoutines != "[]" && cleanRoutines != "null"

                    val cleanSteps = record.completedSkincareStepsJson.replace("\\s".toRegex(), "")
                    val hasSteps = cleanSteps != "" && cleanSteps != "[]" && cleanSteps != "null"

                    // FIX 2: Check for ANY macro, not just calories
                    val hasMacros = record.calories > 0 || record.protein > 0 || record.carbs > 0 || record.fats > 0

                    if (hasMacros || record.waterIntakeMl > 0 || record.diaryEntry.isNotBlank() || hasFood || hasRoutine || hasSteps) {
                        activeSet.add(record.dateId)
                    }
                }

                exercises.forEach { ex -> activeSet.add(ex.dateId) }
                tasks.forEach { task ->
                    if (task.targetDate.startsWith(prefix)) activeSet.add(task.targetDate)
                }

                activeSet
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun nextMonth() { currentMonth.value = currentMonth.value.plusMonths(1) }
    fun previousMonth() { currentMonth.value = currentMonth.value.minusMonths(1) }

    private var snapshotJob: Job? = null

    fun selectDate(dateId: String) {
        _selectedDateId.value = dateId

        snapshotJob?.cancel()
        snapshotJob = viewModelScope.launch {
            combine(
                repository.getDailyRecordFlow(dateId),
                repository.getTasksForDate(dateId),
                repository.getExercisesForDate(dateId)
            ) { record, tasks, exercises ->
                DailySnapshot(record, tasks, exercises)
            }.collect { snapshot ->
                _selectedDateSnapshot.value = snapshot
            }
        }
    }

    fun clearSelectedDate() {
        _selectedDateId.value = null
        _selectedDateSnapshot.value = null
        snapshotJob?.cancel()
    }

    fun updatePastMacros(cals: Int, pro: Int, carbs: Int, fats: Int) {
        val dateId = _selectedDateId.value ?: return
        viewModelScope.launch { repository.setNutrition(cals, pro, carbs, fats, targetDateId = dateId) }
    }

    fun addPastWater(amountMl: Int = 250) {
        val dateId = _selectedDateId.value ?: return
        viewModelScope.launch { repository.addWater(amountMl, targetDateId = dateId) }
    }

    fun removePastWater(amountMl: Int = 250) {
        val dateId = _selectedDateId.value ?: return
        viewModelScope.launch { repository.removeWater(amountMl, targetDateId = dateId) }
    }

    fun deletePastExercise(exercise: Exercise) {
        viewModelScope.launch { repository.deleteExercise(exercise) }
    }

    fun createExerciseTemplate(name: String) = viewModelScope.launch {
        repository.insertTemplateIfNotExists(name)
    }

    fun addPastExercise(name: String, sets: Int, reps: Int, weight: Float, weightListJson: String = "[]", isTimeBased: Boolean, duration: Int) {
        val dateId = _selectedDateId.value ?: return
        viewModelScope.launch {
            repository.insertExercise(
                name = name, sets = sets, reps = reps, weight = weight, weightListJson = weightListJson,
                isTimeBased = isTimeBased, duration = duration, targetDateId = dateId
            )
            repository.insertTemplateIfNotExists(name)
        }
    }

    fun logPresetExercisesToPast(presetName: String, exercises: List<PresetExercise>) {
        val dateId = _selectedDateId.value ?: return
        viewModelScope.launch {
            exercises.forEach {
                repository.insertExercise(
                    name = it.name, sets = it.sets, reps = it.reps, weight = it.weight,
                    weightListJson = it.weightListJson, isTimeBased = it.isTimeBased,
                    duration = it.durationInSeconds, presetName = presetName, targetDateId = dateId
                )
            }
        }
    }
}