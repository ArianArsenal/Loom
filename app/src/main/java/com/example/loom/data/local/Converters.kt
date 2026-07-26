package com.example.loom.data.local

import androidx.room.TypeConverter
import com.example.loom.data.local.entity.PresetExercise
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromPresetList(value: List<PresetExercise>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toPresetList(value: String): List<PresetExercise> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}