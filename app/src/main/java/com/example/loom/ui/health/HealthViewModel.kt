package com.example.loom.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.dao.SandboxDao
import com.example.loom.data.local.entity.*
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.data.repository.TrackerRepository
import com.example.loom.data.repository.addJsonIds
import com.example.loom.data.repository.removeJsonIds
import com.example.loom.data.repository.toggleJsonId
import com.example.loom.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class SupplementItem(
    val id: String,
    val name: String,
    val dosage: String,
    val category: String = "Supplement",
    val timeOfUse: String = "Anytime",
    val recurrenceType: String = "DAILY",
    val recurrenceValue: String = "",
    val instruction: String = "None",
    val isActive: Boolean = false // --- NEW: To hide inactive templates ---
)

/**
 * Whether this supplement should be taken on [date]. Shared by Health, Calendar and reminders
 * so the three always agree (Calendar used to show inactive supplements).
 */
fun SupplementItem.isScheduledOn(date: LocalDate): Boolean {
    if (!isActive) return false
    return when (recurrenceType) {
        "DAILY" -> true
        "SPECIFIC_DAYS" -> recurrenceValue.contains(date.dayOfWeek.name.take(3), ignoreCase = true)
        "INTERVAL" -> {
            val interval = recurrenceValue.toIntOrNull()?.takeIf { it > 0 } ?: 1 // "0" would divide by zero
            (date.toEpochDay() % interval) == 0L
        }
        else -> true
    }
}

@HiltViewModel
class HealthViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    private val repository: TrackerRepository,
    private val foodShortcuts: com.example.loom.data.food.FoodShortcuts,
    private val userPreferences: UserPreferences,
    private val sandboxDao: SandboxDao
) : ViewModel() {

    // --- NEW: Lenient Parser so old JSON backups don't crash when adding new fields ---
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    val hiddenHealthCards: StateFlow<Set<String>> = userPreferences.hiddenHealthCardsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val isFasting: StateFlow<Boolean> = userPreferences.isFastingFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val fastingStartTime: StateFlow<Long> = userPreferences.fastingStartTimeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val fastingGoalHours: StateFlow<Int> = userPreferences.fastingGoalHoursFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16)

    val allSpaces: StateFlow<List<com.example.loom.data.models.SandboxSpace>> = sandboxDao.getActiveSpaces()
        .map { entities -> entities.map { it.toDomainModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedHealthCards: StateFlow<List<Pair<com.example.loom.data.models.SandboxSpace, com.example.loom.data.models.SandboxBlock>>> = allSpaces.map { spaces ->
        val cards = mutableListOf<Pair<com.example.loom.data.models.SandboxSpace, com.example.loom.data.models.SandboxBlock>>()
        // Private spaces stay behind the lock: never surface their cards on other tabs
        spaces.filter { !it.isDeleted && !it.isPrivate }.forEach { space ->
            val foundCards = com.example.loom.ui.sandbox.findPinnedCards(space.blocks, "Health")
            foundCards.forEach { card -> cards.add(Pair(space, card)) }
        }
        cards
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSpaceFromWidget(space: com.example.loom.data.models.SandboxSpace) {
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            withContext(NonCancellable) {
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
    val todayExercises: StateFlow<List<Exercise>> = repository.getTodayExercises().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
        .map { try { jsonParser.decodeFromString<List<String>>(it) } catch (_: Exception) { listOf("Supplement", "Medication", "Pre-workout") } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Supplement", "Medication", "Pre-workout"))

    val supplementsRoster: StateFlow<List<SupplementItem>> = userPreferences.supplementsRosterFlow
        .map { try { jsonParser.decodeFromString<List<SupplementItem>>(it) } catch (_: Exception) { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedSupplements: StateFlow<List<String>> = todayRecord
        .map { try { jsonParser.decodeFromString<List<String>>(it?.completedSupplementsJson ?: "[]") } catch (_: Exception) { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- FIX: Only show ACTIVE supplements on the daily card ---
    val todayScheduledSupplements: StateFlow<List<SupplementItem>> = combine(supplementsRoster, DateUtils.todayDateIdFlow) { roster, todayId ->
        // Logical today (respects the rollover hour) and re-evaluated when the day changes
        val today = LocalDate.parse(todayId)
        roster.filter { it.isScheduledOn(today) }.sortedBy { it.timeOfUse }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- TRENDS & STREAKS ---
    private val _trendDays = MutableStateFlow(30)
    val trendDays: StateFlow<Int> = _trendDays.asStateFlow()
    fun setTrendDays(days: Int) { _trendDays.value = days }

    @OptIn(ExperimentalCoroutinesApi::class)
    val trends: StateFlow<TrendData> = combine(_trendDays, DateUtils.todayDateIdFlow) { days, todayId -> days to LocalDate.parse(todayId) }
        .flatMapLatest { (days, today) ->
            val from = today.minusDays(days - 1L)
            combine(
                repository.getRecordsBetween(from, today),
                userPreferences.targetCaloriesFlow,
                userPreferences.targetProteinFlow,
                userPreferences.waterDailyGoalFlow
            ) { records, calTarget, proteinTarget, waterGoal ->
                val byDate = records.associateBy { it.dateId }
                fun series(target: Float? = null, value: (DailyRecord) -> Float?) = TrendSeries(
                    (0 until days).map { i -> from.plusDays(i.toLong()).let { d -> TrendPoint(d, byDate[d.toString()]?.let(value)) } },
                    target
                )
                TrendData(
                    days = days,
                    weight = series { r -> r.weightLogKg.takeIf { it > 0f } },
                    sleep = series { r -> r.sleepHours.takeIf { it > 0f } },
                    calories = series(calTarget.toFloat()) { r -> r.calories.takeIf { it > 0 }?.toFloat() },
                    protein = series(proteinTarget.toFloat()) { r -> r.protein.takeIf { it > 0 }?.toFloat() },
                    water = series(waterGoal.toFloat()) { r -> r.waterIntakeMl.takeIf { it > 0 }?.toFloat() }
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrendData())

    @OptIn(ExperimentalCoroutinesApi::class)
    val streaks: StateFlow<Streaks> = DateUtils.todayDateIdFlow
        .flatMapLatest { todayId ->
            val today = LocalDate.parse(todayId)
            val from = today.minusDays(365)
            combine(
                repository.getRecordsBetween(from, today),
                repository.getExercisesBetween(from, today),
                userPreferences.waterDailyGoalFlow,
                supplementsRoster
            ) { records, exercises, waterGoal, roster ->
                val byDate = records.associateBy { it.dateId }
                val workoutDays = exercises.mapTo(HashSet()) { it.dateId }
                Streaks(
                    workout = StreakCalculator.compute({ d -> d.toString() in workoutDays }, from, today),
                    water = StreakCalculator.compute({ d -> (byDate[d.toString()]?.waterIntakeMl ?: 0) >= waterGoal && waterGoal > 0 }, from, today),
                    supplements = StreakCalculator.compute({ d ->
                        val scheduled = roster.filter { it.isScheduledOn(d) }.map { it.id }
                        if (scheduled.isEmpty()) null
                        else com.example.loom.data.repository.decodeList<String>(byDate[d.toString()]?.completedSupplementsJson).containsAll(scheduled)
                    }, from, today),
                    skincare = StreakCalculator.compute({ d ->
                        com.example.loom.data.repository.decodeList<String>(byDate[d.toString()]?.completedSkincareRoutinesJson).isNotEmpty()
                    }, from, today),
                    workoutsLast7Days = (0..6).count { today.minusDays(it.toLong()).toString() in workoutDays }
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Streaks())

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
        repository.editRecord { it.copy(completedSupplementsJson = toggleJsonId(it.completedSupplementsJson, id)) }
    }

    // --- NEW: Toggle active state of a supplement ---
    fun toggleSupplementActive(item: SupplementItem) = viewModelScope.launch {
        val current = supplementsRoster.value.toMutableList()
        val index = current.indexOfFirst { it.name.equals(item.name, ignoreCase = true) }

        if (index >= 0) {
            current[index] = current[index].copy(isActive = !current[index].isActive)
        } else {
            current.add(item.copy(id = UUID.randomUUID().toString(), isActive = true))
        }
        userPreferences.saveSupplementsRoster(Json.encodeToString(current))
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
        repository.editRecord { it.copy(completedSupplementsJson = removeJsonIds(it.completedSupplementsJson, listOf(id))) }
    }

    fun toggleCardVisibility(cardName: String, isVisible: Boolean) = viewModelScope.launch { userPreferences.toggleHealthCardVisibility(cardName, isVisible) }
    fun deleteDailyWeight() = viewModelScope.launch { repository.editRecord { it.copy(weightLogKg = 0f) } }

    val customWaterContainers: StateFlow<List<WaterContainer>> = userPreferences.customWaterContainersFlow.map { try { jsonParser.decodeFromString<List<WaterContainer>>(it) } catch (_: Exception) { emptyList() } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val waterLogs: StateFlow<List<WaterLog>> = todayRecord.map { try { jsonParser.decodeFromString<List<WaterLog>>(it?.waterLogsJson ?: "[]") } catch (_: Exception) { emptyList() } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveWaterGoal(goal: Int, isAuto: Boolean) = viewModelScope.launch { userPreferences.saveWaterDailyGoal(goal, isAuto) }
    fun saveCustomWaterContainers(containers: List<WaterContainer>) = viewModelScope.launch { userPreferences.saveCustomWaterContainers(Json.encodeToString(containers)) }
    fun logDetailedWater(container: WaterContainer) = viewModelScope.launch { val newLog = WaterLog(id = UUID.randomUUID().toString(), containerName = container.name, emoji = container.iconEmoji, amountMl = container.volumeMl, hydrationFactor = container.hydrationFactor, timestamp = System.currentTimeMillis()); repository.editWaterLogs { it + newLog } }
    fun removeDetailedWaterLog(logId: String) = viewModelScope.launch { repository.editWaterLogs { logs -> logs.filter { it.id != logId } } }

    val consumedFoods: StateFlow<List<ConsumedFoodLog>> = todayRecord.map { try { jsonParser.decodeFromString<List<ConsumedFoodLog>>(it?.consumedFoodsJson ?: "[]") } catch (_: Exception) { emptyList() } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val completedSkincareRoutines: StateFlow<List<String>> = todayRecord.map { try { jsonParser.decodeFromString<List<String>>(it?.completedSkincareRoutinesJson ?: "[]") } catch (_: Exception) { emptyList() } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val completedSkincareSteps: StateFlow<List<String>> = todayRecord.map { try { jsonParser.decodeFromString<List<String>>(it?.completedSkincareStepsJson ?: "[]") } catch (_: Exception) { emptyList() } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _foodSearchQuery = MutableStateFlow("")
    val foodSearchQuery: StateFlow<String> = _foodSearchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchedFoodItems: StateFlow<List<FoodItem>> = _foodSearchQuery.debounce(300.milliseconds).flatMapLatest { query -> if (query.isBlank()) foodShortcuts.quickFoods() else repository.searchFoodItems(query) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateFoodSearchQuery(query: String) { _foodSearchQuery.value = query }

    val favoriteFoods: StateFlow<Set<String>> = foodShortcuts.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val mealTemplates: StateFlow<List<com.example.loom.data.food.MealTemplate>> = foodShortcuts.templates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun toggleFavoriteFood(name: String) = viewModelScope.launch { foodShortcuts.toggleFavorite(name) }
    fun copyYesterdaysMeals(onDone: (Int) -> Unit) = viewModelScope.launch {
        onDone(foodShortcuts.copyMeals(DateUtils.getLogicalToday().minusDays(1).toString()))
    }
    fun saveMealTemplate(name: String, onDone: (Boolean) -> Unit) = viewModelScope.launch { onDone(foodShortcuts.saveTemplate(name)) }
    fun applyMealTemplate(template: com.example.loom.data.food.MealTemplate) = viewModelScope.launch { foodShortcuts.applyTemplate(template) }
    fun deleteMealTemplate(name: String) = viewModelScope.launch { foodShortcuts.deleteTemplate(name) }

    init { viewModelScope.launch { repository.prepopulateSkincareProductsIfEmpty() } }

    fun saveBioMetrics(weight: Float, height: Int, age: Int, gender: String, activity: String, goal: String, bodyFat: Float, dietType: String, phaseWeeks: Int, isCycling: Boolean) = viewModelScope.launch { userPreferences.saveBioMetrics(weight, height, age, gender, activity, goal, bodyFat, dietType, phaseWeeks, isCycling) }
    fun logDailyWeight(weightKg: Float) = viewModelScope.launch { repository.editRecord { it.copy(weightLogKg = weightKg) } }
    fun saveDefaultWaterIncrement(ml: Int) = viewModelScope.launch { userPreferences.saveDefaultWaterIncrement(ml) }
    fun addFoodItem(food: FoodItem) = viewModelScope.launch { repository.insertFoodItem(food.copy(isUserCreated = true)) }
    fun deleteFoodItem(food: FoodItem) = viewModelScope.launch { repository.deleteFoodItem(food) }
    fun logFoodItem(food: FoodItem, servings: Float) = viewModelScope.launch { val totalCals = (food.calories * servings).toInt(); val totalPro = (food.protein * servings).toInt(); val totalCarbs = (food.carbs * servings).toInt(); val totalFats = (food.fats * servings).toInt(); val currentTime = System.currentTimeMillis(); val log = ConsumedFoodLog(id = UUID.randomUUID().toString(), foodName = food.name, emoji = getMealEmojiForTime(currentTime), servings = servings, servingUnit = food.servingUnit, calories = totalCals, protein = totalPro, carbs = totalCarbs, fats = totalFats, timestamp = currentTime); repository.addConsumedFood(log) }
    fun removeConsumedFoodLog(log: ConsumedFoodLog) = viewModelScope.launch { repository.removeConsumedFood(log.id) }
    suspend fun exerciseStats(name: String): ExerciseStats = ExerciseStats.from(repository.getExerciseHistory(name))
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
    fun toggleSkincareStep(routineName: String, productName: String) = viewModelScope.launch { repository.editRecord { it.copy(completedSkincareStepsJson = toggleJsonId(it.completedSkincareStepsJson, "${routineName}_${productName}")) } }
    fun logSkincareRoutine(routineName: String) = viewModelScope.launch { repository.editRecord { it.copy(completedSkincareRoutinesJson = addJsonIds(it.completedSkincareRoutinesJson, listOf(routineName))) } }
    fun removeLoggedSkincareRoutine(routineName: String, products: List<String>) = viewModelScope.launch { repository.editRecord { it.copy(completedSkincareRoutinesJson = removeJsonIds(it.completedSkincareRoutinesJson, listOf(routineName)), completedSkincareStepsJson = removeJsonIds(it.completedSkincareStepsJson, products.map { p -> "${routineName}_$p" })) } }
    fun toggleAllSkincareSteps(routineName: String, products: List<String>, isCompleted: Boolean) = viewModelScope.launch { val stepIds = products.map { p -> "${routineName}_$p" }; repository.editRecord { it.copy(completedSkincareStepsJson = if (isCompleted) addJsonIds(it.completedSkincareStepsJson, stepIds) else removeJsonIds(it.completedSkincareStepsJson, stepIds)) } }
    private fun getMealEmojiForTime(timestamp: Long): String { val hour = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY); return when (hour) { in 5..9 -> "🍳"; in 10..12 -> "🍎"; in 13..15 -> "🍛"; in 16..19 -> "☕"; in 20..23 -> "🍲"; else -> "🌙" } }
    fun toggleFasting(start: Boolean, goalHours: Int) = viewModelScope.launch {
        if (start) userPreferences.setFastingState(true, System.currentTimeMillis(), goalHours) else userPreferences.setFastingState(false, 0L, goalHours)
        com.example.loom.reminders.ReminderScheduler.rescheduleAll(appContext, userPreferences)
    }
    fun logSleep(hours: Float, rating: Int) = viewModelScope.launch { repository.editRecord { it.copy(sleepHours = hours, sleepRating = rating) } }
    fun logMeasurements(measurements: Map<String, Float>) = viewModelScope.launch { repository.editRecord { it.copy(measurementsJson = Json.encodeToString(measurements)) } }
}