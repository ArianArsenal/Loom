package com.example.loom.ui.sandbox

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.TextFormatSpan
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val ZWSP = "\u200B"

//val BANNER_OPTIONS = listOf(
//    "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80",
//    "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=800&q=80",
//    "https://images.unsplash.com/photo-1445905595283-214159428aa9?auto=format&fit=crop&w=800&q=80",
//    "https://images.unsplash.com/photo-1534088568595-a066f410cbda?auto=format&fit=crop&w=800&q=80",
//    "https://images.unsplash.com/photo-1503803548695-c2a7b4a5b875?auto=format&fit=crop&w=800&q=80",
//    "https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?auto=format&fit=crop&w=800&q=80",
//    "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=800&q=80",
//    "https://images.unsplash.com/photo-1436891620584-47fd0e565afb?auto=format&fit=crop&w=800&q=80",
//    "https://images.unsplash.com/photo-1490730141103-6cac27aaab94?auto=format&fit=crop&w=800&q=80",
//    "https://images.unsplash.com/photo-1483728642387-6c3bdd6c93e5?auto=format&fit=crop&w=800&q=80"
//)

@Serializable
data class TableCellData(val text: String = "", val colorHex: String? = null, val isDeleted: Boolean = false)

fun parseCell(raw: String): TableCellData {
    if (raw.isBlank()) return TableCellData()
    return try { Json.decodeFromString(raw) } catch (e: Exception) { TableCellData(text = raw) }
}

fun encodeCell(cell: TableCellData): String = Json.encodeToString(cell)

// --- RICH TEXT ENGINES ---
fun buildAnnotated(text: String, formats: List<TextFormatSpan>): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        formats.forEach { f ->
            val start = f.start.coerceIn(0, text.length)
            val end = f.end.coerceIn(0, text.length)
            if (start < end) {
                when (f.type) {
                    "BOLD" -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    "ITALIC" -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    "STRIKE" -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
                    "COLOR" -> try { addStyle(SpanStyle(color = Color(android.graphics.Color.parseColor(f.value))), start, end) } catch(e:Exception){}
                    "HIGHLIGHT" -> try { addStyle(SpanStyle(background = Color(android.graphics.Color.parseColor(f.value))), start, end) } catch(e:Exception){}
                }
            }
        }
    }
}

fun adjustFormats(oldText: String, newText: String, oldFormats: List<TextFormatSpan>, selectionStart: Int): List<TextFormatSpan> {
    val diff = newText.length - oldText.length
    if (diff == 0) return oldFormats
    val changeIndex = if (diff > 0) selectionStart - diff else selectionStart
    return oldFormats.mapNotNull { format ->
        var newStart = format.start
        var newEnd = format.end
        if (format.end <= changeIndex) {
            // Unaffected
        } else if (changeIndex < format.start) {
            newStart += diff
            newEnd += diff
        } else {
            newEnd += diff
        }
        if (newStart >= newEnd || newStart < 0) null else format.copy(start = newStart, end = newEnd)
    }
}

class MathVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder()
        var isSuper = false
        var isSub = false
        for (i in text.indices) {
            val c = text[i]
            if (c == '^') {
                builder.withStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp)) { append(c.toString()) }
                isSuper = true
                isSub = false
            } else if (c == '_') {
                builder.withStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp)) { append(c.toString()) }
                isSub = true
                isSuper = false
            } else if (c == ' ') {
                builder.append(c.toString())
                isSuper = false
                isSub = false
            } else {
                if (isSuper) {
                    builder.withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.7.em)) { append(c.toString()) }
                } else if (isSub) {
                    builder.withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.7.em)) { append(c.toString()) }
                } else {
                    builder.append(c.toString())
                }
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

fun prepareBlocksForUI(blocks: List<SandboxBlock>): List<SandboxBlock> {
    if (blocks.isEmpty()) return listOf(SandboxBlock(content = ZWSP))
    return blocks.map {
        val needsZWSP = it.type in listOf(
            BlockType.TEXT, BlockType.HEADING, BlockType.HEADING_2, BlockType.HEADING_3, BlockType.HEADING_4,
            BlockType.BULLET, BlockType.NUMBER, BlockType.TODO, BlockType.QUOTE, BlockType.CALLOUT, BlockType.TOGGLE,
            BlockType.IMAGE, BlockType.AUDIO, BlockType.FILE, BlockType.SPACE, BlockType.LINK, BlockType.PROGRESS,
            BlockType.MATH, BlockType.VIDEO, BlockType.KANBAN, BlockType.SKETCH
        )
        val newContent = if (needsZWSP && !it.content.startsWith(ZWSP)) ZWSP + it.content else it.content
        it.copy(content = newContent, children = prepareBlocksForUI(it.children).takeIf { c -> c.isNotEmpty() } ?: emptyList())
    }
}

fun cleanBlocksForDB(blocks: List<SandboxBlock>): List<SandboxBlock> {
    return blocks.map {
        it.copy(content = it.content.replace(ZWSP, ""), children = cleanBlocksForDB(it.children))
    }
}