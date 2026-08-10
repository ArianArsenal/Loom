package com.example.loom.utils

import android.content.Context
import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class SpaceTemplate(
    val id: String,
    val name: String,
    val icon: String,
    val blocksJson: String,
    val isAppDefault: Boolean
)

object TemplateManager {
    private const val PREFS_NAME = "loom_templates"
    private const val KEY_CUSTOM_TEMPLATES = "custom_templates"
    private const val KEY_TRASHED_CUSTOM_TEMPLATES = "trashed_custom_templates"
    private val gson = Gson()

    val appTemplates = listOf(
        SpaceTemplate(
            id = "empty",
            name = "Empty Space",
            icon = "📄",
            blocksJson = "[]",
            isAppDefault = true
        ),
        SpaceTemplate(
            id = "workout",
            name = "Home Workout Protocol",
            icon = "💪",
            blocksJson = gson.toJson(listOf(
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING_2, content = "\u200BToday's Split"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TIMER, content = "\u200B"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING_3, content = "\u200BExercises"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TODO, content = "\u200BPull-ups (3x8)"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TODO, content = "\u200BAdjustable Dumbbell Press (4x10)"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TODO, content = "\u200BBodyweight Dips (3x12)")
            )),
            isAppDefault = true
        ),
        SpaceTemplate(
            id = "study_dash",
            name = "Study Dashboard",
            icon = "🎓",
            blocksJson = gson.toJson(listOf(
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING_2, content = "\u200BCoursework & Quera Assignments"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TODO, content = "\u200BComplete algorithmic problem set"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TIMER, content = "\u200B"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING_3, content = "\u200BThesis Draft Notes"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.BULLET, content = "\u200BLiterature review on automated LLM testing tools"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.BULLET, content = "\u200BRefactoring proposal outline")
            )),
            isAppDefault = true
        ),
        SpaceTemplate(
            id = "skincare",
            name = "AM/PM Skincare Cycle",
            icon = "✨",
            blocksJson = gson.toJson(listOf(
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING_2, content = "\u200BMorning Routine"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TODO, content = "\u200BHyaluronic Acid"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TODO, content = "\u200BVitamin C Serum"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TODO, content = "\u200BSunscreen"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.DIVIDER, content = "\u200B"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING_2, content = "\u200BEvening Routine"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TODO, content = "\u200BNiacinamide"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TODO, content = "\u200BMoisturizer")
            )),
            isAppDefault = true
        )
    )

    // --- APP TEMPLATES LOGIC ---
    fun getActiveAppTemplates(context: Context): List<SpaceTemplate> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deletedIds = prefs.getStringSet("deleted_app_templates", emptySet()) ?: emptySet()
        val permDeletedIds = prefs.getStringSet("perm_deleted_app_templates", emptySet()) ?: emptySet()
        // ALWAYS keep "empty" safe!
        return appTemplates.filter { it.id == "empty" || (it.id !in deletedIds && it.id !in permDeletedIds) }
    }

    fun getTrashedAppTemplates(context: Context): List<SpaceTemplate> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deletedIds = prefs.getStringSet("deleted_app_templates", emptySet()) ?: emptySet()
        val permDeletedIds = prefs.getStringSet("perm_deleted_app_templates", emptySet()) ?: emptySet()
        return appTemplates.filter { it.id in deletedIds && it.id !in permDeletedIds }
    }

    fun deleteAppTemplate(context: Context, templateId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deletedIds = prefs.getStringSet("deleted_app_templates", emptySet())?.toMutableSet() ?: mutableSetOf()
        deletedIds.add(templateId)
        prefs.edit().putStringSet("deleted_app_templates", deletedIds).apply()
    }

    fun restoreAppTemplate(context: Context, templateId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deletedIds = prefs.getStringSet("deleted_app_templates", emptySet())?.toMutableSet() ?: mutableSetOf()
        deletedIds.remove(templateId)
        prefs.edit().putStringSet("deleted_app_templates", deletedIds).apply()
    }

    fun permanentlyDeleteAppTemplate(context: Context, templateId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val permDeletedIds = prefs.getStringSet("perm_deleted_app_templates", emptySet())?.toMutableSet() ?: mutableSetOf()
        permDeletedIds.add(templateId)
        prefs.edit().putStringSet("perm_deleted_app_templates", permDeletedIds).apply()
    }

    // --- CUSTOM TEMPLATES LOGIC ---
    fun getCustomTemplates(context: Context): List<SpaceTemplate> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CUSTOM_TEMPLATES, "[]") ?: "[]"
        val type = object : TypeToken<List<SpaceTemplate>>() {}.type
        return try { gson.fromJson(jsonStr, type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    fun getTrashedCustomTemplates(context: Context): List<SpaceTemplate> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_TRASHED_CUSTOM_TEMPLATES, "[]") ?: "[]"
        val type = object : TypeToken<List<SpaceTemplate>>() {}.type
        return try { gson.fromJson(jsonStr, type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    fun saveCustomTemplate(context: Context, name: String, icon: String, blocks: List<SandboxBlock>) {
        val current = getCustomTemplates(context).toMutableList()
        val cleanBlocks = blocks.map { it.copy(id = UUID.randomUUID().toString()) }

        val newTemplate = SpaceTemplate(
            id = UUID.randomUUID().toString(),
            name = name,
            icon = icon.ifEmpty { "📁" },
            blocksJson = gson.toJson(cleanBlocks),
            isAppDefault = false
        )
        current.add(newTemplate)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_CUSTOM_TEMPLATES, gson.toJson(current)).apply()
    }

    fun deleteCustomTemplate(context: Context, templateName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val currentActive = getCustomTemplates(context).toMutableList()
        val toTrash = currentActive.find { it.name == templateName } ?: return

        currentActive.remove(toTrash)
        prefs.edit().putString(KEY_CUSTOM_TEMPLATES, gson.toJson(currentActive)).apply()

        val currentTrashed = getTrashedCustomTemplates(context).toMutableList()
        currentTrashed.add(toTrash)
        prefs.edit().putString(KEY_TRASHED_CUSTOM_TEMPLATES, gson.toJson(currentTrashed)).apply()
    }

    fun restoreCustomTemplate(context: Context, templateName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val currentTrashed = getTrashedCustomTemplates(context).toMutableList()
        val toRestore = currentTrashed.find { it.name == templateName } ?: return

        currentTrashed.remove(toRestore)
        prefs.edit().putString(KEY_TRASHED_CUSTOM_TEMPLATES, gson.toJson(currentTrashed)).apply()

        val currentActive = getCustomTemplates(context).toMutableList()
        currentActive.add(toRestore)
        prefs.edit().putString(KEY_CUSTOM_TEMPLATES, gson.toJson(currentActive)).apply()
    }

    fun permanentlyDeleteCustomTemplate(context: Context, templateName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTrashed = getTrashedCustomTemplates(context).toMutableList()
        currentTrashed.removeAll { it.name == templateName }
        prefs.edit().putString(KEY_TRASHED_CUSTOM_TEMPLATES, gson.toJson(currentTrashed)).apply()
    }

    // --- EMPTY ALL TEMPLATE TRASH ---
    fun emptyTemplateTrash(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val deletedIds = prefs.getStringSet("deleted_app_templates", emptySet()) ?: emptySet()
        val permDeletedIds = prefs.getStringSet("perm_deleted_app_templates", emptySet())?.toMutableSet() ?: mutableSetOf()
        permDeletedIds.addAll(deletedIds)

        prefs.edit()
            .putStringSet("perm_deleted_app_templates", permDeletedIds) // Lock out app defaults forever
            .putStringSet("deleted_app_templates", emptySet()) // Clear soft delete
            .putString(KEY_TRASHED_CUSTOM_TEMPLATES, "[]") // Wipe custom trash
            .apply()
    }
}