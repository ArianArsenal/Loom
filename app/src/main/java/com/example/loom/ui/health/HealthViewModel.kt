package com.example.loom.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.entity.ConsumedFoodLog
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.ExerciseTemplate
import com.example.loom.data.local.entity.FoodItem
import com.example.loom.data.local.entity.PresetExercise
import com.example.loom.data.local.entity.SkincareProduct
import com.example.loom.data.local.entity.SkincareRoutine
import com.example.loom.data.local.entity.WorkoutPreset
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.data.repository.TrackerRepository
import com.example.loom.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repository: TrackerRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

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

    val customWaterContainers: StateFlow<List<com.example.loom.data.local.entity.WaterContainer>> = userPreferences.customWaterContainersFlow
        .map { try { Json.decodeFromString<List<com.example.loom.data.local.entity.WaterContainer>>(it) } catch (e: Exception) { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterLogs: StateFlow<List<com.example.loom.data.local.entity.WaterLog>> = todayRecord
        .map { try { Json.decodeFromString<List<com.example.loom.data.local.entity.WaterLog>>(it?.waterLogsJson ?: "[]") } catch (e: Exception) { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveWaterGoal(goal: Int, isAuto: Boolean) = viewModelScope.launch { userPreferences.saveWaterDailyGoal(goal, isAuto) }
    fun saveCustomWaterContainers(containers: List<com.example.loom.data.local.entity.WaterContainer>) = viewModelScope.launch {
        userPreferences.saveCustomWaterContainers(Json.encodeToString(containers))
    }

    fun logDetailedWater(container: com.example.loom.data.local.entity.WaterContainer) = viewModelScope.launch {
        val currentLogs = waterLogs.value
        val newLog = com.example.loom.data.local.entity.WaterLog(
            id = java.util.UUID.randomUUID().toString(),
            containerName = container.name, emoji = container.iconEmoji, amountMl = container.volumeMl, hydrationFactor = container.hydrationFactor, timestamp = System.currentTimeMillis()
        )
        repository.logDetailedWater(Json.encodeToString(currentLogs + newLog))
    }

    fun removeDetailedWaterLog(logId: String) = viewModelScope.launch {
        val currentLogs = waterLogs.value
        val updatedLogs = currentLogs.filter { it.id != logId }
        repository.logDetailedWater(Json.encodeToString(updatedLogs))
    }

    val consumedFoods: StateFlow<List<ConsumedFoodLog>> = todayRecord
        .map { try { Json.decodeFromString<List<ConsumedFoodLog>>(it?.consumedFoodsJson ?: "[]") } catch (e: Exception) { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedSkincareRoutines: StateFlow<List<String>> = todayRecord
        .map { try { Json.decodeFromString<List<String>>(it?.completedSkincareRoutinesJson ?: "[]") } catch (e: Exception) { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedSkincareSteps: StateFlow<List<String>> = todayRecord
        .map { try { Json.decodeFromString<List<String>>(it?.completedSkincareStepsJson ?: "[]") } catch (e: Exception) { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _foodSearchQuery = MutableStateFlow("")
    val foodSearchQuery: StateFlow<String> = _foodSearchQuery.asStateFlow()

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

    fun updateFoodSearchQuery(query: String) {
        _foodSearchQuery.value = query
    }

    init {
        viewModelScope.launch {
            repository.prepopulateSkincareProductsIfEmpty()
        }
    }

    fun saveBioMetrics(weight: Float, height: Int, age: Int, gender: String, activity: String, goal: String) = viewModelScope.launch {
        userPreferences.saveBioMetrics(weight, height, age, gender, activity, goal)
    }

    fun saveDefaultWaterIncrement(ml: Int) = viewModelScope.launch {
        userPreferences.saveDefaultWaterIncrement(ml)
    }

    fun addFoodItem(food: FoodItem) = viewModelScope.launch { repository.insertFoodItem(food) }
    fun deleteFoodItem(food: FoodItem) = viewModelScope.launch { repository.deleteFoodItem(food) }

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
            servingUnit = food.servingUnit, // Added Unit Save
            calories = totalCals, protein = totalPro, carbs = totalCarbs, fats = totalFats,
            timestamp = currentTime
        )

        val currentLogs = try { Json.decodeFromString<List<ConsumedFoodLog>>(record.consumedFoodsJson) } catch (e: Exception) { emptyList() }
        repository.logConsumedFood(totalCals, totalPro, totalCarbs, totalFats, Json.encodeToString(currentLogs + log))
    }

    fun removeConsumedFoodLog(log: ConsumedFoodLog) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        val currentLogs = try { Json.decodeFromString<List<ConsumedFoodLog>>(record.consumedFoodsJson) } catch (e: Exception) { emptyList() }

        val updatedLogs = currentLogs.filter { it.id != log.id }
        repository.logConsumedFood(-log.calories, -log.protein, -log.carbs, -log.fats, Json.encodeToString(updatedLogs))
    }

    fun addWater() = viewModelScope.launch { repository.addWater(defaultWaterIncrement.value) }
    fun removeWater() = viewModelScope.launch { repository.removeWater(defaultWaterIncrement.value) }

    fun createExerciseTemplate(name: String) = viewModelScope.launch { repository.insertTemplateIfNotExists(name) }

    fun addExercise(name: String, sets: Int, reps: Int, weight: Float, weightListJson: String = "[]", isTimeBased: Boolean, duration: Int, presetName: String? = null) = viewModelScope.launch {
        repository.insertExercise(name, sets, reps, weight, weightListJson, isTimeBased, duration, presetName)
        repository.insertTemplateIfNotExists(name)
    }

    fun updateExercise(exercise: Exercise) = viewModelScope.launch { repository.updateExercise(exercise) }

    fun toggleExerciseCompletion(exercise: Exercise) = viewModelScope.launch {
        val updatedExercise = exercise.copy(isCompleted = !exercise.isCompleted)
        repository.updateExercise(updatedExercise)
    }

    fun deleteExercise(exercise: Exercise) = viewModelScope.launch { repository.deleteExercise(exercise) }
    fun savePreset(id: Int, name: String, exercises: List<PresetExercise>) = viewModelScope.launch {
        repository.insertPreset(WorkoutPreset(id = id, presetName = name, exercises = exercises))
        exercises.forEach { repository.insertTemplateIfNotExists(it.name) }
    }
    fun deletePreset(preset: WorkoutPreset) = viewModelScope.launch { repository.deletePreset(preset) }
    fun logPresetExercises(presetName: String, exercises: List<PresetExercise>) = viewModelScope.launch {
        exercises.forEach { repository.insertExercise(it.name, it.sets, it.reps, it.weight, it.weightListJson, it.isTimeBased, it.durationInSeconds, presetName) }
    }

    fun updateTemplate(template: ExerciseTemplate) = viewModelScope.launch { repository.updateTemplate(template) }
    fun deleteTemplate(template: ExerciseTemplate) = viewModelScope.launch { repository.deleteTemplate(template) }

    fun saveSkincareRoutine(routine: SkincareRoutine) = viewModelScope.launch { repository.insertSkincareRoutine(routine) }
    fun deleteSkincareRoutine(routine: SkincareRoutine) = viewModelScope.launch { repository.deleteSkincareRoutine(routine) }
    fun addSkincareProduct(name: String) = viewModelScope.launch {
        repository.insertSkincareProduct(SkincareProduct(name = name))
    }
    fun deleteSkincareProduct(product: SkincareProduct) = viewModelScope.launch { repository.deleteSkincareProduct(product) }

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

    // 1. Logs the routine to today's list (so it appears on the screen)
    fun logSkincareRoutine(routineName: String) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        val currentRoutines = try { Json.decodeFromString<List<String>>(record.completedSkincareRoutinesJson) } catch (e: Exception) { emptyList() }.toMutableList()
        if (!currentRoutines.contains(routineName)) {
            currentRoutines.add(routineName)
            repository.updateCompletedSkincareRoutines(Json.encodeToString(currentRoutines))
        }
    }

    // 2. Swiping the card removes it from today's list AND clears its checkboxes
    fun removeLoggedSkincareRoutine(routineName: String, products: List<String>) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        val currentRoutines = try { Json.decodeFromString<List<String>>(record.completedSkincareRoutinesJson) } catch (e: Exception) { emptyList() }.toMutableList()
        val currentSteps = try { Json.decodeFromString<List<String>>(record.completedSkincareStepsJson) } catch (e: Exception) { emptyList() }.toMutableList()

        currentRoutines.remove(routineName)
        products.forEach { p ->
            val stepId = "${routineName}_$p"
            currentSteps.remove(stepId)
        }

        repository.updateCompletedSkincareRoutines(Json.encodeToString(currentRoutines))
        repository.updateCompletedSkincareSteps(Json.encodeToString(currentSteps))
    }

    // 3. The Master Checkbox toggles all items without making the card disappear
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

    fun updateSkincareProduct(product: SkincareProduct) = viewModelScope.launch { repository.updateSkincareProduct(product) }
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
}