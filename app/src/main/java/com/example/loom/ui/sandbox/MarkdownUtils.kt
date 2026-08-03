package com.example.loom.ui.sandbox

import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock
import com.google.gson.Gson
import java.util.UUID

fun exportSpaceToMarkdown(title: String, blocks: List<SandboxBlock>): String {
    val sb = StringBuilder()
    sb.append("# ${title.ifEmpty { "Untitled Workspace" }}\n\n")
    val gson = Gson()

    fun parseBlock(b: SandboxBlock, indent: String = "") {
        val clean = b.content.replace("\u200B", "")
        when (b.type) {
            BlockType.HEADING -> sb.append("$indent# $clean\n")
            BlockType.HEADING_2 -> sb.append("$indent## $clean\n")
            BlockType.HEADING_3 -> sb.append("$indent### $clean\n")
            BlockType.HEADING_4 -> sb.append("$indent#### $clean\n")
            BlockType.BULLET -> sb.append("$indent- $clean\n")
            BlockType.TODO -> sb.append("$indent- [${if (b.isChecked) "x" else " "}] $clean\n")
            BlockType.NUMBER -> sb.append("${indent}1. $clean\n")
            BlockType.QUOTE -> sb.append("$indent> $clean\n")
            BlockType.DIVIDER -> sb.append("$indent---\n")

            BlockType.TOGGLE -> {
                sb.append("$indent<details><summary>$clean</summary>\n")
                b.children.forEach { parseBlock(it, "$indent  ") }
                sb.append("$indent</details>\n")
            }

            // Links & Media
            BlockType.LINK -> sb.append("$indent[${clean.ifEmpty { b.url }}](${b.url})\n")
            BlockType.IMAGE -> sb.append("$indent![${clean.ifEmpty { "Image" }}](${b.url})\n")
            BlockType.VIDEO -> sb.append("$indent[Video: ${clean.ifEmpty { "Attached File" }}](${b.url})\n")
            BlockType.AUDIO -> sb.append("$indent[Audio: ${clean.ifEmpty { "Attached File" }}](${b.url})\n")
            BlockType.FILE -> sb.append("$indent[File: ${clean.ifEmpty { "Attached File" }}](${b.url})\n")

            // Standard Code Blocks
            BlockType.CODE -> sb.append("$indent```\n$clean\n$indent```\n")

            // NEW: Interactive Blocks exported as JSON inside custom Markdown code blocks
            BlockType.CHART, BlockType.KANBAN, BlockType.CALENDAR, BlockType.TABLE, BlockType.SKETCH, BlockType.MATH, BlockType.PROGRESS -> {
                val json = gson.toJson(b)
                sb.append("$indent```loom-${b.type.name}\n")
                sb.append("$indent$json\n")
                sb.append("$indent```\n")
            }

            else -> {
                if (clean.isNotBlank()) sb.append("$indent$clean\n")
            }
        }
    }

    blocks.forEach { parseBlock(it) }
    return sb.toString()
}

fun parseMarkdownToBlocks(md: String): List<SandboxBlock> {
    val blocks = mutableListOf<SandboxBlock>()
    val lines = md.lines()
    val gson = Gson()

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmedLine = line.trimStart()

        if (line.isBlank() || (line.startsWith("# ") && blocks.isEmpty())) {
            i++
            continue
        }

        // NEW: Intercept our custom JSON interactive blocks
        if (trimmedLine.startsWith("```loom-")) {
            val jsonBuilder = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                jsonBuilder.append(lines[i].trimStart())
                i++
            }
            try {
                val parsedBlock = gson.fromJson(jsonBuilder.toString(), SandboxBlock::class.java)
                // Assign a fresh UUID so we don't accidentally duplicate IDs if importing into the same space
                blocks.add(parsedBlock.copy(id = UUID.randomUUID().toString()))
            } catch (e: Exception) {
                blocks.add(SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.TEXT, content = "\u200B[Error parsing interactive block]"))
            }
            i++
            continue
        }

        // Parse Standard Multiline Code Blocks
        if (trimmedLine.startsWith("```")) {
            val codeBuilder = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeBuilder.append(lines[i]).append("\n")
                i++
            }
            val content = codeBuilder.toString().trimEnd()
            blocks.add(SandboxBlock(id = UUID.randomUUID().toString(), type = BlockType.CODE, content = "\u200B$content"))
            i++
            continue
        }

        // Parse Basic Single-Line Blocks
        var type = BlockType.TEXT
        var content = line
        var isChecked = false
        var url = ""

        if (line.startsWith("## ")) { type = BlockType.HEADING_2; content = line.drop(3) }
        else if (line.startsWith("### ")) { type = BlockType.HEADING_3; content = line.drop(4) }
        else if (line.startsWith("#### ")) { type = BlockType.HEADING_4; content = line.drop(5) }
        else if (line.startsWith("- [x]", ignoreCase = true)) { type = BlockType.TODO; content = line.drop(5).trim(); isChecked = true }
        else if (line.startsWith("- [ ]")) { type = BlockType.TODO; content = line.drop(5).trim() }
        else if (line.startsWith("- ")) { type = BlockType.BULLET; content = line.drop(2) }
        else if (line.startsWith("1. ")) { type = BlockType.NUMBER; content = line.drop(3) }
        else if (line.startsWith("> ")) { type = BlockType.QUOTE; content = line.drop(2) }
        else if (line.startsWith("---")) { type = BlockType.DIVIDER; content = "" }
        else if (line.startsWith("![") && line.contains("](") && line.endsWith(")")) {
            type = BlockType.IMAGE
            content = line.substringAfter("![").substringBefore("](")
            url = line.substringAfter("](").substringBeforeLast(")")
        } else if (line.startsWith("[") && line.contains("](") && line.endsWith(")")) {
            // Differentiate between generic Links and our custom Media exports
            val linkText = line.substringAfter("[").substringBefore("](")
            url = line.substringAfter("](").substringBeforeLast(")")
            content = linkText

            type = when {
                linkText.startsWith("Video: ") -> { content = linkText.removePrefix("Video: "); BlockType.VIDEO }
                linkText.startsWith("Audio: ") -> { content = linkText.removePrefix("Audio: "); BlockType.AUDIO }
                linkText.startsWith("File: ") -> { content = linkText.removePrefix("File: "); BlockType.FILE }
                else -> BlockType.LINK
            }
        }

        blocks.add(SandboxBlock(id = UUID.randomUUID().toString(), type = type, content = "\u200B$content", isChecked = isChecked, url = url))
        i++
    }

    return if (blocks.isEmpty()) listOf(SandboxBlock(content = "\u200B")) else blocks
}