package com.example.loom.ui.sandbox

import android.content.Context
import android.net.Uri
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
import androidx.core.graphics.toColorInt // --- FIX: Using fast KTX extension ---
import com.example.loom.data.local.entity.SandboxBlockEntity
import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.TextFormatSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json // --- FIX: Removed redundant inline qualifiers below ---
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

const val ZWSP = "\u200B"

@Serializable
data class TableCellData(
    val text: String = "",
    val colorHex: String? = null,
    val isDeleted: Boolean = false,
    val formats: List<TextFormatSpan> = emptyList()
)

fun parseCell(raw: String): TableCellData {
    if (raw.isBlank()) return TableCellData()
    return try { Json.decodeFromString(raw) } catch (_: Exception) { TableCellData(text = raw) } // --- FIX: Unused e ---
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
                    "UNDERLINE" -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                    "COLOR" -> try { addStyle(SpanStyle(color = Color(f.value.toColorInt())), start, end) } catch(_: Exception){} // --- FIX: Fast KTX & Unused e ---
                    "HIGHLIGHT" -> try { addStyle(SpanStyle(background = Color(f.value.toColorInt())), start, end) } catch(_: Exception){} // --- FIX: Fast KTX & Unused e ---
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

        // --- FIX: Converted to a clean Kotlin Range Check ---
        if (newStart !in 0 until newEnd) null else format.copy(start = newStart, end = newEnd)
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
            BlockType.MATH, BlockType.VIDEO, BlockType.KANBAN, BlockType.SKETCH, BlockType.GALLERY, BlockType.CARD
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

data class LinkMetadata(val title: String, val description: String, val imageUrl: String)

@Suppress("unused") // --- FIX: Tell compiler this is an intended network utility ---
suspend fun fetchUrlMetadata(urlString: String): LinkMetadata? = withContext(Dispatchers.IO) {
    try {
        var url = URL(urlString)
        var connection = url.openConnection() as HttpURLConnection

        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.instanceFollowRedirects = true

        var responseCode = connection.responseCode
        // --- FIX: Formatted for clean IDE range checking ---
        if (responseCode in 300..399) {
            val redirectUrl = connection.getHeaderField("Location")
            if (redirectUrl != null) {
                url = URL(redirectUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                responseCode = connection.responseCode
            }
        }

        if (responseCode != HttpURLConnection.HTTP_OK) return@withContext null

        val html = connection.inputStream.bufferedReader().use {
            val chars = CharArray(30000)
            val read = it.read(chars)
            if (read > 0) String(chars, 0, read) else ""
        }

        fun extractMetaTag(vararg names: String): String? {
            for (name in names) {
                val regex1 = """<meta[^>]+(?:property|name)=["']$name["'][^>]+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
                val regex2 = """<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["']$name["']""".toRegex(RegexOption.IGNORE_CASE)
                regex1.find(html)?.groupValues?.get(1)?.let { return it }
                regex2.find(html)?.groupValues?.get(1)?.let { return it }
            }
            return null
        }

        val titleFallback = """<title>(.*?)</title>""".toRegex(RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
        val rawTitle = extractMetaTag("og:title", "twitter:title") ?: titleFallback ?: ""
        val rawDesc = extractMetaTag("og:description", "twitter:description", "description") ?: ""
        val rawImg = extractMetaTag("og:image", "twitter:image") ?: ""

        fun decodeHtml(text: String) = text.replace("&amp;", "&").replace("&#39;", "'").replace("&quot;", "\"").replace("&#x27;", "'")

        LinkMetadata(title = decodeHtml(rawTitle), description = decodeHtml(rawDesc), imageUrl = rawImg)
    } catch (_: Exception) { null } // --- FIX: Unused e ---
}

fun copyMediaToLocal(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "media_${UUID.randomUUID()}"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        Uri.fromFile(file).toString()
    } catch (_: Exception) { null } // --- FIX: Unused e ---
}

// --- WIDGET HELPER FIXES ---

fun findPinnedCards(blocks: List<SandboxBlock>, tab: String): List<SandboxBlock> {
    val result = mutableListOf<SandboxBlock>()
    for (b in blocks) {
        if (b.type == BlockType.CARD) {
            val tabs = b.pinnedTab.split(",").map { it.trim() }
            if (tabs.contains(tab)) {
                result.add(b)
            }
        }
        if (b.children.isNotEmpty()) {
            result.addAll(findPinnedCards(b.children, tab))
        }
    }
    return result
}

fun replaceBlockRecursively(blocks: List<SandboxBlock>, updatedBlock: SandboxBlock): List<SandboxBlock> {
    return blocks.map {
        if (it.id == updatedBlock.id) updatedBlock
        else it.copy(children = replaceBlockRecursively(it.children, updatedBlock))
    }
}

fun removeBlockRecursively(blocks: List<SandboxBlock>, targetId: String): List<SandboxBlock> {
    return blocks.filter { it.id != targetId }.map {
        it.copy(children = removeBlockRecursively(it.children, targetId))
    }
}

fun flattenTree(blocks: List<SandboxBlock>, spaceId: String, parentId: String? = null): List<SandboxBlockEntity> {
    val flatList = mutableListOf<SandboxBlockEntity>()
    blocks.forEachIndexed { index, block ->
        flatList.add(
            SandboxBlockEntity(
                id = block.id, spaceId = spaceId, parentId = parentId, orderIndex = index,
                type = block.type.name, content = block.content,
                textFormatsJson = Json.encodeToString(block.textFormats), // --- FIX: Removed redundant qualifier ---
                secondaryContent = block.secondaryContent, icon = block.icon, url = block.url,
                isChecked = block.isChecked, isExpanded = block.isExpanded, progress = block.progress,
                tableDataJson = Json.encodeToString(block.tableData),
                colWidthsJson = Json.encodeToString(block.colWidths),
                rowHeightsJson = Json.encodeToString(block.rowHeights),
                calendarEventsJson = Json.encodeToString(block.calendarEvents),
                kanbanColumnsJson = Json.encodeToString(block.kanbanColumns),
                sketchStrokesJson = Json.encodeToString(block.sketchStrokes),
                galleryItemsJson = Json.encodeToString(block.galleryItems),
                pinnedTab = block.pinnedTab, isLocked = block.isLocked
            )
        )
        // Recursively flatten children
        if (block.children.isNotEmpty()) {
            flatList.addAll(flattenTree(block.children, spaceId, block.id))
        }
    }
    return flatList
}

fun buildTree(domainBlocks: List<SandboxBlock>, rawEntities: List<SandboxBlockEntity>, parentId: String? = null): List<SandboxBlock> {
    // Find all entities that belong to this parent, sort them by their physical layout order
    val childrenEntities = rawEntities.filter { it.parentId == parentId }.sortedBy { it.orderIndex }

    return childrenEntities.map { entity ->
        val domainBlock = domainBlocks.find { it.id == entity.id }!!
        domainBlock.copy(children = buildTree(domainBlocks, rawEntities, entity.id))
    }
}