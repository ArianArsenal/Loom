package com.example.loom.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.entity.*
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.data.repository.TrackerRepository
import com.example.loom.ui.health.SupplementItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DailySnapshot(
    val record: DailyRecord?,
    val tasks: List<Task>,
    val exercises: List<Exercise>
)

// NEW: Data class for our Monthly Overview Dashboard
data class MonthlyAnalytics(
    val averageSleep: Float = 0f,
    val workoutDays: Int = 0,
    val averageCalories: Int = 0,
    val tasksCompleted: Int = 0,
    val totalTasks: Int = 0
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: TrackerRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val currentMonth = MutableStateFlow(YearMonth.now())

    private val _selectedDateId = MutableStateFlow<String?>(null)
    val selectedDateId: StateFlow<String?> = _selectedDateId.asStateFlow()

    private val _selectedDateSnapshot = MutableStateFlow<DailySnapshot?>(null)
    val selectedDateSnapshot: StateFlow<DailySnapshot?> = _selectedDateSnapshot.asStateFlow()

    val allPresets: StateFlow<List<WorkoutPreset>> = repository.getAllPresets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val exerciseTemplates: StateFlow<List<ExerciseTemplate>> = repository.getAllTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSkincareRoutines: StateFlow<List<SkincareRoutine>> = repository.getAllSkincareRoutines().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSkincareProducts: StateFlow<List<SkincareProduct>> = repository.getAllSkincareProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val targetProtein: StateFlow<Int> = userPreferences.targetProteinFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 140)
    val targetCarbs: StateFlow<Int> = userPreferences.targetCarbsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 350)
    val targetFats: StateFlow<Int> = userPreferences.targetFatsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 75)
    val weightFlow: StateFlow<Float> = userPreferences.weightFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70f)

    val supplementsRoster: StateFlow<List<SupplementItem>> = userPreferences.supplementsRosterFlow
        .map { try { Json.decodeFromString<List<SupplementItem>>(it) } catch (e: Exception) { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supplementCategories: StateFlow<List<String>> = userPreferences.supplementCategoriesFlow
        .map { try { Json.decodeFromString<List<String>>(it) } catch (e: Exception) { listOf("Supplement", "Medication", "Pre-workout") } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Supplement", "Medication", "Pre-workout"))

    val selectedDateScheduledSupplements: StateFlow<List<SupplementItem>> = combine(
        supplementsRoster,
        selectedDateId
    ) { roster, dateId ->
        if (dateId == null) return@combine emptyList()
        val date = LocalDate.parse(dateId)
        roster.filter { item ->
            when (item.recurrenceType) {
                "DAILY" -> true
                "SPECIFIC_DAYS" -> {
                    val dayStr = date.dayOfWeek.name.take(3)
                    item.recurrenceValue.contains(dayStr, ignoreCase = true)
                }
                "INTERVAL" -> {
                    val interval = item.recurrenceValue.toIntOrNull() ?: 1
                    (date.toEpochDay() % interval) == 0L
                }
                else -> true
            }
        }.sortedBy { it.timeOfUse }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                    val cleanFoods = record.consumedFoodsJson.replace("\\s".toRegex(), "")
                    val hasFood = cleanFoods != "" && cleanFoods != "[]" && cleanFoods != "null"

                    val cleanRoutines = record.completedSkincareRoutinesJson.replace("\\s".toRegex(), "")
                    val hasRoutine = cleanRoutines != "" && cleanRoutines != "[]" && cleanRoutines != "null"

                    val cleanSteps = record.completedSkincareStepsJson.replace("\\s".toRegex(), "")
                    val hasSteps = cleanSteps != "" && cleanSteps != "[]" && cleanSteps != "null"

                    val cleanSupplements = record.completedSupplementsJson.replace("\\s".toRegex(), "")
                    val hasSupplements = cleanSupplements != "" && cleanSupplements != "[]" && cleanSupplements != "null"

                    val hasMacros = record.calories > 0 || record.protein > 0 || record.carbs > 0 || record.fats > 0

                    if (hasMacros || record.waterIntakeMl > 0 || record.diaryEntry.isNotBlank() || hasFood || hasRoutine || hasSteps || hasSupplements) {
                        activeSet.add(record.dateId)
                    }
                }

                exercises.forEach { ex -> activeSet.add(ex.dateId) }

                tasks.forEach { task ->
                    if (task.isCompleted) {
                        val dateStr = if (task.completionDateMillis > 0) {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(task.completionDateMillis))
                        } else {
                            task.targetDate
                        }
                        if (dateStr.startsWith(prefix)) activeSet.add(dateStr)
                    } else {
                        if (task.targetDate.startsWith(prefix)) activeSet.add(task.targetDate)
                    }
                }

                activeSet
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // NEW: Analytics Engine computes averages for the month
    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyAnalytics: StateFlow<MonthlyAnalytics> = currentMonth
        .flatMapLatest { yearMonth ->
            val prefix = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            combine(
                repository.getRecordsForMonth(prefix),
                repository.getExercisesForMonth(prefix),
                repository.getAllTasks()
            ) { records, exercises, allTasks ->

                val sleepDays = records.filter { it.sleepHours > 0f }
                val avgSleep = if (sleepDays.isNotEmpty()) sleepDays.map { it.sleepHours }.average().toFloat() else 0f

                val calDays = records.filter { it.calories > 0 }
                val avgCals = if (calDays.isNotEmpty()) calDays.map { it.calories }.average().toInt() else 0

                val workoutDays = exercises.map { it.dateId }.distinct().count()

                val monthTasks = allTasks.filter {
                    it.targetDate.startsWith(prefix) ||
                            (it.isCompleted && it.completionDateMillis > 0 && SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.completionDateMillis)) == prefix)
                }
                val tasksCompleted = monthTasks.count { it.isCompleted }

                MonthlyAnalytics(avgSleep, workoutDays, avgCals, tasksCompleted, monthTasks.size)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyAnalytics())

    fun nextMonth() { currentMonth.value = currentMonth.value.plusMonths(1) }
    fun previousMonth() { currentMonth.value = currentMonth.value.minusMonths(1) }

    private var snapshotJob: Job? = null

    fun selectDate(dateId: String) {
        _selectedDateId.value = dateId

        snapshotJob?.cancel()
        snapshotJob = viewModelScope.launch {
            combine(
                repository.getDailyRecordFlow(dateId),
                repository.getAllTasks(),
                repository.getExercisesForDate(dateId)
            ) { record, allTasks, exercises ->

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateMillis = sdf.parse(dateId)?.time ?: 0L
                val endOfDay = dateMillis + 86400000L

                val tasksForDay = allTasks.filter { task ->
                    if (task.isCompleted) {
                        if (task.completionDateMillis > 0) {
                            task.completionDateMillis in dateMillis until endOfDay
                        } else {
                            task.targetDate == dateId
                        }
                    } else {
                        task.targetDate == dateId
                    }
                }.sortedWith(compareBy({ it.isCompleted }, { -it.priority }))

                DailySnapshot(record, tasksForDay, exercises)
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

    // --- FOOD SEARCH & PANTRY ---
    private val _foodSearchQuery = MutableStateFlow("")
    val foodSearchQuery: StateFlow<String> = _foodSearchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchedFoodItems: StateFlow<List<FoodItem>> = _foodSearchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) repository.searchFoodItems("مرغ")
            else repository.searchFoodItems(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateFoodSearchQuery(query: String) { _foodSearchQuery.value = query }
    fun addFoodItem(food: FoodItem) = viewModelScope.launch { repository.insertFoodItem(food) }
    fun deleteFoodItem(food: FoodItem) = viewModelScope.launch { repository.deleteFoodItem(food) }

    fun logFoodItemToPast(food: FoodItem, servings: Float) = viewModelScope.launch {
        val record = _selectedDateSnapshot.value?.record ?: return@launch
        val totalCals = (food.calories * servings).toInt()
        val totalPro = (food.protein * servings).toInt()
        val totalCarbs = (food.carbs * servings).toInt()
        val totalFats = (food.fats * servings).toInt()
        val currentTime = System.currentTimeMillis()

        val hour = java.util.Calendar.getInstance().apply { timeInMillis = currentTime }.get(java.util.Calendar.HOUR_OF_DAY)
        val emoji = when (hour) { in 5..9 -> "🍳"; in 10..12 -> "🍎"; in 13..15 -> "🍛"; in 16..19 -> "☕"; in 20..23 -> "🍲"; else -> "🌙" }

        val log = ConsumedFoodLog(
            id = java.util.UUID.randomUUID().toString(),
            foodName = food.name, emoji = emoji, servings = servings, servingUnit = food.servingUnit,
            calories = totalCals, protein = totalPro, carbs = totalCarbs, fats = totalFats, timestamp = currentTime
        )

        val currentLogs = try { Json.decodeFromString<List<ConsumedFoodLog>>(record.consumedFoodsJson) } catch (e: Exception) { emptyList() }
        repository.updateDailyRecord(record.copy(
            calories = record.calories + totalCals,
            protein = record.protein + totalPro,
            carbs = record.carbs + totalCarbs,
            fats = record.fats + totalFats,
            consumedFoodsJson = Json.encodeToString(currentLogs + log)
        ))
    }

    fun removeConsumedFoodLog(log: ConsumedFoodLog) = viewModelScope.launch {
        val record = _selectedDateSnapshot.value?.record ?: return@launch
        val currentLogs = try { Json.decodeFromString<List<ConsumedFoodLog>>(record.consumedFoodsJson) } catch (e: Exception) { emptyList() }
        val updatedLogs = currentLogs.filter { it.id != log.id }
        repository.updateDailyRecord(record.copy(
            calories = record.calories - log.calories,
            protein = record.protein - log.protein,
            carbs = record.carbs - log.carbs,
            fats = record.fats - log.fats,
            consumedFoodsJson = Json.encodeToString(updatedLogs)
        ))
    }

    // --- WEIGHT & SLEEP ---
    fun logDailyWeight(weightKg: Float) = viewModelScope.launch {
        val record = _selectedDateSnapshot.value?.record ?: return@launch
        repository.updateDailyRecord(record.copy(weightLogKg = weightKg))
    }

    fun deleteDailyWeight() = viewModelScope.launch {
        val record = _selectedDateSnapshot.value?.record ?: return@launch
        repository.updateDailyRecord(record.copy(weightLogKg = 0f))
    }

    fun logSleep(hours: Float, rating: Int) = viewModelScope.launch {
        val record = _selectedDateSnapshot.value?.record ?: return@launch
        repository.updateDailyRecord(record.copy(sleepHours = hours, sleepRating = rating))
    }

    fun deleteSleep() = viewModelScope.launch {
        val record = _selectedDateSnapshot.value?.record ?: return@launch
        repository.updateDailyRecord(record.copy(sleepHours = 0f, sleepRating = 0))
    }

    // --- SUPPLEMENTS ---
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

    fun saveSupplementItem(item: SupplementItem) = viewModelScope.launch {
        val current = supplementsRoster.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) current[index] = item else current.add(item)
        userPreferences.saveSupplementsRoster(Json.encodeToString(current))
    }

    fun deleteSupplementItem(id: String) = viewModelScope.launch {
        val current = supplementsRoster.value.filter { it.id != id }
        userPreferences.saveSupplementsRoster(Json.encodeToString(current))

        val record = _selectedDateSnapshot.value?.record ?: return@launch
        val currentCompleted = try { Json.decodeFromString<List<String>>(record.completedSupplementsJson) } catch (e: Exception) { emptyList() }.toMutableList()
        if (currentCompleted.contains(id)) {
            currentCompleted.remove(id)
            repository.updateDailyRecord(record.copy(completedSupplementsJson = Json.encodeToString(currentCompleted)))
        }
    }

    fun toggleSupplement(id: String) = viewModelScope.launch {
        val record = _selectedDateSnapshot.value?.record ?: return@launch
        val currentList = try { Json.decodeFromString<List<String>>(record.completedSupplementsJson) } catch (e: Exception) { emptyList() }.toMutableList()
        if (currentList.contains(id)) currentList.remove(id) else currentList.add(id)
        repository.updateDailyRecord(record.copy(completedSupplementsJson = Json.encodeToString(currentList)))
    }

    // --- SKINCARE ---
    fun saveSkincareRoutine(routine: SkincareRoutine) = viewModelScope.launch { repository.insertSkincareRoutine(routine) }
    fun deleteSkincareRoutine(routine: SkincareRoutine) = viewModelScope.launch { repository.deleteSkincareRoutine(routine) }
    fun addSkincareProduct(name: String) = viewModelScope.launch { repository.insertSkincareProduct(SkincareProduct(name = name)) }

    fun logSkincareRoutine(routineName: String) = viewModelScope.launch {
        val record = _selectedDateSnapshot.value?.record ?: return@launch
        val currentRoutines = try { Json.decodeFromString<List<String>>(record.completedSkincareRoutinesJson) } catch (e: Exception) { emptyList() }.toMutableList()
        if (!currentRoutines.contains(routineName)) {
            currentRoutines.add(routineName)
            repository.updateDailyRecord(record.copy(completedSkincareRoutinesJson = Json.encodeToString(currentRoutines)))
        }
    }

    fun toggleSkincareStep(routineName: String, productName: String) = viewModelScope.launch {
        val record = _selectedDateSnapshot.value?.record ?: return@launch
        val stepId = "${routineName}_${productName}"
        val currentList = try { Json.decodeFromString<List<String>>(record.completedSkincareStepsJson) } catch (e: Exception) { emptyList() }.toMutableList()
        if (currentList.contains(stepId)) currentList.remove(stepId) else currentList.add(stepId)
        repository.updateDailyRecord(record.copy(completedSkincareStepsJson = Json.encodeToString(currentList)))
    }

    fun toggleAllSkincareSteps(routineName: String, products: List<String>, isCompleted: Boolean) = viewModelScope.launch {
        val record = _selectedDateSnapshot.value?.record ?: return@launch
        val currentSteps = try { Json.decodeFromString<List<String>>(record.completedSkincareStepsJson) } catch (e: Exception) { emptyList() }.toMutableList()
        products.forEach { p ->
            val stepId = "${routineName}_$p"
            if (isCompleted) {
                if (!currentSteps.contains(stepId)) currentSteps.add(stepId)
            } else {
                currentSteps.remove(stepId)
            }
        }
        repository.updateDailyRecord(record.copy(completedSkincareStepsJson = Json.encodeToString(currentSteps)))
    }

    fun removeLoggedSkincareRoutine(routineName: String, products: List<String>) = viewModelScope.launch {
        val record = _selectedDateSnapshot.value?.record ?: return@launch
        val currentRoutines = try { Json.decodeFromString<List<String>>(record.completedSkincareRoutinesJson) } catch (e: Exception) { emptyList() }.toMutableList()
        val currentSteps = try { Json.decodeFromString<List<String>>(record.completedSkincareStepsJson) } catch (e: Exception) { emptyList() }.toMutableList()
        currentRoutines.remove(routineName)
        products.forEach { p -> currentSteps.remove("${routineName}_$p") }
        repository.updateDailyRecord(record.copy(completedSkincareRoutinesJson = Json.encodeToString(currentRoutines), completedSkincareStepsJson = Json.encodeToString(currentSteps)))
    }

    // --- WORKOUTS ---
    fun toggleExerciseCompletion(exercise: Exercise) = viewModelScope.launch {
        repository.updateExercise(exercise.copy(isCompleted = !exercise.isCompleted))
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