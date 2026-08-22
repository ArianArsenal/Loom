package com.example.loom.util

import android.content.Context
import androidx.annotation.Keep
import androidx.core.content.edit
import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

@Keep
data class SpaceTemplate(
    val id: String,
    val name: String,
    val icon: String,
    val blocksJson: String,
    val isAppDefault: Boolean
)

@Keep
object TemplateManager {
    private const val PREFS_NAME = "loom_templates"
    private const val KEY_CUSTOM_TEMPLATES = "custom_templates"
    private const val KEY_TRASHED_CUSTOM_TEMPLATES = "trashed_custom_templates"
    private val gson = Gson()

    @Suppress("SpellCheckingInspection")
    val appTemplates = listOf(
        SpaceTemplate(
            id = "empty",
            name = "Empty Space",
            icon = "📄",
            blocksJson = "[]",
            isAppDefault = true
        ),
        // --- NEW: Project Roadmap Template ---
        SpaceTemplate(
            id = "project_roadmap",
            name = "Project Roadmap",
            icon = "🗺️",
            blocksJson = gson.toJson(listOf(
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING, content = "\u200BProject Overview"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TEXT, content = "\u200BDefine the main objectives and scope here."),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING_4, content = "\u200BTask Board"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.KANBAN, content = "\u200B[]"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.DIVIDER, content = "\u200B"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING_2, content = "\u200BMeeting Notes"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.BULLET, content = "\u200BKickoff discussion points...")
            )),
            isAppDefault = true
        ),
        // --- NEW: Cornell Notes Template ---
        SpaceTemplate(
            id = "cornell_notes",
            name = "Cornell Notes System",
            icon = "📝",
            blocksJson = gson.toJson(listOf(
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING, content = "\u200BTopic / Objective"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING_2, content = "\u200BCues & Questions"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.BULLET, content = "\u200B"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING_2, content = "\u200BMain Notes"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.BULLET, content = "\u200B"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.DIVIDER, content = "\u200B"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING_2, content = "\u200BSummary"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TEXT, content = "\u200B")
            )),
            isAppDefault = true
        ),
        // --- NEW: Budget Dashboard Template ---
        SpaceTemplate(
            id = "finance_budget",
            name = "Budget & Finance Dashboard",
            icon = "💰",
            blocksJson = gson.toJson(listOf(
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING, content = "\u200BMonthly Budget"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TABLE, content = "\u200B[[\"Expense\",\"Amount\",\"Status\"],[\"Rent / Mortgage\",\"\",\"\"],[\"Groceries\",\"\",\"\"]]"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.HEADING_2, content = "\u200BFinancial Goals"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TODO, content = "\u200BSave 20% of income"),
                SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TODO, content = "\u200BPay off remaining debt")
            )),
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

    fun getActiveAppTemplates(context: Context): List<SpaceTemplate> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deletedIds = prefs.getStringSet("deleted_app_templates", emptySet()) ?: emptySet()
        val permDeletedIds = prefs.getStringSet("perm_deleted_app_templates", emptySet()) ?: emptySet()
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
        prefs.edit { putStringSet("deleted_app_templates", deletedIds) }
    }

    fun restoreAppTemplate(context: Context, templateId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deletedIds = prefs.getStringSet("deleted_app_templates", emptySet())?.toMutableSet() ?: mutableSetOf()
        deletedIds.remove(templateId)
        prefs.edit { putStringSet("deleted_app_templates", deletedIds) }
    }

    fun permanentlyDeleteAppTemplate(context: Context, templateId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val permDeletedIds = prefs.getStringSet("perm_deleted_app_templates", emptySet())?.toMutableSet() ?: mutableSetOf()
        permDeletedIds.add(templateId)
        prefs.edit { putStringSet("perm_deleted_app_templates", permDeletedIds) }
    }

    fun getCustomTemplates(context: Context): List<SpaceTemplate> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CUSTOM_TEMPLATES, "[]") ?: "[]"

        val type = TypeToken.getParameterized(List::class.java, SpaceTemplate::class.java).type

        return try { gson.fromJson(jsonStr, type) ?: emptyList() } catch (_: Exception) { emptyList() }
    }

    fun getTrashedCustomTemplates(context: Context): List<SpaceTemplate> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_TRASHED_CUSTOM_TEMPLATES, "[]") ?: "[]"

        val type = TypeToken.getParameterized(List::class.java, SpaceTemplate::class.java).type

        return try { gson.fromJson(jsonStr, type) ?: emptyList() } catch (_: Exception) { emptyList() }
    }

    fun saveCustomTemplate(context: Context, name: String, icon: String, blocks: List<SandboxBlock>) {
        val current = getCustomTemplates(context).toMutableList()

        // Strip out sensitive IDs to make the template fully reusable
        val cleanBlocks = blocks.map { it.copy(id = UUID.randomUUID().toString()) }

        val newTemplate = SpaceTemplate(
            id = UUID.randomUUID().toString(),
            name = name,
            icon = icon.ifEmpty { "📁" },
            blocksJson = gson.toJson(cleanBlocks),
            isAppDefault = false
        )
        current.add(newTemplate)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putString(KEY_CUSTOM_TEMPLATES, gson.toJson(current)) }
    }

    fun deleteCustomTemplate(context: Context, templateName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val currentActive = getCustomTemplates(context).toMutableList()
        val toTrash = currentActive.find { it.name == templateName } ?: return

        currentActive.remove(toTrash)
        prefs.edit { putString(KEY_CUSTOM_TEMPLATES, gson.toJson(currentActive)) }
        val currentTrashed = getTrashedCustomTemplates(context).toMutableList()
        currentTrashed.add(toTrash)
        prefs.edit { putString(KEY_TRASHED_CUSTOM_TEMPLATES, gson.toJson(currentTrashed)) }
    }

    fun restoreCustomTemplate(context: Context, templateName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val currentTrashed = getTrashedCustomTemplates(context).toMutableList()
        val toRestore = currentTrashed.find { it.name == templateName } ?: return

        currentTrashed.remove(toRestore)
        prefs.edit { putString(KEY_TRASHED_CUSTOM_TEMPLATES, gson.toJson(currentTrashed)) }

        val currentActive = getCustomTemplates(context).toMutableList()
        currentActive.add(toRestore)
        prefs.edit { putString(KEY_CUSTOM_TEMPLATES, gson.toJson(currentActive)) }
    }

    fun permanentlyDeleteCustomTemplate(context: Context, templateName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTrashed = getTrashedCustomTemplates(context).toMutableList()
        currentTrashed.removeAll { it.name == templateName }
        prefs.edit { putString(KEY_TRASHED_CUSTOM_TEMPLATES, gson.toJson(currentTrashed)) }
    }

    fun emptyTemplateTrash(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val deletedIds = prefs.getStringSet("deleted_app_templates", emptySet()) ?: emptySet()
        val permDeletedIds = prefs.getStringSet("perm_deleted_app_templates", emptySet())?.toMutableSet() ?: mutableSetOf()
        permDeletedIds.addAll(deletedIds)

        prefs.edit {
            putStringSet("perm_deleted_app_templates", permDeletedIds)
            putStringSet("deleted_app_templates", emptySet())
            putString(KEY_TRASHED_CUSTOM_TEMPLATES, "[]")
        }
    }
}