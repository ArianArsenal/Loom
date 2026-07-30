package com.example.loom.data.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class BlockType {
    TEXT, HEADING, BULLET, NUMBER, TODO, TOGGLE, CALLOUT, QUOTE, TABLE, DIVIDER, CALENDAR,
    IMAGE, AUDIO, FILE, CODE // <-- Added new media and code block types
}

@Serializable
data class SandboxBlock(
    val id: String = UUID.randomUUID().toString(),
    var type: BlockType = BlockType.TEXT,
    var content: String = "",
    var icon: String = "💡", // Customizable emoji for Callout
    var url: String = "",    // Stores file URIs for Image/Audio/File
    var isChecked: Boolean = false, // For TODO
    var isExpanded: Boolean = false, // For TOGGLE
    var tableData: List<List<String>> = emptyList(), // For TABLE (2D Array)
    var children: List<SandboxBlock> = emptyList() // For nested TOGGLE content
)

@Serializable
data class SandboxSpace(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var icon: String = "📄",
    var blocks: List<SandboxBlock> = listOf(SandboxBlock()) // Starts with one empty block
)