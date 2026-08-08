package com.example.loom.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
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

        // NEW BIO ENGINE KEYS
        val BODY_FAT = floatPreferencesKey("bio_body_fat")
        val DIET_TYPE = stringPreferencesKey("bio_diet_type")
        val PHASE_START_TIME = longPreferencesKey("bio_phase_start")
        val PHASE_WEEKS = intPreferencesKey("bio_phase_weeks")
        val CALORIE_CYCLING = booleanPreferencesKey("bio_calorie_cycling")

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

        // Add to your companion object keys:
        val FASTING_START_TIME = longPreferencesKey("fasting_start_time")
        val FASTING_GOAL_HOURS = intPreferencesKey("fasting_goal_hours")
        val IS_FASTING = booleanPreferencesKey("is_fasting")

        val HIDDEN_HEALTH_CARDS = stringPreferencesKey("hidden_health_cards")

        val SUPPLEMENTS_ROSTER = stringPreferencesKey("supplements_roster")
        val SUPPLEMENT_CATEGORIES = stringPreferencesKey("supplement_categories")
    }

    val hiddenHealthCardsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        try {
            val json = preferences[HIDDEN_HEALTH_CARDS] ?: "[]"
            kotlinx.serialization.json.Json.decodeFromString<Set<String>>(json)
        } catch (e: Exception) {
            emptySet()
        }
    }

    // Add these state flows:
    val fastingStartTimeFlow: Flow<Long> = context.dataStore.data.map { it[FASTING_START_TIME] ?: 0L }
    val fastingGoalHoursFlow: Flow<Int> = context.dataStore.data.map { it[FASTING_GOAL_HOURS] ?: 16 }
    val isFastingFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_FASTING] ?: false }

    val appThemeFlow: Flow<String> = context.dataStore.data.map { it[APP_THEME] ?: "System" }
    val weightFlow: Flow<Float> = context.dataStore.data.map { it[WEIGHT] ?: 70f }
    val heightFlow: Flow<Int> = context.dataStore.data.map { it[HEIGHT] ?: 175 }
    val ageFlow: Flow<Int> = context.dataStore.data.map { it[AGE] ?: 21 }
    val genderFlow: Flow<String> = context.dataStore.data.map { it[GENDER] ?: "Male" }
    val activityFlow: Flow<String> = context.dataStore.data.map { it[ACTIVITY_LEVEL] ?: "Moderate" }
    val goalFlow: Flow<String> = context.dataStore.data.map { it[FITNESS_GOAL] ?: "Maintain" }

    val bodyFatFlow: Flow<Float> = context.dataStore.data.map { it[BODY_FAT] ?: 0f }
    val dietTypeFlow: Flow<String> = context.dataStore.data.map { it[DIET_TYPE] ?: "Balanced" }
    val phaseStartTimeFlow: Flow<Long> = context.dataStore.data.map { it[PHASE_START_TIME] ?: System.currentTimeMillis() }
    val phaseWeeksFlow: Flow<Int> = context.dataStore.data.map { it[PHASE_WEEKS] ?: 12 }
    val calorieCyclingFlow: Flow<Boolean> = context.dataStore.data.map { it[CALORIE_CYCLING] ?: false }

    val targetCaloriesFlow: Flow<Int> = context.dataStore.data.map { it[TARGET_CALORIES] ?: 2800 }
    val targetProteinFlow: Flow<Int> = context.dataStore.data.map { it[TARGET_PROTEIN] ?: 140 }
    val targetCarbsFlow: Flow<Int> = context.dataStore.data.map { it[TARGET_CARBS] ?: 350 }
    val targetFatsFlow: Flow<Int> = context.dataStore.data.map { it[TARGET_FATS] ?: 75 }

    val defaultWaterIncrementFlow: Flow<Int> = context.dataStore.data.map { it[DEFAULT_WATER_ML] ?: 250 }
    val waterDailyGoalFlow: Flow<Int> = context.dataStore.data.map { it[WATER_DAILY_GOAL] ?: 3000 }
    val autoWaterGoalFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_WATER_GOAL] ?: true }

    val customWaterContainersFlow: Flow<String> = context.dataStore.data.map {
        it[CUSTOM_WATER_CONTAINERS] ?: """[{"id":"1","name":"Glass","iconEmoji":"LocalDrink","volumeMl":250,"hydrationFactor":1.0},{"id":"2","name":"Shaker","iconEmoji":"LocalCafe","volumeMl":600,"hydrationFactor":1.0},{"id":"3","name":"Bottle","iconEmoji":"WaterDrop","volumeMl":1500,"hydrationFactor":1.0}]"""
    }

    // Default to the upgraded JSON structure
    private val defaultSupplements = """[{"id":"1","name":"Vitamin D3","dosage":"2000 IU","category":"Supplement","timeOfUse":"Morning","recurrenceType":"DAILY","recurrenceValue":"","instruction":"With Food"},{"id":"2","name":"Vitamin C","dosage":"500mg","category":"Supplement","timeOfUse":"Anytime","recurrenceType":"DAILY","recurrenceValue":"","instruction":"None"},{"id":"3","name":"Magnesium","dosage":"200mg","category":"Supplement","timeOfUse":"Before Bed","recurrenceType":"DAILY","recurrenceValue":"","instruction":"None"},{"id":"4","name":"Zinc","dosage":"15mg","category":"Supplement","timeOfUse":"Evening","recurrenceType":"DAILY","recurrenceValue":"","instruction":"With Food"},{"id":"5","name":"Omega-3 Fish Oil","dosage":"1000mg","category":"Supplement","timeOfUse":"Morning","recurrenceType":"DAILY","recurrenceValue":"","instruction":"With Food"},{"id":"6","name":"Creatine","dosage":"5g","category":"Supplement","timeOfUse":"Anytime","recurrenceType":"DAILY","recurrenceValue":"","instruction":"With Water"},{"id":"7","name":"Whey Protein","dosage":"30g","category":"Supplement","timeOfUse":"Anytime","recurrenceType":"DAILY","recurrenceValue":"","instruction":"With Water"},{"id":"8","name":"Multivitamin","dosage":"1 pill","category":"Supplement","timeOfUse":"Morning","recurrenceType":"DAILY","recurrenceValue":"","instruction":"With Food"},{"id":"9","name":"B-Complex","dosage":"1 cap","category":"Supplement","timeOfUse":"Morning","recurrenceType":"DAILY","recurrenceValue":"","instruction":"With Food"},{"id":"10","name":"Iron","dosage":"18mg","category":"Supplement","timeOfUse":"Anytime","recurrenceType":"DAILY","recurrenceValue":"","instruction":"With Food"},{"id":"11","name":"Vitamin B12","dosage":"1000mcg","category":"Supplement","timeOfUse":"Morning","recurrenceType":"DAILY","recurrenceValue":"","instruction":"None"},{"id":"12","name":"Ashwagandha","dosage":"500mg","category":"Supplement","timeOfUse":"Evening","recurrenceType":"DAILY","recurrenceValue":"","instruction":"None"},{"id":"13","name":"Probiotics","dosage":"1 cap","category":"Supplement","timeOfUse":"Morning","recurrenceType":"DAILY","recurrenceValue":"","instruction":"Empty Stomach"}]"""
    val supplementsRosterFlow: Flow<String> = context.dataStore.data.map { it[SUPPLEMENTS_ROSTER] ?: defaultSupplements }
    // NEW: Categories Flow
    private val defaultCategories = """["Supplement", "Medication", "Pre-workout"]"""
    val supplementCategoriesFlow: Flow<String> = context.dataStore.data.map { it[SUPPLEMENT_CATEGORIES] ?: defaultCategories }
    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED] ?: true }

    suspend fun saveSupplementsRoster(json: String) {
        context.dataStore.edit { it[SUPPLEMENTS_ROSTER] = json }
    }
    suspend fun saveSupplementCategories(json: String) { context.dataStore.edit { it[SUPPLEMENT_CATEGORIES] = json } }
    suspend fun toggleHealthCardVisibility(cardName: String, isVisible: Boolean) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[HIDDEN_HEALTH_CARDS] ?: "[]"
            val currentSet = try {
                kotlinx.serialization.json.Json.decodeFromString<Set<String>>(currentJson).toMutableSet()
            } catch (e: Exception) {
                mutableSetOf()
            }

            if (isVisible) {
                currentSet.remove(cardName) // Remove from hidden list to show it
            } else {
                currentSet.add(cardName) // Add to hidden list
            }

            preferences[HIDDEN_HEALTH_CARDS] = kotlinx.serialization.json.Json.encodeToString(currentSet)
        }
    }

    suspend fun saveAppTheme(theme: String) { context.dataStore.edit { it[APP_THEME] = theme } }

    suspend fun saveBioMetrics(
        weight: Float, height: Int, age: Int, gender: String, activity: String, goal: String,
        bodyFat: Float, dietType: String, phaseWeeks: Int, isCycling: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[WEIGHT] = weight
            preferences[HEIGHT] = height
            preferences[AGE] = age
            preferences[GENDER] = gender
            preferences[ACTIVITY_LEVEL] = activity
            preferences[FITNESS_GOAL] = goal
            preferences[BODY_FAT] = bodyFat
            preferences[DIET_TYPE] = dietType
            preferences[CALORIE_CYCLING] = isCycling

            // Only reset start time if the phase duration changed significantly
            val currentWeeks = preferences[PHASE_WEEKS] ?: 12
            if (currentWeeks != phaseWeeks) {
                preferences[PHASE_WEEKS] = phaseWeeks
                preferences[PHASE_START_TIME] = System.currentTimeMillis()
            }

            // 1. Calculate BMR (Katch-McArdle if BF% is provided, otherwise Mifflin-St Jeor)
            val bmr = if (bodyFat > 0f) {
                val leanMass = weight * (1f - (bodyFat / 100f))
                370f + (21.6f * leanMass)
            } else {
                if (gender == "Male") {
                    (10f * weight) + (6.25f * height) - (5f * age) + 5f
                } else {
                    (10f * weight) + (6.25f * height) - (5f * age) - 161f
                }
            }

            // 2. Calculate TDEE
            val multiplier = when (activity) { "Sedentary" -> 1.2f; "Light" -> 1.375f; "Moderate" -> 1.55f; "Active" -> 1.725f; "Very Active" -> 1.9f; else -> 1.2f }
            var tdee = bmr * multiplier

            // 3. Goal Adjustment
            tdee += when (goal) { "Lose Weight" -> -500f; "Build Muscle" -> 300f; else -> 0f }
            val cals = tdee.toInt()

            // 4. Custom Macro Splits
            var protein = 0; var fats = 0; var carbs = 0
            when (dietType) {
                "High Protein" -> {
                    protein = (weight * 2.6f).toInt()
                    fats = (weight * 0.8f).toInt()
                    carbs = ((cals - (protein * 4) - (fats * 9)) / 4).coerceAtLeast(0)
                }
                "Low Carb" -> {
                    protein = (weight * 2.2f).toInt()
                    carbs = (weight * 1.0f).toInt() // Strict 1g per kg
                    fats = ((cals - (protein * 4) - (carbs * 4)) / 9).coerceAtLeast(0)
                }
                "Keto" -> {
                    carbs = ((cals * 0.05f) / 4).toInt() // Strict 5% carbs
                    protein = ((cals * 0.25f) / 4).toInt() // 25% protein
                    fats = ((cals - (protein * 4) - (carbs * 4)) / 9).coerceAtLeast(0) // 70% fats
                }
                else -> { // Balanced
                    protein = (weight * 2.2f).toInt()
                    fats = (weight * 0.8f).toInt()
                    carbs = ((cals - (protein * 4) - (fats * 9)) / 4).coerceAtLeast(0)
                }
            }

            preferences[TARGET_CALORIES] = cals
            preferences[TARGET_PROTEIN] = protein
            preferences[TARGET_FATS] = fats
            preferences[TARGET_CARBS] = carbs

            val isAutoWater = preferences[AUTO_WATER_GOAL] ?: true
            if (isAutoWater) preferences[WATER_DAILY_GOAL] = (weight * 35).toInt()
        }
    }

    // Add this control function:
    suspend fun setFastingState(isFasting: Boolean, startTime: Long, goalHours: Int) {
        context.dataStore.edit { prefs ->
            prefs[IS_FASTING] = isFasting
            prefs[FASTING_START_TIME] = startTime
            prefs[FASTING_GOAL_HOURS] = goalHours
        }
    }

    suspend fun saveDefaultWaterIncrement(ml: Int) { context.dataStore.edit { it[DEFAULT_WATER_ML] = ml } }
    suspend fun saveWaterDailyGoal(goalMl: Int, isAuto: Boolean) { context.dataStore.edit { it[WATER_DAILY_GOAL] = goalMl; it[AUTO_WATER_GOAL] = isAuto } }
    suspend fun saveCustomWaterContainers(json: String) { context.dataStore.edit { it[CUSTOM_WATER_CONTAINERS] = json } }
    suspend fun saveTargets(calories: Int, protein: Int, carbs: Int, fats: Int) { context.dataStore.edit { prefs -> prefs[TARGET_CALORIES] = calories; prefs[TARGET_PROTEIN] = protein; prefs[TARGET_CARBS] = carbs; prefs[TARGET_FATS] = fats } }
    suspend fun setBiometricEnabled(enabled: Boolean) { context.dataStore.edit { it[BIOMETRIC_ENABLED] = enabled } }
}