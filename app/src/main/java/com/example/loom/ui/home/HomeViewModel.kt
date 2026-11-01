package com.example.loom.ui.home

import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.dao.SandboxDao
import com.example.loom.data.local.dao.TrackerDao
import com.example.loom.data.local.entity.*
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import com.example.loom.data.repository.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@Keep
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
    val blocksJson: String? = null,
    val blocks: List<SandboxBlock>? = null
) {
    fun toDomainModel(): SandboxSpace {
        val finalBlocks = if (!blocks.isNullOrEmpty()) {
            blocks
        } else if (!blocksJson.isNullOrBlank()) {
            try { Json.decodeFromString<List<SandboxBlock>>(blocksJson) } catch (_: Exception) { emptyList() }
        } else {
            emptyList()
        }

        return SandboxSpace(
            id = id, title = title, icon = icon, bannerUrl = bannerUrl,
            updatedAt = updatedAt, isPinned = isPinned,
            tags = try { Json.decodeFromString(tagsJson) } catch (_: Exception) { emptyList() },
            isPrivate = isPrivate, isDeleted = isDeleted, blocks = finalBlocks
        )
    }
}

@Keep
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
    val notionSpaces: List<SpaceBackupDto> = emptyList(),
    val targetCalories: Int = 2800,
    val targetProtein: Int = 140,
    val targetCarbs: Int = 350,
    val targetFats: Int = 75,
    val defaultWaterIncrement: Int = 250,
    val appTheme: String = "System",
    val isBiometricEnabled: Boolean = true,
    val dayRolloverHour: Int = 2 // --- NEW: Added to Backup ---
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TrackerRepository,
    private val userPreferences: UserPreferences,
    private val trackerDao: TrackerDao,
    private val sandboxDao: SandboxDao
) : ViewModel() {

    // --- User Name State ---
    val userName: StateFlow<String> = userPreferences.userNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun saveUserName(name: String) = viewModelScope.launch {
        userPreferences.saveUserName(name)
    }

    // --- Dashboard State ---
    val hiddenHomeCards: StateFlow<Set<String>> = userPreferences.hiddenHomeCardsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val agendaText: StateFlow<String> = userPreferences.agendaTextFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun toggleHomeCardVisibility(cardName: String, isVisible: Boolean) = viewModelScope.launch {
        userPreferences.toggleHomeCardVisibility(cardName, isVisible)
    }

    fun updateAgendaText(text: String) = viewModelScope.launch {
        userPreferences.saveAgendaText(text)
    }

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

    // --- NEW: Night Owl Mode State ---
    val dayRolloverHour: StateFlow<Int> = userPreferences.dayRolloverHourFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    fun setDayRolloverHour(hour: Int) = viewModelScope.launch {
        userPreferences.saveDayRolloverHour(hour)
        com.example.loom.util.DateUtils.rolloverOffsetHours = hour
    }

    val todayRecord: StateFlow<DailyRecord?> = repository.getTodayRecord()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val consumedFoods: StateFlow<List<ConsumedFoodLog>> = todayRecord
        .map { try { Json.decodeFromString<List<ConsumedFoodLog>>(it?.consumedFoodsJson ?: "[]") } catch (_: Exception) { emptyList() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSpaces: StateFlow<List<SandboxSpace>> = sandboxDao.getActiveSpaces()
        .map { entities -> entities.map { it.toDomainModel() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedHomeCards: StateFlow<List<Pair<SandboxSpace, SandboxBlock>>> = allSpaces.map { spaces ->
        val cards = mutableListOf<Pair<SandboxSpace, SandboxBlock>>()
        spaces.filter { !it.isDeleted }.forEach { space ->
            val foundCards = com.example.loom.ui.sandbox.findPinnedCards(space.blocks, "Home")
            foundCards.forEach { card -> cards.add(Pair(space, card)) }
        }
        cards
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSpaceFromWidget(space: SandboxSpace) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            withContext(NonCancellable) {
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
    }

    val completedSkincareRoutines: StateFlow<List<String>> = todayRecord
        .map { try { Json.decodeFromString<List<String>>(it?.completedSkincareRoutinesJson ?: "[]") } catch (_: Exception) { emptyList() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedSkincareSteps: StateFlow<List<String>> = todayRecord
        .map { try { Json.decodeFromString<List<String>>(it?.completedSkincareStepsJson ?: "[]") } catch (_: Exception) { emptyList() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<Task>> = repository.getAllTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val waterDailyGoal: StateFlow<Int> = userPreferences.waterDailyGoalFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3000)

    private val _selectedFoodVariants = MutableStateFlow<List<FoodItem>>(emptyList())
    val selectedFoodVariants: StateFlow<List<FoodItem>> = _selectedFoodVariants.asStateFlow()

    fun fetchFoodVariants(foodName: String) = viewModelScope.launch(Dispatchers.IO) {
        _selectedFoodVariants.value = trackerDao.getFoodVariantsSync(foodName)
    }

    fun clearFoodVariants() {
        _selectedFoodVariants.value = emptyList()
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchedFoodItems: StateFlow<List<FoodItem>> = _foodSearchQuery
        .debounce(300.milliseconds)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                trackerDao.searchFoodItemsGrouped("مرغ")
            } else {
                trackerDao.searchFoodItemsGrouped(query)
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // --- NEW: Instantly sync the Rollover hour to DateUtils on launch ---
            userPreferences.dayRolloverHourFlow.collect { hour ->
                com.example.loom.util.DateUtils.rolloverOffsetHours = hour
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.initializeTodayRecord()
            repository.prepopulateTemplatesIfEmpty()
            repository.prepopulateTagsIfEmpty()
            repository.prepopulateTaskSectionsIfEmpty()
            repository.prepopulateSkincareProductsIfEmpty()
        }
    }

    fun exportDatabase(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val activeSpaces = sandboxDao.getActiveSpaces().first().map { it.toDomainModel() }
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
                    notionSpaces = backupSpaces,
                    targetCalories = targetCalories.value,
                    targetProtein = targetProtein.value,
                    targetCarbs = targetCarbs.value,
                    targetFats = targetFats.value,
                    defaultWaterIncrement = defaultWaterIncrement.value,
                    appTheme = appTheme.value,
                    isBiometricEnabled = isBiometricEnabled.value,
                    dayRolloverHour = dayRolloverHour.value // --- NEW: Added to JSON payload ---
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

                    // 1. WIPE SLATE CLEAN TO PREVENT DUPLICATES
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

                    // 2. BULK INSERT FROM BACKUP
                    trackerDao.insertDailyRecords(backup.dailyRecords)
                    trackerDao.insertTasks(backup.tasks)
                    trackerDao.insertTaskSections(backup.taskSections)
                    trackerDao.insertExercises(backup.exercises)
                    trackerDao.insertPresets(backup.workoutPresets)
                    trackerDao.insertTemplates(backup.exerciseTemplates)
                    trackerDao.insertDiaryNotes(backup.diaryNotes)
                    trackerDao.insertNoteTags(backup.noteTags)
                    trackerDao.insertSkincareProducts(backup.skincareProducts)
                    trackerDao.insertSkincareRoutines(backup.skincareRoutines)
                    trackerDao.insertFoodItems(backup.foodItems)

                    // 3. RESTORE SPACES
                    backup.notionSpaces.forEach { dto ->
                        val domainSpace = dto.toDomainModel()
                        val spaceEntity = NotionSpaceEntity.fromDomainModel(domainSpace)
                        val blockEntities = com.example.loom.ui.sandbox.flattenTree(domainSpace.blocks, domainSpace.id)
                        sandboxDao.saveSpaceWithBlocks(spaceEntity, blockEntities)
                    }

                    // 4. RESTORE PREFERENCES
                    userPreferences.saveTargets(backup.targetCalories, backup.targetProtein, backup.targetCarbs, backup.targetFats)
                    userPreferences.saveDefaultWaterIncrement(backup.defaultWaterIncrement)
                    userPreferences.saveAppTheme(backup.appTheme)
                    userPreferences.setBiometricEnabled(backup.isBiometricEnabled)
                    userPreferences.saveDayRolloverHour(backup.dayRolloverHour) // --- NEW: Restore Rollover Hour ---

                    // 5. FILL GAPS
                    repository.initializeTodayRecord()
                    repository.prepopulateTemplatesIfEmpty()
                    repository.prepopulateTagsIfEmpty()
                    repository.prepopulateTaskSectionsIfEmpty()
                    repository.prepopulateSkincareProductsIfEmpty()

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
                userPreferences.saveDayRolloverHour(2) // --- NEW: Reset to 2 AM default ---

                repository.initializeTodayRecord()
                repository.prepopulateTemplatesIfEmpty()
                repository.prepopulateTagsIfEmpty()
                repository.prepopulateTaskSectionsIfEmpty()
                repository.prepopulateSkincareProductsIfEmpty()

                launch(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                launch(Dispatchers.Main) { onError(e.message ?: "Failed to wipe data") }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addWater() = viewModelScope.launch(Dispatchers.IO) { repository.addWater(defaultWaterIncrement.value) }
    fun removeWater() = viewModelScope.launch(Dispatchers.IO) { repository.removeWater(defaultWaterIncrement.value) }

    fun updateFoodSearchQuery(query: String) {
        _foodSearchQuery.value = query
    }

    fun addFoodItemToPantry(food: FoodItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertFoodItem(food.copy(isUserCreated = true))
    }

    fun logFoodItem(food: FoodItem, servings: Float) = viewModelScope.launch(Dispatchers.IO) {
        val record = todayRecord.value ?: return@launch
        val totalCals = (food.calories * servings).toInt()
        val totalPro = (food.protein * servings).toInt()
        val totalCarbs = (food.carbs * servings).toInt()
        val totalFats = (food.fats * servings).toInt()
        val currentTime = System.currentTimeMillis()

        val log = ConsumedFoodLog(
            id = UUID.randomUUID().toString(),
            foodName = food.name,
            emoji = getMealEmojiForTime(currentTime),
            servings = servings,
            servingUnit = food.servingUnit,
            calories = totalCals, protein = totalPro, carbs = totalCarbs, fats = totalFats,
            timestamp = currentTime
        )

        val currentLogs = try { Json.decodeFromString<List<ConsumedFoodLog>>(record.consumedFoodsJson) } catch (_: Exception) { emptyList() }
        repository.logConsumedFood(totalCals, totalPro, totalCarbs, totalFats, Json.encodeToString(currentLogs + log))
    }

    fun toggleSkincareStep(routineName: String, productName: String) = viewModelScope.launch(Dispatchers.IO) {
        val record = todayRecord.value ?: return@launch
        val stepId = "${routineName}_${productName}"
        val currentList = try { Json.decodeFromString<List<String>>(record.completedSkincareStepsJson) } catch (_: Exception) { emptyList() }.toMutableList()
        if (currentList.contains(stepId)) currentList.remove(stepId) else currentList.add(stepId)
        repository.updateCompletedSkincareSteps(Json.encodeToString(currentList))
    }

    fun toggleAllSkincareSteps(routineName: String, products: List<String>, isCompleted: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        val record = todayRecord.value ?: return@launch
        val currentSteps = try { Json.decodeFromString<List<String>>(record.completedSkincareStepsJson) } catch (_: Exception) { emptyList() }.toMutableList()

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

    fun saveSkincareRoutine(routine: SkincareRoutine) = viewModelScope.launch(Dispatchers.IO) { repository.insertSkincareRoutine(routine) }

    fun addTask(task: Task) = viewModelScope.launch(Dispatchers.IO) { repository.insertTask(task) }
    fun updateTask(task: Task) = viewModelScope.launch(Dispatchers.IO) { repository.updateTask(task) }

    fun updateTaskStatus(task: Task, newStatus: String) = viewModelScope.launch(Dispatchers.IO) {
        val isNowDone = newStatus == "DONE"
        val wasDone = task.isCompleted

        val completionTime = if (isNowDone && !wasDone) {
            System.currentTimeMillis()
        } else if (isNowDone) {
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

    fun toggleSubtaskCompletion(task: Task, subtaskId: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val subtasks = Json.decodeFromString<List<SubTask>>(task.subtasksJson).toMutableList()
            val index = subtasks.indexOfFirst { it.id == subtaskId }
            if (index != -1) {
                subtasks[index] = subtasks[index].copy(isCompleted = !subtasks[index].isCompleted)
                repository.updateTask(task.copy(subtasksJson = Json.encodeToString(subtasks)))
            }
        } catch (_: Exception) { }
    }

    private suspend fun spawnNextRecurringTask(task: Task) {
        val parts = task.recurringInterval.split(" ")
        val amount = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val type = parts.getOrNull(1) ?: "DAYS"

        val currentDate = try { LocalDate.parse(task.targetDate) } catch (_: Exception) { LocalDate.now() }
        val nextDate = when (type) {
            "DAYS" -> currentDate.plusDays(amount.toLong())
            "WEEKS" -> currentDate.plusWeeks(amount.toLong())
            "MONTHS" -> currentDate.plusMonths(amount.toLong())
            else -> currentDate
        }
        val nextDateStr = nextDate.toString()

        val subtasks = try { Json.decodeFromString<List<SubTask>>(task.subtasksJson) } catch (_: Exception) { emptyList() }
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

    fun deleteTask(task: Task) = viewModelScope.launch(Dispatchers.IO) { repository.deleteTask(task) }
    fun addTaskSection(name: String, order: Int = 0) = viewModelScope.launch(Dispatchers.IO) { repository.insertTaskSection(TaskSection(name = name, orderIndex = order)) }
    fun deleteTaskSection(section: TaskSection) = viewModelScope.launch(Dispatchers.IO) { repository.deleteTaskSection(section) }

    suspend fun saveDiaryNoteWithReturn(id: Int, title: String, content: String, tag: String, imagesJson: String, blocksJson: String, isFav: Boolean, isPrivate: Boolean, isPinned: Boolean, isChecklist: Boolean): Long {
        return repository.insertDiaryNoteAndGetId(DiaryNote(id = id, title = title, content = content, tag = tag, imagesJson = imagesJson, blocksJson = blocksJson, isFavorite = isFav, isPrivate = isPrivate, isPinned = isPinned, isChecklistMode = isChecklist))
    }
    fun deleteDiaryNote(note: DiaryNote) = viewModelScope.launch(Dispatchers.IO) { repository.deleteDiaryNote(note) }
    fun addNoteTag(name: String, colorHex: String) = viewModelScope.launch(Dispatchers.IO) { repository.insertNoteTag(name, colorHex) }
    fun deleteNoteTag(tag: NoteTag) = viewModelScope.launch(Dispatchers.IO) { repository.deleteNoteTag(tag) }

    private fun getMealEmojiForTime(timestamp: Long): String {
        val hour = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).hour
        return when (hour) {
            in 5..9 -> "🍳"
            in 10..12 -> "🍎"
            in 13..15 -> "🍛"
            in 16..19 -> "☕"
            in 20..23 -> "🍲"
            else -> "🌙"
        }
    }

    fun logManualMeal(cals: Int, pro: Int, carbs: Int, fats: Int) = viewModelScope.launch(Dispatchers.IO) {
        val record = todayRecord.value ?: return@launch
        val currentTime = System.currentTimeMillis()

        val log = ConsumedFoodLog(
            id = UUID.randomUUID().toString(),
            foodName = "Manual Entry",
            emoji = "✍️",
            servings = 1f,
            servingUnit = "serving",
            calories = cals, protein = pro, carbs = carbs, fats = fats,
            timestamp = currentTime
        )

        val currentLogs = try { Json.decodeFromString<List<ConsumedFoodLog>>(record.consumedFoodsJson) } catch (_: Exception) { emptyList() }
        repository.logConsumedFood(cals, pro, carbs, fats, Json.encodeToString(currentLogs + log))
    }

    fun removeConsumedFoodLog(log: ConsumedFoodLog) = viewModelScope.launch(Dispatchers.IO) {
        val record = todayRecord.value ?: return@launch
        val currentLogs = try { Json.decodeFromString<List<ConsumedFoodLog>>(record.consumedFoodsJson) } catch (_: Exception) { emptyList() }

        val updatedLogs = currentLogs.filter { it.id != log.id }
        repository.logConsumedFood(-log.calories, -log.protein, -log.carbs, -log.fats, Json.encodeToString(updatedLogs))
    }

    private val _shortcutAction = MutableStateFlow<String?>(null)
    val shortcutAction: StateFlow<String?> = _shortcutAction.asStateFlow()

    fun setShortcutAction(action: String) {
        _shortcutAction.value = action
    }

    fun clearShortcutAction() {
        _shortcutAction.value = null
    }
}