package com.example.loom.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferences @Inject constructor(private val context: Context) {

    companion object {
        val WEIGHT = floatPreferencesKey("bio_weight")
        val HEIGHT = intPreferencesKey("bio_height")
        val AGE = intPreferencesKey("bio_age")
        val GENDER = stringPreferencesKey("bio_gender")
        val ACTIVITY_LEVEL = stringPreferencesKey("bio_activity")
        val FITNESS_GOAL = stringPreferencesKey("bio_goal")

        val TARGET_CALORIES = intPreferencesKey("target_calories")
        val TARGET_PROTEIN = intPreferencesKey("target_protein")
        val TARGET_CARBS = intPreferencesKey("target_carbs")
        val TARGET_FATS = intPreferencesKey("target_fats")

        val DEFAULT_WATER_ML = intPreferencesKey("default_water_ml")
        val WATER_DAILY_GOAL = intPreferencesKey("water_daily_goal")
        val AUTO_WATER_GOAL = booleanPreferencesKey("auto_water_goal")
        val CUSTOM_WATER_CONTAINERS = stringPreferencesKey("custom_water_containers")

        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val APP_THEME = stringPreferencesKey("app_theme")
    }

    val appThemeFlow: Flow<String> = context.dataStore.data.map { it[APP_THEME] ?: "System" }
    val weightFlow: Flow<Float> = context.dataStore.data.map { it[WEIGHT] ?: 70f }
    val heightFlow: Flow<Int> = context.dataStore.data.map { it[HEIGHT] ?: 175 }
    val ageFlow: Flow<Int> = context.dataStore.data.map { it[AGE] ?: 21 }
    val genderFlow: Flow<String> = context.dataStore.data.map { it[GENDER] ?: "Male" }
    val activityFlow: Flow<String> = context.dataStore.data.map { it[ACTIVITY_LEVEL] ?: "Moderate" }
    val goalFlow: Flow<String> = context.dataStore.data.map { it[FITNESS_GOAL] ?: "Maintain" }

    val targetCaloriesFlow: Flow<Int> = context.dataStore.data.map { it[TARGET_CALORIES] ?: 2800 }
    val targetProteinFlow: Flow<Int> = context.dataStore.data.map { it[TARGET_PROTEIN] ?: 140 }
    val targetCarbsFlow: Flow<Int> = context.dataStore.data.map { it[TARGET_CARBS] ?: 350 }
    val targetFatsFlow: Flow<Int> = context.dataStore.data.map { it[TARGET_FATS] ?: 75 }

    val defaultWaterIncrementFlow: Flow<Int> = context.dataStore.data.map { it[DEFAULT_WATER_ML] ?: 250 }
    val waterDailyGoalFlow: Flow<Int> = context.dataStore.data.map { it[WATER_DAILY_GOAL] ?: 3000 }
    val autoWaterGoalFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_WATER_GOAL] ?: true }

    // Default containers JSON string
    val customWaterContainersFlow: Flow<String> = context.dataStore.data.map {
        it[CUSTOM_WATER_CONTAINERS] ?: """[{"id":"1","name":"Glass","iconEmoji":"LocalDrink","volumeMl":250,"hydrationFactor":1.0},{"id":"2","name":"Shaker","iconEmoji":"LocalCafe","volumeMl":600,"hydrationFactor":1.0},{"id":"3","name":"Bottle","iconEmoji":"WaterDrop","volumeMl":1500,"hydrationFactor":1.0}]"""
    }

    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED] ?: true }

    suspend fun saveAppTheme(theme: String) { context.dataStore.edit { it[APP_THEME] = theme } }

    suspend fun saveBioMetrics(weight: Float, height: Int, age: Int, gender: String, activity: String, goal: String) {
        context.dataStore.edit { preferences ->
            preferences[WEIGHT] = weight
            preferences[HEIGHT] = height
            preferences[AGE] = age
            preferences[GENDER] = gender
            preferences[ACTIVITY_LEVEL] = activity
            preferences[FITNESS_GOAL] = goal

            // Mifflin-St Jeor
            val bmr = if (gender == "Male") { (10 * weight) + (6.25 * height) - (5 * age) + 5 } else { (10 * weight) + (6.25 * height) - (5 * age) - 161 }
            val multiplier = when (activity) { "Sedentary" -> 1.2f; "Light" -> 1.375f; "Moderate" -> 1.55f; "Active" -> 1.725f; "Very Active" -> 1.9f; else -> 1.2f }
            var tdee = bmr * multiplier
            tdee += when (goal) { "Lose Weight" -> -500f; "Build Muscle" -> 300f; else -> 0f }

            val cals = tdee.toInt()
            val protein = (weight * 2.2f).toInt()
            val fats = (weight * 0.8f).toInt()
            val carbs = ((cals - (protein * 4) - (fats * 9)) / 4).coerceAtLeast(0)

            preferences[TARGET_CALORIES] = cals
            preferences[TARGET_PROTEIN] = protein
            preferences[TARGET_FATS] = fats
            preferences[TARGET_CARBS] = carbs

            // Auto-calculate hydration (35ml per kg)
            val isAutoWater = preferences[AUTO_WATER_GOAL] ?: true
            if (isAutoWater) {
                preferences[WATER_DAILY_GOAL] = (weight * 35).toInt()
            }
        }
    }

    suspend fun saveDefaultWaterIncrement(ml: Int) { context.dataStore.edit { it[DEFAULT_WATER_ML] = ml } }
    suspend fun saveWaterDailyGoal(goalMl: Int, isAuto: Boolean) {
        context.dataStore.edit {
            it[WATER_DAILY_GOAL] = goalMl
            it[AUTO_WATER_GOAL] = isAuto
        }
    }
    suspend fun saveCustomWaterContainers(json: String) { context.dataStore.edit { it[CUSTOM_WATER_CONTAINERS] = json } }

    suspend fun saveTargets(calories: Int, protein: Int, carbs: Int, fats: Int) {
        context.dataStore.edit { prefs ->
            prefs[TARGET_CALORIES] = calories; prefs[TARGET_PROTEIN] = protein; prefs[TARGET_CARBS] = carbs; prefs[TARGET_FATS] = fats
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) { context.dataStore.edit { it[BIOMETRIC_ENABLED] = enabled } }
}