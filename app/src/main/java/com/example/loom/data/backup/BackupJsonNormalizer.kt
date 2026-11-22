package com.example.loom.data.backup

import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.data.local.entity.DiaryNote
import com.example.loom.data.local.entity.Exercise
import com.example.loom.data.local.entity.ExerciseTemplate
import com.example.loom.data.local.entity.FoodItem
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.data.local.entity.PresetExercise
import com.example.loom.data.local.entity.SkincareProduct
import com.example.loom.data.local.entity.SkincareRoutine
import com.example.loom.data.local.entity.Task
import com.example.loom.data.local.entity.TaskSection
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Repairs backup JSON before it is bound to classes, so backups written by older (or newer)
 * versions of the app import cleanly:
 * - Release builds before the R8 keep rules wrote workout-preset exercises with obfuscated keys
 *   ("a".."g"); they are mapped back to the real field names.
 * - Gson ignores Kotlin defaults for classes without a no-arg constructor, so a field missing from
 *   an older backup would arrive as null and crash later. Missing fields are filled with the
 *   type's defaults.
 * - Records missing an identifying field (e.g. a daily record without its date) are dropped and
 *   counted instead of failing the whole import.
 */
object BackupJsonNormalizer {

    data class Result(val backup: JsonObject, val skippedItems: Int)

    private val gson = Gson()

    // Declaration order of PresetExercise; R8 assigned a, b, c... in this order
    private val presetExerciseFields = listOf("name", "sets", "reps", "weight", "weightListJson", "isTimeBased", "durationInSeconds")

    private class ListSpec(val key: String, val defaults: JsonObject, val required: List<String>)

    private val listSpecs: List<ListSpec> by lazy {
        listOf(
            ListSpec("dailyRecords", defaultsOf(DailyRecord(dateId = "")), listOf("dateId")),
            ListSpec("tasks", defaultsOf(Task(title = "", targetDate = "")), listOf("title")),
            ListSpec("taskSections", defaultsOf(TaskSection(name = "")), listOf("name")),
            ListSpec("exercises", defaultsOf(Exercise(dateId = "", exerciseName = "")), listOf("dateId", "exerciseName")),
            ListSpec("exerciseTemplates", defaultsOf(ExerciseTemplate(name = "")), listOf("name")),
            ListSpec("diaryNotes", defaultsOf(DiaryNote(title = "", content = "")), emptyList()),
            ListSpec("noteTags", defaultsOf(NoteTag(name = "", colorHex = "#888888")), listOf("name")),
            ListSpec("skincareProducts", defaultsOf(SkincareProduct(name = "")), listOf("name")),
            ListSpec("skincareRoutines", defaultsOf(SkincareRoutine(name = "")), listOf("name")),
            ListSpec("foodItems", defaultsOf(FoodItem(name = "", servingUnit = "", calories = 0f, protein = 0f, carbs = 0f, fats = 0f)), listOf("name"))
        )
    }

    private val presetExerciseDefaults: JsonObject by lazy { defaultsOf(PresetExercise(name = "", sets = 0, reps = 0, weight = 0f)) }

    private fun defaultsOf(value: Any): JsonObject = gson.toJsonTree(value).asJsonObject

    fun normalize(root: JsonObject): Result {
        var skipped = 0

        listSpecs.forEach { spec ->
            val list = root.arrayOrNull(spec.key) ?: return@forEach
            skipped += normalizeArray(list, spec.defaults, spec.required)
        }

        root.arrayOrNull("workoutPresets")?.let { presets ->
            val keep = JsonArray()
            presets.forEach { element ->
                val preset = element.objectOrNull()
                if (preset == null || preset.isMissing("presetName")) { skipped++; return@forEach }
                val exercises = preset.arrayOrNull("exercises") ?: JsonArray().also { preset.add("exercises", it) }
                exercises.forEach { ex -> ex.objectOrNull()?.let { deobfuscatePresetExercise(it) } }
                skipped += normalizeArray(exercises, presetExerciseDefaults, listOf("name"))
                keep.add(preset)
            }
            root.add("workoutPresets", keep)
        }

        root.arrayOrNull("notionSpaces")?.let { spaces ->
            val keep = JsonArray()
            spaces.forEach { element ->
                val space = element.objectOrNull()
                if (space == null || space.isMissing("id")) { skipped++; return@forEach }
                if (space.isMissing("title")) space.addProperty("title", "")
                if (space.isMissing("icon")) space.addProperty("icon", "📄")
                keep.add(space)
            }
            root.add("notionSpaces", keep)
        }

        return Result(root, skipped)
    }

    /** Maps obfuscated keys ("a".."g") back to PresetExercise field names when the real ones are absent. */
    private fun deobfuscatePresetExercise(obj: JsonObject) {
        if (obj.has("name") || !obj.has("a")) return
        presetExerciseFields.forEachIndexed { index, field ->
            val obfuscated = ('a' + index).toString()
            obj.get(obfuscated)?.let { value ->
                obj.remove(obfuscated)
                obj.add(field, value)
            }
        }
    }

    /** Fills missing fields from [defaults] and removes records lacking a [required] field; returns how many were removed. */
    private fun normalizeArray(array: JsonArray, defaults: JsonObject, required: List<String>): Int {
        val keep = JsonArray()
        var removed = 0
        array.forEach { element ->
            val obj = element.objectOrNull()
            if (obj == null || required.any { obj.isMissing(it) }) {
                removed++
                return@forEach
            }
            defaults.entrySet().forEach { (key, value) -> if (obj.isMissing(key)) obj.add(key, value.deepCopy()) }
            keep.add(obj)
        }
        // Replace contents in place so the parent keeps referencing the same array
        while (array.size() > 0) array.remove(0)
        keep.forEach { array.add(it) }
        return removed
    }

    private fun JsonObject.isMissing(key: String): Boolean = !has(key) || get(key).isJsonNull

    private fun JsonObject.arrayOrNull(key: String): JsonArray? = get(key)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonElement.objectOrNull(): JsonObject? = takeIf { it.isJsonObject }?.asJsonObject
}
