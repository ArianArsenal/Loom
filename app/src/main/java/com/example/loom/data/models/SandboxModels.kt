package com.example.loom.data.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class BlockType {
    TEXT, HEADING, HEADING_2, HEADING_3, HEADING_4, BULLET, NUMBER, TODO, TOGGLE, CALLOUT, QUOTE, TABLE, DIVIDER, CALENDAR,
    IMAGE, AUDIO, FILE, CODE, SPACE, LINK,
    SKETCH, KANBAN, VIDEO, PROGRESS, MATH,
    TOC // NEW: Table of Contents Block
}

@Serializable
data class CalendarChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String = "",
    var isChecked: Boolean = false
)

@Serializable
data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var type: String = "Task",
    var details: String = "",
    var checklist: List<CalendarChecklistItem> = emptyList(),
    var imageUrl: String = "",
    var frequency: String = "Once"
)

@Serializable
data class KanbanCard(
    val id: String = UUID.randomUUID().toString(),
    var text: String = "",
    var description: String = "",
    var priority: Int = 1,
    var subtasks: List<CalendarChecklistItem> = emptyList(),
    var tags: List<String> = emptyList()
)

@Serializable
data class KanbanColumn(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var cards: List<KanbanCard> = emptyList()
)

@Serializable
data class SketchPoint(val x: Float, val y: Float)

@Serializable
data class SketchStroke(
    val color: String,
    val width: Float,
    val points: List<SketchPoint>
)

@Serializable
data class TextFormatSpan(
    val start: Int = 0,
    val end: Int = 0,
    val type: String = "",
    val value: String = ""
)

@Serializable
data class SandboxBlock(
    val id: String = UUID.randomUUID().toString(),
    var type: BlockType = BlockType.TEXT,
    var content: String = "",
    var textFormats: List<TextFormatSpan> = emptyList(),
    var secondaryContent: String = "",
    var icon: String = "💡",
    var url: String = "",
    var isChecked: Boolean = false,
    var isExpanded: Boolean = false,
    var progress: Float = 0f,
    var tableData: List<List<String>> = emptyList(),
    var colWidths: List<Float> = emptyList(),
    var rowHeights: List<Float> = emptyList(),
    var calendarEvents: Map<String, List<CalendarEvent>> = emptyMap(),
    var kanbanColumns: List<KanbanColumn> = emptyList(),
    var sketchStrokes: List<SketchStroke> = emptyList(),
    var children: List<SandboxBlock> = emptyList()
)

@Serializable
data class SandboxSpace(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var icon: String = "📄",
    var bannerUrl: String = "",
    var isPinned: Boolean = false,
    var tags: List<String> = emptyList(),
    var updatedAt: Long = System.currentTimeMillis(),
    var blocks: List<SandboxBlock> = listOf(SandboxBlock(content = "\u200B"))
)