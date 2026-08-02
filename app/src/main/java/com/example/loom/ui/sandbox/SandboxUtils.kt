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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log
const val ZWSP = "\u200B"

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

data class LinkMetadata(val title: String, val description: String, val imageUrl: String)

suspend fun fetchUrlMetadata(urlString: String): LinkMetadata? = withContext(Dispatchers.IO) {
    try {
        var url = URL(urlString)
        var connection = url.openConnection() as HttpURLConnection

        // 1. Pretend to be a real browser to bypass basic bot-blockers
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.instanceFollowRedirects = true

        var responseCode = connection.responseCode

        // 2. Manually follow redirect if the server requires it
        if (responseCode in 300..399) {
            val redirectUrl = connection.getHeaderField("Location")
            if (redirectUrl != null) {
                url = URL(redirectUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                responseCode = connection.responseCode
            }
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            Log.e("LinkMetadata", "Failed to fetch: HTTP $responseCode")
            return@withContext null
        }

        // 3. Read only the first 30,000 characters (Metadata is always in the top <head> section)
        val html = connection.inputStream.bufferedReader().use {
            val chars = CharArray(30000)
            val read = it.read(chars)
            if (read > 0) String(chars, 0, read) else ""
        }

        // 4. Robust Regex to catch multiple meta tag formats and Twitter tags
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

        fun decodeHtml(text: String) = text
            .replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")

        LinkMetadata(title = decodeHtml(rawTitle), description = decodeHtml(rawDesc), imageUrl = rawImg)
    } catch (e: Exception) {
        Log.e("LinkMetadata", "Error fetching metadata: ${e.message}")
        null
    }
}