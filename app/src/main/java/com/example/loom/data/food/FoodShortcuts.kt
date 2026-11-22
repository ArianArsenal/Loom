package com.example.loom.data.food

import com.example.loom.data.local.dao.TrackerDao
import com.example.loom.data.local.entity.ConsumedFoodLog
import com.example.loom.data.local.entity.FoodItem
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.data.repository.TrackerRepository
import com.example.loom.data.repository.decodeList
import com.example.loom.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class MealTemplate(val name: String, val logs: List<ConsumedFoodLog>)

/** Faster food logging: favourites, recently eaten foods, copying a day's meals and meal templates. */
@Singleton
class FoodShortcuts @Inject constructor(
    private val repository: TrackerRepository,
    private val dao: TrackerDao,
    private val prefs: UserPreferences
) {
    val favorites: Flow<Set<String>> = prefs.favoriteFoodsFlow

    val templates: Flow<List<MealTemplate>> = prefs.mealTemplatesFlow.map { decodeList<MealTemplate>(it) }

    /** Shown when the pantry search is empty: favourites first, then foods eaten in the last two weeks. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun quickFoods(): Flow<List<FoodItem>> = DateUtils.todayDateIdFlow
        .flatMapLatest { todayId ->
            val today = LocalDate.parse(todayId)
            combine(favorites, repository.getRecordsBetween(today.minusDays(14), today)) { favs, records ->
                val recent = records.flatMap { decodeList<ConsumedFoodLog>(it.consumedFoodsJson) }
                    .filter { it.foodName != MANUAL_ENTRY }
                    .sortedByDescending { it.timestamp }
                    .map { it.foodName }
                (favs.sorted() + recent).distinct().take(30)
            }
        }
        .map { names ->
            if (names.isEmpty()) return@map emptyList()
            val byName = dao.getFoodsByNames(names).associateBy { it.name }
            names.mapNotNull { byName[it] }
        }

    suspend fun toggleFavorite(foodName: String) = prefs.toggleFavoriteFood(foodName)

    /** Re-logs every meal from [fromDateId] onto [toDateId] (now-stamped, new ids). Returns how many were copied. */
    suspend fun copyMeals(fromDateId: String, toDateId: String = DateUtils.getTodayDateId()): Int {
        val logs = decodeList<ConsumedFoodLog>(dao.getDailyRecordSync(fromDateId)?.consumedFoodsJson)
        addLogs(logs, toDateId)
        return logs.size
    }

    suspend fun saveTemplate(name: String, fromDateId: String = DateUtils.getTodayDateId()): Boolean {
        val logs = decodeList<ConsumedFoodLog>(dao.getDailyRecordSync(fromDateId)?.consumedFoodsJson)
        if (logs.isEmpty() || name.isBlank()) return false
        val current = templates.first().filter { it.name != name }
        prefs.saveMealTemplates(Json.encodeToString(current + MealTemplate(name.trim(), logs)))
        return true
    }

    suspend fun applyTemplate(template: MealTemplate, dateId: String = DateUtils.getTodayDateId()) = addLogs(template.logs, dateId)

    suspend fun deleteTemplate(name: String) {
        prefs.saveMealTemplates(Json.encodeToString(templates.first().filter { it.name != name }))
    }

    private suspend fun addLogs(logs: List<ConsumedFoodLog>, dateId: String) {
        val now = System.currentTimeMillis()
        logs.forEach { log -> repository.addConsumedFood(log.copy(id = UUID.randomUUID().toString(), timestamp = now), dateId) }
    }

    companion object {
        private const val MANUAL_ENTRY = "Manual Entry"
    }
}
