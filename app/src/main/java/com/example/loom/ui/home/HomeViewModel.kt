package com.example.loom.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.entity.ConsumedFoodLog
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.data.local.entity.DiaryNote
import com.example.loom.data.local.entity.FoodItem
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.data.local.entity.SubTask
import com.example.loom.data.local.entity.Task
import com.example.loom.data.local.entity.TaskSection
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.data.repository.TrackerRepository
import com.example.loom.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.debounce
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TrackerRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val isBiometricEnabled: StateFlow<Boolean> = userPreferences.biometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // 2. Add this function down with your other functions:
    fun setBiometricEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferences.setBiometricEnabled(enabled)
    }

    val appTheme: StateFlow<String> = userPreferences.appThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")

    // Add this with your other functions:
    fun setAppTheme(theme: String) = viewModelScope.launch {
        userPreferences.saveAppTheme(theme)
    }

    val todayRecord: StateFlow<DailyRecord?> = repository.getTodayRecord().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val consumedFoods: StateFlow<List<ConsumedFoodLog>> = todayRecord
        .map { try { Json.decodeFromString<List<ConsumedFoodLog>>(it?.consumedFoodsJson ?: "[]") } catch (e: Exception) { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedSkincareRoutines: StateFlow<List<String>> = todayRecord
        .map { try { Json.decodeFromString<List<String>>(it?.completedSkincareRoutinesJson ?: "[]") } catch (e: Exception) { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedSkincareSteps: StateFlow<List<String>> = todayRecord
        .map { try { Json.decodeFromString<List<String>>(it?.completedSkincareStepsJson ?: "[]") } catch (e: Exception) { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<Task>> = repository.getAllTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val todayTasks: StateFlow<List<Task>> = repository.getTasksForDate(DateUtils.getTodayDateId()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTaskSections: StateFlow<List<TaskSection>> = repository.getAllTaskSections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val targetCalories: StateFlow<Int> = userPreferences.targetCaloriesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2800)
    val targetProtein: StateFlow<Int> = userPreferences.targetProteinFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 140)
    val targetCarbs: StateFlow<Int> = userPreferences.targetCarbsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 350)
    val targetFats: StateFlow<Int> = userPreferences.targetFatsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 75)
    val defaultWaterIncrement: StateFlow<Int> = userPreferences.defaultWaterIncrementFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 250)

    val allDiaryNotes: StateFlow<List<DiaryNote>> = repository.getAllDiaryNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allNoteTags: StateFlow<List<NoteTag>> = repository.getAllNoteTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedSkincareRoutines: StateFlow<List<com.example.loom.data.local.entity.SkincareRoutine>> = repository.getPinnedSkincareRoutines().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _diaryViewMode = MutableStateFlow(1)
    val diaryViewMode: StateFlow<Int> = _diaryViewMode.asStateFlow()
    fun setDiaryViewMode(mode: Int) { _diaryViewMode.value = mode }

    private val _foodSearchQuery = MutableStateFlow("")
    val foodSearchQuery: StateFlow<String> = _foodSearchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchedFoodItems: StateFlow<List<FoodItem>> = _foodSearchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.searchFoodItems("مرغ") // Defaults to showing chicken-related items
            } else {
                repository.searchFoodItems(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeTodayRecord()
            repository.prepopulateTemplatesIfEmpty()
            repository.prepopulateTagsIfEmpty()
            repository.prepopulateTaskSectionsIfEmpty()
            repository.cleanupOldDoneTasks()
        }
    }

    fun addWater() = viewModelScope.launch { repository.addWater(defaultWaterIncrement.value) }
    fun removeWater() = viewModelScope.launch { repository.removeWater(defaultWaterIncrement.value) }

    fun updateFoodSearchQuery(query: String) {
        _foodSearchQuery.value = query
    }

    fun addFoodItemToPantry(food: FoodItem) = viewModelScope.launch { repository.insertFoodItem(food) }

    fun logFoodItem(food: FoodItem, servings: Float) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch

        val totalCals = (food.calories * servings).toInt()
        val totalPro = (food.protein * servings).toInt()
        val totalCarbs = (food.carbs * servings).toInt()
        val totalFats = (food.fats * servings).toInt()
        val currentTime = System.currentTimeMillis()

        val log = ConsumedFoodLog(
            id = java.util.UUID.randomUUID().toString(),
            foodName = food.name,
            emoji = getMealEmojiForTime(currentTime), // <--- DYNAMIC EMOJI
            servings = servings,
            calories = totalCals, protein = totalPro, carbs = totalCarbs, fats = totalFats,
            timestamp = currentTime
        )

        val currentLogs = try { Json.decodeFromString<List<ConsumedFoodLog>>(record.consumedFoodsJson) } catch (e: Exception) { emptyList() }
        repository.logConsumedFood(totalCals, totalPro, totalCarbs, totalFats, Json.encodeToString(currentLogs + log))
    }

    fun editNutrition(cals: Int, pro: Int, carbs: Int, fats: Int) = viewModelScope.launch { repository.setNutrition(cals, pro, carbs, fats) }

    fun toggleSkincareRoutine(routineName: String) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        val currentList = try { Json.decodeFromString<List<String>>(record.completedSkincareRoutinesJson) } catch (e: Exception) { emptyList() }.toMutableList()
        if (currentList.contains(routineName)) currentList.remove(routineName) else currentList.add(routineName)
        repository.updateCompletedSkincareRoutines(Json.encodeToString(currentList))
    }

    fun toggleSkincareStep(routineName: String, productName: String) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        val stepId = "${routineName}_${productName}"
        val currentList = try { Json.decodeFromString<List<String>>(record.completedSkincareStepsJson) } catch (e: Exception) { emptyList() }.toMutableList()
        if (currentList.contains(stepId)) currentList.remove(stepId) else currentList.add(stepId)
        repository.updateCompletedSkincareSteps(Json.encodeToString(currentList))
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

    suspend fun saveDiaryNoteWithReturn(id: Int, title: String, content: String, tag: String, imagesJson: String, blocksJson: String, isFav: Boolean, isPriv: Boolean, isPinned: Boolean, isChecklist: Boolean): Long { return repository.insertDiaryNoteAndGetId(DiaryNote(id = id, title = title, content = content, tag = tag, imagesJson = imagesJson, blocksJson = blocksJson, isFavorite = isFav, isPrivate = isPriv, isPinned = isPinned, isChecklistMode = isChecklist)) }
    fun deleteDiaryNote(note: DiaryNote) = viewModelScope.launch { repository.deleteDiaryNote(note) }
    fun addNoteTag(name: String, colorHex: String) = viewModelScope.launch { repository.insertNoteTag(name, colorHex) }
    fun deleteNoteTag(tag: NoteTag) = viewModelScope.launch { repository.deleteNoteTag(tag) }

    private fun getMealEmojiForTime(timestamp: Long): String {
        val hour = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }.get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..9 -> "🍳"   // Breakfast
            in 10..12 -> "🍎"  // Morning Snack
            in 13..15 -> "🍛"  // Lunch
            in 16..19 -> "☕"   // Asraneh (عصرانه)
            in 20..23 -> "🍲"  // Dinner
            else -> "🌙"       // Midnight Snack
        }
    }

    fun logManualMeal(cals: Int, pro: Int, carbs: Int, fats: Int) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        val currentTime = System.currentTimeMillis()

        val log = ConsumedFoodLog(
            id = java.util.UUID.randomUUID().toString(),
            foodName = "ثبت دستی", // "Manual Entry"
            emoji = "✍️", // A distinct icon so the user knows it was custom macros
            servings = 1f,
            calories = cals, protein = pro, carbs = carbs, fats = fats,
            timestamp = currentTime
        )

        val currentLogs = try { Json.decodeFromString<List<ConsumedFoodLog>>(record.consumedFoodsJson) } catch (e: Exception) { emptyList() }
        repository.logConsumedFood(cals, pro, carbs, fats, Json.encodeToString(currentLogs + log))
    }

    fun removeConsumedFoodLog(log: ConsumedFoodLog) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        val currentLogs = try { Json.decodeFromString<List<ConsumedFoodLog>>(record.consumedFoodsJson) } catch (e: Exception) { emptyList() }

        val updatedLogs = currentLogs.filter { it.id != log.id }
        repository.logConsumedFood(-log.calories, -log.protein, -log.carbs, -log.fats, Json.encodeToString(updatedLogs))
    }


}