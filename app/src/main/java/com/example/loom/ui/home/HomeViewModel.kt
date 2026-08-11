package com.example.loom.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.dao.SandboxDao
import com.example.loom.data.local.dao.TrackerDao
import com.example.loom.data.local.entity.*
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.data.repository.TrackerRepository
import com.example.loom.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// --- THE FIX: Universal Backup Adapter (Supports Old & New Backups) ---
data class SpaceBackupDto(
    val id: String,
    val title: String,
    val icon: String,
    val bannerUrl: String = "",
    val updatedAt: Long = 0L,
    val isPinned: Boolean = false,
    val tagsJson: String = "[]",
    val isPrivate: Boolean = false,
    val isDeleted: Boolean = false,
    val blocksJson: String? = null, // CATCHES OLD BACKUPS
    val blocks: List<com.example.loom.data.models.SandboxBlock>? = null // CATCHES NEW BACKUPS
) {
    fun toDomainModel(): com.example.loom.data.models.SandboxSpace {
        // Smart fallback: If 'blocks' exists use it, otherwise decode the old 'blocksJson'
        val finalBlocks = if (blocks != null && blocks.isNotEmpty()) {
            blocks
        } else if (!blocksJson.isNullOrBlank()) {
            try { Json.decodeFromString<List<com.example.loom.data.models.SandboxBlock>>(blocksJson) } catch (e: Exception) { emptyList() }
        } else {
            emptyList()
        }

        return com.example.loom.data.models.SandboxSpace(
            id = id, title = title, icon = icon, bannerUrl = bannerUrl,
            updatedAt = updatedAt, isPinned = isPinned,
            tags = try { Json.decodeFromString(tagsJson) } catch (e: Exception) { emptyList() },
            isPrivate = isPrivate, isDeleted = isDeleted, blocks = finalBlocks
        )
    }
}

data class LoomBackup(
    val dailyRecords: List<DailyRecord> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val taskSections: List<TaskSection> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val workoutPresets: List<WorkoutPreset> = emptyList(),
    val exerciseTemplates: List<ExerciseTemplate> = emptyList(),
    val diaryNotes: List<DiaryNote> = emptyList(),
    val noteTags: List<NoteTag> = emptyList(),
    val skincareProducts: List<SkincareProduct> = emptyList(),
    val skincareRoutines: List<SkincareRoutine> = emptyList(),
    val foodItems: List<FoodItem> = emptyList(),
    val notionSpaces: List<SpaceBackupDto> = emptyList(), // Uses the new Bridge Adapter

    val targetCalories: Int = 2800,
    val targetProtein: Int = 140,
    val targetCarbs: Int = 350,
    val targetFats: Int = 75,
    val defaultWaterIncrement: Int = 250,
    val appTheme: String = "System",
    val isBiometricEnabled: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TrackerRepository,
    private val userPreferences: UserPreferences,
    private val trackerDao: TrackerDao,
    private val sandboxDao: SandboxDao
) : ViewModel() {

    val isBiometricEnabled: StateFlow<Boolean> = userPreferences.biometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setBiometricEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferences.setBiometricEnabled(enabled)
    }

    val appTheme: StateFlow<String> = userPreferences.appThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")

    fun setAppTheme(theme: String) = viewModelScope.launch {
        userPreferences.saveAppTheme(theme)
    }

    val todayRecord: StateFlow<DailyRecord?> = repository.getTodayRecord().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val consumedFoods: StateFlow<List<ConsumedFoodLog>> = todayRecord
        .map { try { Json.decodeFromString<List<ConsumedFoodLog>>(it?.consumedFoodsJson ?: "[]") } catch (e: Exception) { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- UPDATED: Pinned Home Cards Pipeline ---
    val allSpaces: StateFlow<List<com.example.loom.data.models.SandboxSpace>> = sandboxDao.getActiveSpaces()
        .onEach { android.util.Log.e("LoomSync", "PULSE CHECK: Database just emitted new data! Total Spaces: ${it.size}") }
        .map { entities -> entities.map { it.toDomainModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedHomeCards: StateFlow<List<Pair<com.example.loom.data.models.SandboxSpace, com.example.loom.data.models.SandboxBlock>>> = allSpaces.map { spaces ->
        val cards = mutableListOf<Pair<com.example.loom.data.models.SandboxSpace, com.example.loom.data.models.SandboxBlock>>()
        // Filter strictly for non-deleted spaces
        spaces.filter { !it.isDeleted }.forEach { space ->
            val foundCards = com.example.loom.ui.sandbox.findPinnedCards(space.blocks, "Home")
            foundCards.forEach { card -> cards.add(Pair(space, card)) }
        }
        cards
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSpaceFromWidget(space: com.example.loom.data.models.SandboxSpace) {
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        // THE FIX: NonCancellable ensures that typing in the widget saves even if you switch tabs 0.1 seconds later!
        GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
            try {
                val spaceToSave = space.copy(updatedAt = System.currentTimeMillis())
                val spaceEntity = NotionSpaceEntity.fromDomainModel(spaceToSave)
                val blockEntities = com.example.loom.ui.sandbox.flattenTree(spaceToSave.blocks, spaceToSave.id)
                sandboxDao.saveSpaceWithBlocks(spaceEntity, blockEntities)
            } catch (e: Exception) {
                android.util.Log.e("LoomDatabase", "Fatal error updating space from widget", e)
            }
        }
    }

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

    val pinnedSkincareRoutines: StateFlow<List<SkincareRoutine>> = repository.getPinnedSkincareRoutines().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _diaryViewMode = MutableStateFlow(1)
    val diaryViewMode: StateFlow<Int> = _diaryViewMode.asStateFlow()
    fun setDiaryViewMode(mode: Int) { _diaryViewMode.value = mode }

    private val _foodSearchQuery = MutableStateFlow("")
    val foodSearchQuery: StateFlow<String> = _foodSearchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val allSkincareProducts: StateFlow<List<SkincareProduct>> = repository.getAllSkincareProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterDailyGoal: StateFlow<Int> = userPreferences.waterDailyGoalFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3000)

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchedFoodItems: StateFlow<List<FoodItem>> = _foodSearchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.searchFoodItems("مرغ")
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

    fun exportDatabase(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Stitch the SQL rows into nested Domain Models for JSON Export
                val activeSpaces = sandboxDao.getActiveSpaces().first().map { it.toDomainModel() }

                // Map to DTO for safe exporting
                val backupSpaces = activeSpaces.map { space ->
                    SpaceBackupDto(
                        id = space.id, title = space.title, icon = space.icon, bannerUrl = space.bannerUrl,
                        updatedAt = space.updatedAt, isPinned = space.isPinned,
                        tagsJson = Json.encodeToString(space.tags),
                        isPrivate = space.isPrivate, isDeleted = space.isDeleted,
                        blocks = space.blocks
                    )
                }

                val backup = LoomBackup(
                    dailyRecords = trackerDao.getAllDailyRecords().first(),
                    tasks = trackerDao.getAllTasks().first(),
                    taskSections = trackerDao.getAllTaskSections().first(),
                    exercises = trackerDao.getAllExercises().first(),
                    workoutPresets = trackerDao.getAllPresets().first(),
                    exerciseTemplates = trackerDao.getAllTemplates().first(),
                    diaryNotes = trackerDao.getAllDiaryNotes().first(),
                    noteTags = trackerDao.getAllNoteTags().first(),
                    skincareProducts = trackerDao.getAllSkincareProducts().first(),
                    skincareRoutines = trackerDao.getAllSkincareRoutines().first(),
                    foodItems = trackerDao.getUserCreatedFoodItems().first(),
                    notionSpaces = backupSpaces, // Uses the mapped list here
                    targetCalories = targetCalories.value,
                    targetProtein = targetProtein.value,
                    targetCarbs = targetCarbs.value,
                    targetFats = targetFats.value,
                    defaultWaterIncrement = defaultWaterIncrement.value,
                    appTheme = appTheme.value,
                    isBiometricEnabled = isBiometricEnabled.value
                )

                val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                val jsonStr = gson.toJson(backup)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonStr.toByteArray())
                }

                launch(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                launch(Dispatchers.Main) { onError(e.message ?: "Unknown Error") }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importDatabase(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonStr = inputStream.bufferedReader().use { it.readText() }
                    val backup = com.google.gson.Gson().fromJson(jsonStr, LoomBackup::class.java)

                    backup.dailyRecords.forEach { trackerDao.insertDailyRecordReplace(it) }
                    backup.tasks.forEach { trackerDao.insertTask(it) }
                    backup.taskSections.forEach { trackerDao.insertTaskSection(it) }
                    backup.exercises.forEach { trackerDao.insertExercise(it) }
                    backup.workoutPresets.forEach { trackerDao.insertPreset(it) }
                    backup.exerciseTemplates.forEach { trackerDao.insertTemplate(it) }
                    backup.diaryNotes.forEach { trackerDao.insertDiaryNote(it) }
                    backup.noteTags.forEach { trackerDao.insertNoteTagReplace(it) }
                    backup.skincareProducts.forEach { trackerDao.insertSkincareProductReplace(it) }
                    backup.skincareRoutines.forEach { trackerDao.insertSkincareRoutine(it) }
                    backup.foodItems.forEach { trackerDao.insertFoodItem(it) }

                    // Maps the bridge adapter into Domain models, then safely slices them into SQL
                    backup.notionSpaces.forEach { dto ->
                        val domainSpace = dto.toDomainModel()
                        val spaceEntity = NotionSpaceEntity.fromDomainModel(domainSpace)
                        val blockEntities = com.example.loom.ui.sandbox.flattenTree(domainSpace.blocks, domainSpace.id)
                        sandboxDao.saveSpaceWithBlocks(spaceEntity, blockEntities)
                    }

                    userPreferences.saveTargets(backup.targetCalories, backup.targetProtein, backup.targetCarbs, backup.targetFats)
                    userPreferences.saveDefaultWaterIncrement(backup.defaultWaterIncrement)
                    userPreferences.saveAppTheme(backup.appTheme)
                    userPreferences.setBiometricEnabled(backup.isBiometricEnabled)

                    launch(Dispatchers.Main) { onSuccess() }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) { onError(e.message ?: "Invalid Backup File") }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun wipeAllData(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                trackerDao.wipeDailyRecords()
                trackerDao.wipeTasks()
                trackerDao.wipeTaskSections()
                trackerDao.wipeExercises()
                trackerDao.wipeWorkoutPresets()
                trackerDao.wipeExerciseTemplates()
                trackerDao.wipeDiaryNotes()
                trackerDao.wipeNoteTags()
                trackerDao.wipeSkincareProducts()
                trackerDao.wipeSkincareRoutines()
                trackerDao.wipeUserCreatedFoodItems()
                sandboxDao.wipeAllSpaces()

                userPreferences.saveTargets(2800, 140, 350, 75)
                userPreferences.saveDefaultWaterIncrement(250)
                userPreferences.saveAppTheme("System")
                userPreferences.setBiometricEnabled(true)

                repository.initializeTodayRecord()
                repository.prepopulateTemplatesIfEmpty()
                repository.prepopulateTagsIfEmpty()
                repository.prepopulateTaskSectionsIfEmpty()

                launch(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                launch(Dispatchers.Main) { onError(e.message ?: "Failed to wipe data") }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addWater() = viewModelScope.launch { repository.addWater(defaultWaterIncrement.value) }
    fun removeWater() = viewModelScope.launch { repository.removeWater(defaultWaterIncrement.value) }

    fun updateFoodSearchQuery(query: String) {
        _foodSearchQuery.value = query
    }

    fun addFoodItemToPantry(food: FoodItem) = viewModelScope.launch {
        repository.insertFoodItem(food.copy(isUserCreated = true))
    }

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
            emoji = getMealEmojiForTime(currentTime),
            servings = servings,
            servingUnit = food.servingUnit,
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

    fun deleteSkincareProduct(product: SkincareProduct) = viewModelScope.launch {
        repository.deleteSkincareProduct(product)
    }

    fun toggleAllSkincareSteps(routineName: String, products: List<String>, isCompleted: Boolean) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        val currentSteps = try { Json.decodeFromString<List<String>>(record.completedSkincareStepsJson) } catch (e: Exception) { emptyList() }.toMutableList()

        products.forEach { p ->
            val stepId = "${routineName}_$p"
            if (isCompleted) {
                if (!currentSteps.contains(stepId)) currentSteps.add(stepId)
            } else {
                currentSteps.remove(stepId)
            }
        }
        repository.updateCompletedSkincareSteps(Json.encodeToString(currentSteps))
    }

    fun saveSkincareRoutine(routine: SkincareRoutine) = viewModelScope.launch { repository.insertSkincareRoutine(routine) }

    fun updateSkincareProduct(product: SkincareProduct) = viewModelScope.launch { repository.updateSkincareProduct(product) }

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
        } catch (e: Exception) { }
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
            in 5..9 -> "🍳"
            in 10..12 -> "🍎"
            in 13..15 -> "🍛"
            in 16..19 -> "☕"
            in 20..23 -> "🍲"
            else -> "🌙"
        }
    }

    fun logManualMeal(cals: Int, pro: Int, carbs: Int, fats: Int) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        val currentTime = System.currentTimeMillis()

        val log = ConsumedFoodLog(
            id = java.util.UUID.randomUUID().toString(),
            foodName = "Manual Entry",
            emoji = "✍️",
            servings = 1f,
            servingUnit = "serving",
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