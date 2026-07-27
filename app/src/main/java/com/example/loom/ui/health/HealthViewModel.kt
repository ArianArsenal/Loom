package com.example.loom.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repository: TrackerRepository,
    private val userPreferences: UserPreferences // <-- INJECTED
) : ViewModel() {

    val todayRecord: StateFlow<DailyRecord?> = repository.getTodayRecord().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val todayExercises: StateFlow<List<Exercise>> = repository.getExercisesForDate(DateUtils.getTodayDateId()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPresets: StateFlow<List<WorkoutPreset>> = repository.getAllPresets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val exerciseTemplates: StateFlow<List<ExerciseTemplate>> = repository.getAllTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSkincareRoutines: StateFlow<List<SkincareRoutine>> = repository.getAllSkincareRoutines().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSkincareProducts: StateFlow<List<SkincareProduct>> = repository.getAllSkincareProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- NEW: BIO & FOOD STATES ---
    val allFoodItems: StateFlow<List<FoodItem>> = repository.getAllFoodItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val defaultWaterIncrement: StateFlow<Int> = userPreferences.defaultWaterIncrementFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 250)

    val weight: StateFlow<Float> = userPreferences.weightFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70f)
    val height: StateFlow<Int> = userPreferences.heightFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 175)
    val age: StateFlow<Int> = userPreferences.ageFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 21) // Defaults to your age!
    val gender: StateFlow<String> = userPreferences.genderFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Male")
    val activity: StateFlow<String> = userPreferences.activityFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Moderate")
    val goal: StateFlow<String> = userPreferences.goalFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Maintain")

    init {
        viewModelScope.launch {
            repository.prepopulateSkincareProductsIfEmpty()
            repository.prepopulateFoodPantryIfEmpty()
        }
    }

    // --- BIO ENGINE ---
    fun saveBioMetrics(weight: Float, height: Int, age: Int, gender: String, activity: String, goal: String) = viewModelScope.launch {
        userPreferences.saveBioMetrics(weight, height, age, gender, activity, goal)
    }

    fun saveDefaultWaterIncrement(ml: Int) = viewModelScope.launch {
        userPreferences.saveDefaultWaterIncrement(ml)
    }

    // --- FOOD PANTRY ---
    fun addFoodItem(food: FoodItem) = viewModelScope.launch { repository.insertFoodItem(food) }
    fun deleteFoodItem(food: FoodItem) = viewModelScope.launch { repository.deleteFoodItem(food) }

    fun logFoodItem(food: FoodItem, servings: Float) = viewModelScope.launch {
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

    fun removeConsumedFoodLog(log: com.example.loom.data.local.entity.ConsumedFoodLog) = viewModelScope.launch {
        val record = todayRecord.value ?: return@launch
        val currentLogs = try { Json.decodeFromString<List<com.example.loom.data.local.entity.ConsumedFoodLog>>(record.consumedFoodsJson) } catch (e: Exception) { emptyList() }
        val updatedLogs = currentLogs.filter { it.id != log.id }

        // Pass negative values to subtract them from the daily total!
        repository.logConsumedFood(-log.calories, -log.protein, -log.carbs, -log.fats, Json.encodeToString(updatedLogs))
    }

    // --- GYM & SKINCARE ---
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

    fun saveSkincareRoutine(routine: SkincareRoutine) = viewModelScope.launch { repository.insertSkincareRoutine(routine) }
    fun deleteSkincareRoutine(routine: SkincareRoutine) = viewModelScope.launch { repository.deleteSkincareRoutine(routine) }
    fun addSkincareProduct(name: String) = viewModelScope.launch { repository.insertSkincareProduct(SkincareProduct(name)) }
    fun deleteSkincareProduct(product: SkincareProduct) = viewModelScope.launch { repository.deleteSkincareProduct(product) }

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
}