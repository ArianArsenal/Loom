package com.example.loom.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.data.local.entity.DiaryNote
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.ExerciseTemplate
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.data.local.entity.PresetExercise
import com.example.loom.data.local.entity.SubTask
import com.example.loom.data.local.entity.Task
import com.example.loom.data.local.entity.TaskSection
import com.example.loom.data.local.entity.WorkoutPreset
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.data.repository.TrackerRepository
import com.example.loom.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TrackerRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val todayRecord: StateFlow<DailyRecord?> = repository.getTodayRecord().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val allTasks: StateFlow<List<Task>> = repository.getAllTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val todayTasks: StateFlow<List<Task>> = repository.getTasksForDate(DateUtils.getTodayDateId()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTaskSections: StateFlow<List<TaskSection>> = repository.getAllTaskSections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayExercises: StateFlow<List<Exercise>> = repository.getExercisesForDate(DateUtils.getTodayDateId()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- UPDATED MACROS & SETTINGS ---
    val targetCalories: StateFlow<Int> = userPreferences.targetCaloriesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2800)
    val targetProtein: StateFlow<Int> = userPreferences.targetProteinFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 140)
    val targetCarbs: StateFlow<Int> = userPreferences.targetCarbsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 350)
    val targetFats: StateFlow<Int> = userPreferences.targetFatsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 75)
    val defaultWaterIncrement: StateFlow<Int> = userPreferences.defaultWaterIncrementFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 250)

    val allPresets: StateFlow<List<WorkoutPreset>> = repository.getAllPresets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val exerciseTemplates: StateFlow<List<ExerciseTemplate>> = repository.getAllTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDiaryNotes: StateFlow<List<DiaryNote>> = repository.getAllDiaryNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allNoteTags: StateFlow<List<NoteTag>> = repository.getAllNoteTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedSkincareRoutines: StateFlow<List<com.example.loom.data.local.entity.SkincareRoutine>> = repository.getPinnedSkincareRoutines().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _diaryViewMode = MutableStateFlow(1)
    val diaryViewMode: StateFlow<Int> = _diaryViewMode.asStateFlow()
    fun setDiaryViewMode(mode: Int) { _diaryViewMode.value = mode }

    init {
        viewModelScope.launch {
            repository.initializeTodayRecord()
            repository.prepopulateTemplatesIfEmpty()
            repository.prepopulateTagsIfEmpty()
            repository.prepopulateTaskSectionsIfEmpty()
            repository.prepopulateFoodPantryIfEmpty() // <-- NEW FOOD PANTRY
            repository.cleanupOldDoneTasks()
        }
    }

    // --- UPDATED HYDRATION & NUTRITION ---
    fun addWater() = viewModelScope.launch { repository.addWater(defaultWaterIncrement.value) }
    fun removeWater() = viewModelScope.launch { repository.removeWater(defaultWaterIncrement.value) }

    val allFoodItems: StateFlow<List<com.example.loom.data.local.entity.FoodItem>> = repository.getAllFoodItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFoodItemToPantry(food: com.example.loom.data.local.entity.FoodItem) = viewModelScope.launch { repository.insertFoodItem(food) }

    fun logFoodItem(food: com.example.loom.data.local.entity.FoodItem, servings: Float) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        val totalCals = (food.calories * servings).toInt()
        val totalPro = (food.protein * servings).toInt()
        val totalCarbs = (food.carbs * servings).toInt()
        val totalFats = (food.fats * servings).toInt()

        val log = com.example.loom.data.local.entity.ConsumedFoodLog(
            id = java.util.UUID.randomUUID().toString(),
            foodName = food.name, emoji = food.emoji, servings = servings,
            calories = totalCals, protein = totalPro, carbs = totalCarbs, fats = totalFats,
            timestamp = System.currentTimeMillis()
        )

        val currentLogs = try { Json.decodeFromString<List<com.example.loom.data.local.entity.ConsumedFoodLog>>(record.consumedFoodsJson) } catch (e: Exception) { emptyList() }
        repository.logConsumedFood(totalCals, totalPro, totalCarbs, totalFats, Json.encodeToString(currentLogs + log))
    }
    fun editNutrition(cals: Int, pro: Int, carbs: Int, fats: Int) = viewModelScope.launch { repository.setNutrition(cals, pro, carbs, fats) }

    fun toggleSkincareRoutine(routineName: String) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        try {
            val completedList = Json.decodeFromString<List<String>>(record.completedSkincareRoutinesJson).toMutableList()
            if (completedList.contains(routineName)) completedList.remove(routineName) else completedList.add(routineName)
            repository.updateCompletedSkincareRoutines(Json.encodeToString(completedList))
        } catch (e: Exception) {
            repository.updateCompletedSkincareRoutines(Json.encodeToString(listOf(routineName)))
        }
    }

    fun toggleSkincareStep(routineName: String, productName: String) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        val stepId = "${routineName}_${productName}"
        try {
            val completedList = Json.decodeFromString<List<String>>(record.completedSkincareStepsJson).toMutableList()
            if (completedList.contains(stepId)) completedList.remove(stepId) else completedList.add(stepId)
            repository.updateCompletedSkincareSteps(Json.encodeToString(completedList))
        } catch (e: Exception) {
            repository.updateCompletedSkincareSteps(Json.encodeToString(listOf(stepId)))
        }
    }

    fun updateDiaryNote(note: String) = viewModelScope.launch { repository.updateDiaryNote(note) }

    fun addTask(task: Task) = viewModelScope.launch { repository.insertTask(task) }
    fun updateTask(task: Task) = viewModelScope.launch { repository.updateTask(task) }

    fun updateTaskStatus(task: Task, newStatus: String) = viewModelScope.launch {
        val isNowDone = newStatus == "DONE"
        val wasDone = task.isCompleted

        val completionTime = if (isNowDone && !wasDone) {
            System.currentTimeMillis()
        } else if (isNowDone && wasDone) {
            task.completionDateMillis
        } else {
            0L
        }

        if (isNowDone && !wasDone && task.isRecurring && task.recurringInterval != "NONE") {
            spawnNextRecurringTask(task)
            repository.updateTask(task.copy(status = newStatus, isCompleted = true, isRecurring = false, completionDateMillis = completionTime))
        } else {
            repository.updateTask(task.copy(status = newStatus, isCompleted = isNowDone, completionDateMillis = completionTime))
        }
    }

    fun toggleTaskCompletion(task: Task) {
        val newStatus = if (task.isCompleted) "TO DO" else "DONE"
        updateTaskStatus(task, newStatus)
    }

    fun toggleSubtaskCompletion(task: Task, subtaskId: String) = viewModelScope.launch {
        try {
            val subtasks = Json.decodeFromString<List<SubTask>>(task.subtasksJson).toMutableList()
            val index = subtasks.indexOfFirst { it.id == subtaskId }
            if (index != -1) {
                subtasks[index] = subtasks[index].copy(isCompleted = !subtasks[index].isCompleted)
                repository.updateTask(task.copy(subtasksJson = Json.encodeToString(subtasks)))
            }
        } catch (e: Exception) { /* JSON Error */ }
    }

    private suspend fun spawnNextRecurringTask(task: Task) {
        val parts = task.recurringInterval.split(" ")
        val amount = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val type = parts.getOrNull(1) ?: "DAYS"

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = try { format.parse(task.targetDate) } catch (e: Exception) { Date() }

        val calendar = Calendar.getInstance().apply { time = currentDate }
        when (type) {
            "DAYS" -> calendar.add(Calendar.DAY_OF_YEAR, amount)
            "WEEKS" -> calendar.add(Calendar.WEEK_OF_YEAR, amount)
            "MONTHS" -> calendar.add(Calendar.MONTH, amount)
        }
        val nextDateStr = format.format(calendar.time)

        val subtasks = try { Json.decodeFromString<List<SubTask>>(task.subtasksJson) } catch (e: Exception) { emptyList() }
        val resetSubtasks = subtasks.map { it.copy(isCompleted = false) }

        val nextTask = task.copy(
            id = 0,
            targetDate = nextDateStr,
            isCompleted = false,
            status = "TO DO",
            completionDateMillis = 0L,
            subtasksJson = Json.encodeToString(resetSubtasks)
        )
        repository.insertTask(nextTask)
    }

    fun deleteTask(task: Task) = viewModelScope.launch { repository.deleteTask(task) }

    fun addTaskSection(name: String, order: Int = 0) = viewModelScope.launch { repository.insertTaskSection(TaskSection(name = name, orderIndex = order)) }
    fun deleteTaskSection(section: TaskSection) = viewModelScope.launch { repository.deleteTaskSection(section) }

    fun addExercise(name: String, sets: Int, reps: Int, weight: Int, isTimeBased: Boolean, duration: Int, presetName: String? = null) = viewModelScope.launch {
        repository.insertExercise(name, sets, reps, weight, isTimeBased, duration, presetName)
        repository.insertTemplateIfNotExists(name)
    }
    fun deleteExercise(exercise: Exercise) = viewModelScope.launch { repository.deleteExercise(exercise) }
    fun savePreset(id: Int, name: String, exercises: List<PresetExercise>) = viewModelScope.launch {
        repository.insertPreset(WorkoutPreset(id = id, presetName = name, exercises = exercises))
        exercises.forEach { repository.insertTemplateIfNotExists(it.name) }
    }
    fun deletePreset(preset: WorkoutPreset) = viewModelScope.launch { repository.deletePreset(preset) }
    fun logPresetExercises(presetName: String, exercises: List<PresetExercise>) = viewModelScope.launch {
        exercises.forEach { repository.insertExercise(it.name, it.sets, it.reps, it.weight, it.isTimeBased, it.durationInSeconds, presetName) }
    }
    fun updateTemplate(template: ExerciseTemplate) = viewModelScope.launch { repository.updateTemplate(template) }
    fun deleteTemplate(template: ExerciseTemplate) = viewModelScope.launch { repository.deleteTemplate(template) }
    suspend fun saveDiaryNoteWithReturn(id: Int, title: String, content: String, tag: String, imagesJson: String, blocksJson: String, isFav: Boolean, isPriv: Boolean, isPinned: Boolean, isChecklist: Boolean): Long {
        val note = DiaryNote(id = id, title = title, content = content, tag = tag, imagesJson = imagesJson, blocksJson = blocksJson, isFavorite = isFav, isPrivate = isPriv, isPinned = isPinned, isChecklistMode = isChecklist)
        return repository.insertDiaryNoteAndGetId(note)
    }
    fun deleteDiaryNote(note: DiaryNote) = viewModelScope.launch { repository.deleteDiaryNote(note) }
    fun addNoteTag(name: String, colorHex: String) = viewModelScope.launch { repository.insertNoteTag(name, colorHex) }
    fun deleteNoteTag(tag: NoteTag) = viewModelScope.launch { repository.deleteNoteTag(tag) }
}