package com.example.loom.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Creates the DataStore instance
private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferences @Inject constructor(private val context: Context) {

    companion object {
        val TARGET_CALORIES = intPreferencesKey("target_calories")
        val TARGET_PROTEIN = intPreferencesKey("target_protein")
    }

    // Read the values (with fallbacks if they haven't been set yet)
    val targetCaloriesFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TARGET_CALORIES] ?: 2800
    }

    val targetProteinFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TARGET_PROTEIN] ?: 140
    }

    // Update the values
    suspend fun saveTargets(calories: Int, protein: Int) {
        context.dataStore.edit { preferences ->
            preferences[TARGET_CALORIES] = calories
            preferences[TARGET_PROTEIN] = protein
        }
    }
}