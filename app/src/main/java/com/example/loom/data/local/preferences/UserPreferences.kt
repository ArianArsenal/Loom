package com.example.loom.data.local.preferences

import android.content.Context
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
    }

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

    suspend fun saveBioMetrics(weight: Float, height: Int, age: Int, gender: String, activity: String, goal: String) {
        context.dataStore.edit { preferences ->
            preferences[WEIGHT] = weight
            preferences[HEIGHT] = height
            preferences[AGE] = age
            preferences[GENDER] = gender
            preferences[ACTIVITY_LEVEL] = activity
            preferences[FITNESS_GOAL] = goal

            // Mifflin-St Jeor Equation
            val bmr = if (gender == "Male") {
                (10 * weight) + (6.25 * height) - (5 * age) + 5
            } else {
                (10 * weight) + (6.25 * height) - (5 * age) - 161
            }

            val multiplier = when (activity) {
                "Sedentary" -> 1.2f
                "Light" -> 1.375f
                "Moderate" -> 1.55f
                "Active" -> 1.725f
                "Very Active" -> 1.9f
                else -> 1.2f
            }

            var tdee = bmr * multiplier

            tdee += when (goal) {
                "Lose Weight" -> -500f
                "Build Muscle" -> 300f
                else -> 0f
            }

            val cals = tdee.toInt()
            val protein = (weight * 2.2f).toInt() // 2.2g per kg
            val fats = (weight * 0.8f).toInt()    // 0.8g per kg
            val carbs = ((cals - (protein * 4) - (fats * 9)) / 4).coerceAtLeast(0)

            preferences[TARGET_CALORIES] = cals
            preferences[TARGET_PROTEIN] = protein
            preferences[TARGET_FATS] = fats
            preferences[TARGET_CARBS] = carbs
        }
    }

    suspend fun saveDefaultWaterIncrement(ml: Int) {
        context.dataStore.edit { it[DEFAULT_WATER_ML] = ml }
    }

    // For manual overrides if needed
    suspend fun saveTargets(calories: Int, protein: Int, carbs: Int, fats: Int) {
        context.dataStore.edit { prefs ->
            prefs[TARGET_CALORIES] = calories
            prefs[TARGET_PROTEIN] = protein
            prefs[TARGET_CARBS] = carbs
            prefs[TARGET_FATS] = fats
        }
    }
}