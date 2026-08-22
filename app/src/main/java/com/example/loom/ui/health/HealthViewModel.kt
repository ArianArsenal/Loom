package com.example.loom.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.dao.SandboxDao
import com.example.loom.data.local.entity.*
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.data.repository.TrackerRepository
import com.example.loom.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview // --- FIX ---
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // --- FIX ---
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.Calendar // --- FIX ---
import java.util.UUID // --- FIX ---
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds // --- FIX ---

@Serializable
data class SupplementItem(
    val id: String,
    val name: String,
    val dosage: String,
    val category: String = "Supplement",
    val timeOfUse: String = "Anytime",
    val recurrenceType: String = "DAILY",
    val recurrenceValue: String = "",
    val instruction: String = "None"
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repository: TrackerRepository,
    private val userPreferences: UserPreferences,
    private val sandboxDao: SandboxDao
) : ViewModel() {

    val hiddenHealthCards: StateFlow<Set<String>> = userPreferences.hiddenHealthCardsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val isFasting: StateFlow<Boolean> = userPreferences.isFastingFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val fastingStartTime: StateFlow<Long> = userPreferences.fastingStartTimeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val fastingGoalHours: StateFlow<Int> = userPreferences.fastingGoalHoursFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16)

    val allSpaces: StateFlow<List<com.example.loom.data.models.SandboxSpace>> = sandboxDao.getActiveSpaces()
        .map { entities -> entities.map { it.toDomainModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedHealthCards: StateFlow<List<Pair<com.example.loom.data.models.SandboxSpace, com.example.loom.data.models.SandboxBlock>>> = allSpaces.map { spaces ->
        val cards = mutableListOf<Pair<com.example.loom.data.models.SandboxSpace, com.example.loom.data.models.SandboxBlock>>()
        spaces.filter { !it.isDeleted }.forEach { space ->
            val foundCards = com.example.loom.ui.sandbox.findPinnedCards(space.blocks, "Health")
            foundCards.forEach { card -> cards.add(Pair(space, card)) }
        }
        cards
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSpaceFromWidget(space: com.example.loom.data.models.SandboxSpace) {
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) { // --- FIX ---
            withContext(NonCancellable) { // --- FIX: Proper structured concurrency ---
                try {
                    val spaceToSave = space.copy(updatedAt = System.currentTimeMillis())
                    val spaceEntity = NotionSpaceEntity.fromDomainModel(spaceToSave)
                    val blockEntities = com.example.loom.ui.sandbox.flattenTree(spaceToSave.blocks, spaceToSave.id)
                    sandboxDao.saveSpaceWithBlocks(spaceEntity, blockEntities)
                } catch (e: Exception) {
                    android.util.Log.e("LoomDatabase", "Fatal error updating space from Health widget", e)
                }
            }
        }
    }

    val todayRecord: StateFlow<DailyRecord?> = repository.getTodayRecord().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val todayExercises: StateFlow<List<Exercise>> = repository.getExercisesForDate(DateUtils.getTodayDateId()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPresets: StateFlow<List<WorkoutPreset>> = repository.getAllPresets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val exerciseTemplates: StateFlow<List<ExerciseTemplate>> = repository.getAllTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSkincareRoutines: StateFlow<List<SkincareRoutine>> = repository.getAllSkincareRoutines().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSkincareProducts: StateFlow<List<SkincareProduct>> = repository.getAllSkincareProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultWaterIncrement: StateFlow<Int> = userPreferences.defaultWaterIncrementFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 250)
    val targetCalories: StateFlow<Int> = userPreferences.targetCaloriesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2800)
    val targetProtein: StateFlow<Int> = userPreferences.targetProteinFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 140)
    val targetCarbs: StateFlow<Int> = userPreferences.targetCarbsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 350)
    val targetFats: StateFlow<Int> = userPreferences.targetFatsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 75)
    val weight: StateFlow<Float> = userPreferences.weightFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70f)
    val height: StateFlow<Int> = userPreferences.heightFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 175)
    val age: StateFlow<Int> = userPreferences.ageFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 21)
    val gender: StateFlow<String> = userPreferences.genderFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Male")
    val activity: StateFlow<String> = userPreferences.activityFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Moderate")
    val goal: StateFlow<String> = userPreferences.goalFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Maintain")
    val waterDailyGoal: StateFlow<Int> = userPreferences.waterDailyGoalFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3000)
    val isAutoWaterGoal: StateFlow<Boolean> = userPreferences.autoWaterGoalFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val bodyFat: StateFlow<Float> = userPreferences.bodyFatFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
    val dietType: StateFlow<String> = userPreferences.dietTypeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Balanced")
    val phaseStartTime: StateFlow<Long> = userPreferences.phaseStartTimeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())
    val phaseWeeks: StateFlow<Int> = userPreferences.phaseWeeksFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 12)
    val calorieCycling: StateFlow<Boolean> = userPreferences.calorieCyclingFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val supplementCategories: StateFlow<List<String>> = userPreferences.supplementCategoriesFlow
        .map { try { Json.decodeFromString<List<String>>(it) } catch (_: Exception) { listOf("Supplement", "Medication", "Pre-workout") } } // --- FIX: Unused 'e' ---
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Supplement", "Medication", "Pre-workout"))

    val supplementsRoster: StateFlow<List<SupplementItem>> = userPreferences.supplementsRosterFlow
        .map { try { Json.decodeFromString<List<SupplementItem>>(it) } catch (_: Exception) { emptyList() } } // --- FIX: Unused 'e' ---
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedSupplements: StateFlow<List<String>> = todayRecord
        .map { try { Json.decodeFromString<List<String>>(it?.completedSupplementsJson ?: "[]") } catch (_: Exception) { emptyList() } } // --- FIX: Unused 'e' ---
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayScheduledSupplements: StateFlow<List<SupplementItem>> = supplementsRoster.map { roster ->
        val today = LocalDate.now()
        roster.filter { item ->
            when (item.recurrenceType) {
                "DAILY" -> true
                "SPECIFIC_DAYS" -> {
                    val dayStr = today.dayOfWeek.name.take(3)
                    item.recurrenceValue.contains(dayStr, ignoreCase = true)
                }
                "INTERVAL" -> {
                    val interval = item.recurrenceValue.toIntOrNull() ?: 1
                    (today.toEpochDay() % interval) == 0L
                }
                else -> true
            }
        }.sortedBy { it.timeOfUse }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun saveCategory(categoryName: String) = viewModelScope.launch {
        val current = supplementCategories.value.toMutableList()
        if (!current.contains(categoryName)) {
            current.add(categoryName)
            userPreferences.saveSupplementCategories(Json.encodeToString(current))
        }
    }

    fun deleteCategory(categoryName: String) = viewModelScope.launch {
        val current = supplementCategories.value.toMutableList()
        current.remove(categoryName)
        userPreferences.saveSupplementCategories(Json.encodeToString(current))
    }

    fun toggleSupplement(id: String) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        val currentList = try { Json.decodeFromString<List<String>>(record.completedSupplementsJson) } catch (_: Exception) { emptyList() }.toMutableList() // --- FIX: Unused 'e' ---
        if (currentList.contains(id)) currentList.remove(id) else currentList.add(id)
        repository.updateDailyRecord(record.copy(completedSupplementsJson = Json.encodeToString(currentList)))
    }

    fun saveSupplementItem(item: SupplementItem) = viewModelScope.launch {
        val current = supplementsRoster.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) current[index] = item else current.add(item)
        userPreferences.saveSupplementsRoster(Json.encodeToString(current))
    }

    fun deleteSupplementItem(id: String) = viewModelScope.launch {
        val current = supplementsRoster.value.filter { it.id != id }
        userPreferences.saveSupplementsRoster(Json.encodeToString(current))
        val record = todayRecord.value ?: return@launch
        val currentCompleted = try { Json.decodeFromString<List<String>>(record.completedSupplementsJson) } catch (_: Exception) { emptyList() }.toMutableList() // --- FIX: Unused 'e' ---
        if (currentCompleted.contains(id)) {
            currentCompleted.remove(id)
            repository.updateDailyRecord(record.copy(completedSupplementsJson = Json.encodeToString(currentCompleted)))
        }
    }

    fun toggleCardVisibility(cardName: String, isVisible: Boolean) = viewModelScope.launch { userPreferences.toggleHealthCardVisibility(cardName, isVisible) }
    fun deleteDailyWeight() = viewModelScope.launch { val record = todayRecord.value ?: return@launch; repository.updateDailyRecord(record.copy(weightLogKg = 0f)) }

    val customWaterContainers: StateFlow<List<WaterContainer>> = userPreferences.customWaterContainersFlow.map { try { Json.decodeFromString<List<WaterContainer>>(it) } catch (_: Exception) { emptyList() } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) // --- FIX: Unused 'e' ---
    val waterLogs: StateFlow<List<WaterLog>> = todayRecord.map { try { Json.decodeFromString<List<WaterLog>>(it?.waterLogsJson ?: "[]") } catch (_: Exception) { emptyList() } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) // --- FIX: Unused 'e' ---

    fun saveWaterGoal(goal: Int, isAuto: Boolean) = viewModelScope.launch { userPreferences.saveWaterDailyGoal(goal, isAuto) }
    fun saveCustomWaterContainers(containers: List<WaterContainer>) = viewModelScope.launch { userPreferences.saveCustomWaterContainers(Json.encodeToString(containers)) }
    fun logDetailedWater(container: WaterContainer) = viewModelScope.launch { val newLog = WaterLog(id = UUID.randomUUID().toString(), containerName = container.name, emoji = container.iconEmoji, amountMl = container.volumeMl, hydrationFactor = container.hydrationFactor, timestamp = System.currentTimeMillis()); repository.logDetailedWater(Json.encodeToString(waterLogs.value + newLog)) } // --- FIX ---
    fun removeDetailedWaterLog(logId: String) = viewModelScope.launch { repository.logDetailedWater(Json.encodeToString(waterLogs.value.filter { it.id != logId })) }

    val consumedFoods: StateFlow<List<ConsumedFoodLog>> = todayRecord.map { try { Json.decodeFromString<List<ConsumedFoodLog>>(it?.consumedFoodsJson ?: "[]") } catch (_: Exception) { emptyList() } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) // --- FIX: Unused 'e' ---
    val completedSkincareRoutines: StateFlow<List<String>> = todayRecord.map { try { Json.decodeFromString<List<String>>(it?.completedSkincareRoutinesJson ?: "[]") } catch (_: Exception) { emptyList() } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) // --- FIX: Unused 'e' ---
    val completedSkincareSteps: StateFlow<List<String>> = todayRecord.map { try { Json.decodeFromString<List<String>>(it?.completedSkincareStepsJson ?: "[]") } catch (_: Exception) { emptyList() } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) // --- FIX: Unused 'e' ---

    private val _foodSearchQuery = MutableStateFlow("")
    val foodSearchQuery: StateFlow<String> = _foodSearchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class) // --- FIX: Explicitly Opt-in to Preview Flow features ---
    val searchedFoodItems: StateFlow<List<FoodItem>> = _foodSearchQuery.debounce(300.milliseconds).flatMapLatest { query -> if (query.isBlank()) repository.searchFoodItems("مرغ") else repository.searchFoodItems(query) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) // --- FIX: Duration mapping ---

    fun updateFoodSearchQuery(query: String) { _foodSearchQuery.value = query }

    init { viewModelScope.launch { repository.prepopulateSkincareProductsIfEmpty() } }

    fun saveBioMetrics(weight: Float, height: Int, age: Int, gender: String, activity: String, goal: String, bodyFat: Float, dietType: String, phaseWeeks: Int, isCycling: Boolean) = viewModelScope.launch { userPreferences.saveBioMetrics(weight, height, age, gender, activity, goal, bodyFat, dietType, phaseWeeks, isCycling) }
    fun logDailyWeight(weightKg: Float) = viewModelScope.launch { val record = todayRecord.value ?: return@launch; repository.updateDailyRecord(record.copy(weightLogKg = weightKg)) }
    fun saveDefaultWaterIncrement(ml: Int) = viewModelScope.launch { userPreferences.saveDefaultWaterIncrement(ml) }
    fun addFoodItem(food: FoodItem) = viewModelScope.launch { repository.insertFoodItem(food) }
    fun deleteFoodItem(food: FoodItem) = viewModelScope.launch { repository.deleteFoodItem(food) }
    fun logFoodItem(food: FoodItem, servings: Float) = viewModelScope.launch { val record = todayRecord.value ?: return@launch; val totalCals = (food.calories * servings).toInt(); val totalPro = (food.protein * servings).toInt(); val totalCarbs = (food.carbs * servings).toInt(); val totalFats = (food.fats * servings).toInt(); val currentTime = System.currentTimeMillis(); val log = ConsumedFoodLog(id = UUID.randomUUID().toString(), foodName = food.name, emoji = getMealEmojiForTime(currentTime), servings = servings, servingUnit = food.servingUnit, calories = totalCals, protein = totalPro, carbs = totalCarbs, fats = totalFats, timestamp = currentTime); val currentLogs = try { Json.decodeFromString<List<ConsumedFoodLog>>(record.consumedFoodsJson) } catch (_: Exception) { emptyList() }; repository.logConsumedFood(totalCals, totalPro, totalCarbs, totalFats, Json.encodeToString(currentLogs + log)) } // --- FIX: Unused 'e' & UUID ---
    fun removeConsumedFoodLog(log: ConsumedFoodLog) = viewModelScope.launch { val record = todayRecord.value ?: return@launch; val currentLogs = try { Json.decodeFromString<List<ConsumedFoodLog>>(record.consumedFoodsJson) } catch (_: Exception) { emptyList() }; val updatedLogs = currentLogs.filter { it.id != log.id }; repository.logConsumedFood(-log.calories, -log.protein, -log.carbs, -log.fats, Json.encodeToString(updatedLogs)) } // --- FIX: Unused 'e' ---
    fun createExerciseTemplate(name: String) = viewModelScope.launch { repository.insertTemplateIfNotExists(name) }
    fun addExercise(name: String, sets: Int, reps: Int, weight: Float, weightListJson: String = "[]", isTimeBased: Boolean, duration: Int, presetName: String? = null) = viewModelScope.launch { repository.insertExercise(name, sets, reps, weight, weightListJson, isTimeBased, duration, presetName); repository.insertTemplateIfNotExists(name) }
    fun updateExercise(exercise: Exercise) = viewModelScope.launch { repository.updateExercise(exercise) }
    fun toggleExerciseCompletion(exercise: Exercise) = viewModelScope.launch { repository.updateExercise(exercise.copy(isCompleted = !exercise.isCompleted)) }
    fun deleteExercise(exercise: Exercise) = viewModelScope.launch { repository.deleteExercise(exercise) }
    fun savePreset(id: Int, name: String, exercises: List<PresetExercise>) = viewModelScope.launch { repository.insertPreset(WorkoutPreset(id = id, presetName = name, exercises = exercises)); exercises.forEach { repository.insertTemplateIfNotExists(it.name) } }
    fun deletePreset(preset: WorkoutPreset) = viewModelScope.launch { repository.deletePreset(preset) }
    fun logPresetExercises(presetName: String, exercises: List<PresetExercise>) = viewModelScope.launch { exercises.forEach { repository.insertExercise(it.name, it.sets, it.reps, it.weight, it.weightListJson, it.isTimeBased, it.durationInSeconds, presetName) } }
    fun updateTemplate(template: ExerciseTemplate) = viewModelScope.launch { repository.updateTemplate(template) }
    fun deleteTemplate(template: ExerciseTemplate) = viewModelScope.launch { repository.deleteTemplate(template) }
    fun saveSkincareRoutine(routine: SkincareRoutine) = viewModelScope.launch { repository.insertSkincareRoutine(routine) }
    fun deleteSkincareRoutine(routine: SkincareRoutine) = viewModelScope.launch { repository.deleteSkincareRoutine(routine) }
    fun addSkincareProduct(name: String) = viewModelScope.launch { repository.insertSkincareProduct(SkincareProduct(name = name)) }
    fun toggleSkincareStep(routineName: String, productName: String) = viewModelScope.launch { val record = todayRecord.value ?: return@launch; val stepId = "${routineName}_${productName}"; val currentList = try { Json.decodeFromString<List<String>>(record.completedSkincareStepsJson) } catch (_: Exception) { emptyList() }.toMutableList(); if (currentList.contains(stepId)) currentList.remove(stepId) else currentList.add(stepId); repository.updateDailyRecord(record.copy(completedSkincareStepsJson = Json.encodeToString(currentList))) } // --- FIX: Unused 'e' ---
    fun logSkincareRoutine(routineName: String) = viewModelScope.launch { val record = todayRecord.value ?: return@launch; val currentRoutines = try { Json.decodeFromString<List<String>>(record.completedSkincareRoutinesJson) } catch (_: Exception) { emptyList() }.toMutableList(); if (!currentRoutines.contains(routineName)) { currentRoutines.add(routineName); repository.updateCompletedSkincareRoutines(Json.encodeToString(currentRoutines)) } } // --- FIX: Unused 'e' ---
    fun removeLoggedSkincareRoutine(routineName: String, products: List<String>) = viewModelScope.launch { val record = todayRecord.value ?: return@launch; val currentRoutines = try { Json.decodeFromString<List<String>>(record.completedSkincareRoutinesJson) } catch (_: Exception) { emptyList() }.toMutableList(); val currentSteps = try { Json.decodeFromString<List<String>>(record.completedSkincareStepsJson) } catch (_: Exception) { emptyList() }.toMutableList(); currentRoutines.remove(routineName); products.forEach { p -> currentSteps.remove("${routineName}_$p") }; repository.updateCompletedSkincareRoutines(Json.encodeToString(currentRoutines)); repository.updateCompletedSkincareSteps(Json.encodeToString(currentSteps)) } // --- FIX: Unused 'e' ---
    fun toggleAllSkincareSteps(routineName: String, products: List<String>, isCompleted: Boolean) = viewModelScope.launch { val record = todayRecord.value ?: return@launch; val currentSteps = try { Json.decodeFromString<List<String>>(record.completedSkincareStepsJson) } catch (_: Exception) { emptyList() }.toMutableList(); products.forEach { p -> val stepId = "${routineName}_$p"; if (isCompleted) { if (!currentSteps.contains(stepId)) currentSteps.add(stepId) } else { currentSteps.remove(stepId) } }; repository.updateCompletedSkincareSteps(Json.encodeToString(currentSteps)) } // --- FIX: Unused 'e' ---
    private fun getMealEmojiForTime(timestamp: Long): String { val hour = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY); return when (hour) { in 5..9 -> "🍳"; in 10..12 -> "🍎"; in 13..15 -> "🍛"; in 16..19 -> "☕"; in 20..23 -> "🍲"; else -> "🌙" } } // --- FIX ---
    fun toggleFasting(start: Boolean, goalHours: Int) = viewModelScope.launch { if (start) { userPreferences.setFastingState(true, System.currentTimeMillis(), goalHours) } else { userPreferences.setFastingState(false, 0L, goalHours) } }
    fun logSleep(hours: Float, rating: Int) = viewModelScope.launch { val record = todayRecord.value ?: return@launch; repository.updateDailyRecord(record.copy(sleepHours = hours, sleepRating = rating)) }
    fun logMeasurements(measurements: Map<String, Float>) = viewModelScope.launch { val record = todayRecord.value ?: return@launch; repository.updateDailyRecord(record.copy(measurementsJson = Json.encodeToString(measurements))) } // --- FIX ---
}