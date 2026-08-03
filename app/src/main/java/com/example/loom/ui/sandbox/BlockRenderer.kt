package com.example.loom.ui.sandbox

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.nativeCanvas
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.loom.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.util.UUID

fun extractHeadings(blocks: List<SandboxBlock>): List<SandboxBlock> {
    val result = mutableListOf<SandboxBlock>()
    for (b in blocks) {
        if (b.type in listOf(BlockType.HEADING, BlockType.HEADING_2, BlockType.HEADING_3, BlockType.HEADING_4)) result.add(b)
        if (b.children.isNotEmpty()) result.addAll(extractHeadings(b.children))
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BlockRenderer(
    block: SandboxBlock,
    numberIndex: Int,
    allSpaces: List<SandboxSpace>,
    currentSpaceId: String,
    recentTableColors: MutableList<Color>,
    focusRequesters: MutableMap<String, FocusRequester>,
    focusTargetId: String?,
    allBlocks: List<SandboxBlock>,
    onScrollToBlock: (String) -> Unit,
    onMove: (Int) -> Unit,
    onFocusRequest: (String) -> Unit,
    onScrollToTop: () -> Unit,
    onNavigateToSpace: (String) -> Unit,
    onCreateLinkedSpace: () -> String,
    onUpdate: (SandboxBlock) -> Unit,
    onSplitBlock: (String, BlockType, String) -> Unit,
    onDeleteBlock: (remainder: String, deleteEntirely: Boolean) -> Unit
) {
    val currentBlock by rememberUpdatedState(block)

    val context = LocalContext.current
    val focusRequester = focusRequesters.getOrPut(block.id) { FocusRequester() }

    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var textState by remember { mutableStateOf(TextFieldValue(buildAnnotated(block.content, block.textFormats), TextRange(block.content.length))) }
    var showMenu by remember { mutableStateOf(false) }
    var commandQuery by remember { mutableStateOf("") }
    var showBlockMenu by remember { mutableStateOf(false) }
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showHighlightColorPicker by remember { mutableStateOf(false) }

    var isProcessingEnter by remember { mutableStateOf(false) }

    val isFocused = focusTargetId == block.id

    // Force text field to instantly sync if blocks swap positions globally
    LaunchedEffect(block.content, block.textFormats) {
        if (textState.text != block.content) {
            textState = TextFieldValue(
                annotatedString = buildAnnotated(block.content, block.textFormats),
                selection = TextRange(block.content.length)
            )
        }
    }

    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // Copy it to internal storage, then save the new local URL!
            val localUrl = copyMediaToLocal(context, it)
            if (localUrl != null) onUpdate(currentBlock.copy(url = localUrl))
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val localUrl = copyMediaToLocal(context, it)
            if (localUrl != null) onUpdate(currentBlock.copy(url = localUrl))
        }
    }

    var pendingCalendarDate by remember { mutableStateOf<String?>(null) }
    val calendarImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val localUrl = copyMediaToLocal(context, it)
            if (localUrl != null) {
                pendingCalendarDate?.let { dateStr ->
                    val eventsForDay = currentBlock.calendarEvents[dateStr] ?: emptyList()
                    val newEvents = eventsForDay.toMutableList().apply { add(CalendarEvent(type = "Image", imageUrl = localUrl)) }
                    onUpdate(currentBlock.copy(calendarEvents = currentBlock.calendarEvents + (dateStr to newEvents)))
                }
            }
        }
    }

    LaunchedEffect(isFocused, block.type) {
        if (isFocused) {
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {}
            textState = textState.copy(selection = TextRange(textState.text.length))
        }
    }

    val applyFormat: (String, String) -> Unit = { type, value ->
        val selStart = textState.selection.min
        val selEnd = textState.selection.max
        val overlapping = currentBlock.textFormats.filter { it.type == type && maxOf(it.start, selStart) < minOf(it.end, selEnd) }
        val cleared = currentBlock.textFormats - overlapping.toSet()

        val newFormats = if (value.isNotEmpty()) {
            cleared + TextFormatSpan(selStart, selEnd, type, value)
        } else if (type in listOf("BOLD", "ITALIC", "STRIKE")) {
            if (overlapping.isNotEmpty()) cleared else cleared + TextFormatSpan(selStart, selEnd, type, "")
        } else {
            cleared
        }

        onUpdate(currentBlock.copy(textFormats = newFormats))
        textState = textState.copy(annotatedString = buildAnnotated(textState.text, newFormats))
    }

    val handleTextChange: (TextFieldValue) -> Unit = handleTextChange@{ newTextState ->
        val newText = newTextState.text
        val oldText = textState.text
        var newFormats = currentBlock.textFormats

        if (newText != oldText) {
            newFormats = adjustFormats(oldText, newText, currentBlock.textFormats, newTextState.selection.start)
        }

        val needsZWSP = block.type in listOf(BlockType.TEXT, BlockType.HEADING, BlockType.HEADING_2, BlockType.HEADING_3, BlockType.HEADING_4, BlockType.BULLET, BlockType.NUMBER, BlockType.TODO, BlockType.QUOTE, BlockType.CALLOUT, BlockType.TOGGLE, BlockType.IMAGE, BlockType.AUDIO, BlockType.FILE, BlockType.SPACE, BlockType.LINK, BlockType.PROGRESS, BlockType.MATH, BlockType.VIDEO, BlockType.KANBAN, BlockType.SKETCH)

        if (!needsZWSP) {
            textState = newTextState.copy(annotatedString = buildAnnotated(newText, newFormats))
            onUpdate(currentBlock.copy(content = newText, textFormats = newFormats))
        } else if (newText != block.content) {
            val cleanCurrent = newText.removePrefix(ZWSP)

            when (cleanCurrent) {
                "# " -> { textState = TextFieldValue(ZWSP, TextRange(1)); onUpdate(currentBlock.copy(type = BlockType.HEADING, content = ZWSP, textFormats = emptyList())); return@handleTextChange }
                "## " -> { textState = TextFieldValue(ZWSP, TextRange(1)); onUpdate(currentBlock.copy(type = BlockType.HEADING_2, content = ZWSP, textFormats = emptyList())); return@handleTextChange }
                "### " -> { textState = TextFieldValue(ZWSP, TextRange(1)); onUpdate(currentBlock.copy(type = BlockType.HEADING_3, content = ZWSP, textFormats = emptyList())); return@handleTextChange }
                "#### " -> { textState = TextFieldValue(ZWSP, TextRange(1)); onUpdate(currentBlock.copy(type = BlockType.HEADING_4, content = ZWSP, textFormats = emptyList())); return@handleTextChange }
                "- " -> { textState = TextFieldValue(ZWSP, TextRange(1)); onUpdate(currentBlock.copy(type = BlockType.BULLET, content = ZWSP, textFormats = emptyList())); return@handleTextChange }
                "1. " -> { textState = TextFieldValue(ZWSP, TextRange(1)); onUpdate(currentBlock.copy(type = BlockType.NUMBER, content = ZWSP, textFormats = emptyList())); return@handleTextChange }
                "[] " -> { textState = TextFieldValue(ZWSP, TextRange(1)); onUpdate(currentBlock.copy(type = BlockType.TODO, content = ZWSP, textFormats = emptyList())); return@handleTextChange }
                "> " -> { textState = TextFieldValue(ZWSP, TextRange(1)); onUpdate(currentBlock.copy(type = BlockType.QUOTE, content = ZWSP, textFormats = emptyList())); return@handleTextChange }
            }

            if (!newText.startsWith(ZWSP) || newText.isEmpty()) {
                val remainder = newText.replace(ZWSP, "")
                if (block.type != BlockType.TEXT && remainder.isEmpty()) {
                    textState = textState.copy(text = ZWSP, selection = TextRange(1))
                    onUpdate(currentBlock.copy(type = BlockType.TEXT, content = ZWSP, textFormats = emptyList()))
                    keyboardController?.show()
                } else if (block.type in listOf(BlockType.IMAGE, BlockType.AUDIO, BlockType.FILE, BlockType.SPACE, BlockType.LINK, BlockType.PROGRESS, BlockType.MATH, BlockType.VIDEO, BlockType.KANBAN, BlockType.SKETCH) && remainder.isEmpty()) {
                    onDeleteBlock("", true)
                    keyboardController?.show()
                } else {
                    onDeleteBlock(remainder, false)
                    keyboardController?.show()
                }
            }
            else if (newText.contains('\n')) {
                if (isProcessingEnter) return@handleTextChange
                isProcessingEnter = true

                val parts = newText.substring(1).split('\n', limit = 2)
                val currentLine = parts[0]
                val nextContent = ZWSP + (parts.getOrNull(1) ?: "")

                if (currentLine.isEmpty() && block.type != BlockType.TEXT && block.type != BlockType.TOGGLE) {
                    textState = newTextState.copy(text = ZWSP, selection = TextRange(1))
                    onUpdate(currentBlock.copy(type = BlockType.TEXT, content = ZWSP, textFormats = emptyList()))
                    scope.launch { delay(50); isProcessingEnter = false }
                } else {
                    val nextType = if (block.type in listOf(BlockType.TODO, BlockType.BULLET, BlockType.NUMBER)) block.type else BlockType.TEXT

                    textState = TextFieldValue(ZWSP + currentLine, TextRange((ZWSP + currentLine).length))

                    onSplitBlock(ZWSP + currentLine, nextType, nextContent)
                    scope.launch { delay(50); isProcessingEnter = false }
                }
            } else {
                isProcessingEnter = false
                textState = newTextState.copy(annotatedString = buildAnnotated(newText, newFormats))
                onUpdate(currentBlock.copy(content = newText, textFormats = newFormats))

                val slashIndex = cleanCurrent.lastIndexOf('/')
                if (slashIndex != -1 && !cleanCurrent.substring(slashIndex).contains(' ')) {
                    showMenu = true
                    commandQuery = cleanCurrent.substring(slashIndex + 1).lowercase(Locale.ROOT)
                } else {
                    showMenu = false
                    commandQuery = ""
                }
            }
        } else {
            textState = newTextState.copy(annotatedString = buildAnnotated(newText, newFormats))
        }
    }

    val commonModifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 24.dp)
        .focusRequester(focusRequester)
        .onFocusChanged { state -> if (state.isFocused) onFocusRequest(block.id) }

    val placeholderUI: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { inner ->
        Box {
            if (textState.text.replace(ZWSP, "").isEmpty() && isFocused) {
                val hint = when(block.type) {
                    BlockType.HEADING -> "Heading 1"
                    BlockType.HEADING_2 -> "Heading 2"
                    BlockType.HEADING_3 -> "Heading 3"
                    BlockType.HEADING_4 -> "Heading 4"
                    BlockType.IMAGE, BlockType.AUDIO, BlockType.FILE -> "Write a caption..."
                    BlockType.VIDEO -> "Video caption..."
                    BlockType.SPACE -> "Space Link"
                    BlockType.LINK -> "Web Link"
                    BlockType.PROGRESS -> "Task Name"
                    BlockType.MATH -> "E = mc^2"
                    BlockType.SKETCH -> "Canvas Notes"
                    BlockType.KANBAN -> "Board Name"
                    else -> "Type '/' for commands"
                }
                Text(hint, color = Color.LightGray, fontStyle = if (block.type == BlockType.MATH) FontStyle.Italic else FontStyle.Normal)
            }
            inner()
        }
    }

    val isTextBased = block.type in listOf(BlockType.TEXT, BlockType.HEADING, BlockType.HEADING_2, BlockType.HEADING_3, BlockType.HEADING_4, BlockType.BULLET, BlockType.NUMBER, BlockType.TODO, BlockType.QUOTE, BlockType.CALLOUT, BlockType.TOGGLE)

    var isDragging by remember { mutableStateOf(false) }

    val isEmptyText = block.type == BlockType.TEXT && textState.text.replace(ZWSP, "").trim().isEmpty()
    val showHandle = if (isTextBased) {
        !isEmptyText && (isFocused || isDragging)
    } else {
        true
    }

    val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp, label = "drag_elevation")
    val bgColor = if (isDragging) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent

    var rowModifier = Modifier.fillMaxWidth()
    if (isDragging) {
        rowModifier = rowModifier
            .zIndex(1f)
            .shadow(elevation, RoundedCornerShape(8.dp))
            .background(bgColor, RoundedCornerShape(8.dp))
    }

    Row(modifier = rowModifier, verticalAlignment = Alignment.Top) {

        // FIX: Included all Media & Link types and pushed them down to 22.dp
        // to perfectly center the drag handle with the inside of the Surface cards.
        val handleTopPadding = when (block.type) {
            BlockType.HEADING -> 20.dp
            BlockType.HEADING_2 -> 16.dp
            BlockType.HEADING_3 -> 12.dp
            BlockType.HEADING_4 -> 10.dp
            BlockType.IMAGE, BlockType.VIDEO, BlockType.AUDIO, BlockType.FILE, BlockType.SPACE, BlockType.LINK -> 22.dp
            BlockType.SKETCH, BlockType.KANBAN, BlockType.CALENDAR, BlockType.TABLE -> 16.dp
            BlockType.DIVIDER -> 6.dp
            else -> 6.dp
        }

        // FIX: The pointerInput is now on the Box to give a massive touch target...
        var dragOffset by remember { mutableFloatStateOf(0f) }
        Box(
            modifier = Modifier
                .width(28.dp)
                .padding(top = handleTopPadding)
                .pointerInput(block.id) {
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false; dragOffset = 0f },
                        onDragCancel = { isDragging = false; dragOffset = 0f }
                    ) { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                        if (dragOffset > 50f) {
                            onMove(1)
                            dragOffset -= 50f
                        } else if (dragOffset < -50f) {
                            onMove(-1)
                            dragOffset += 50f
                        }
                    }
                },
            contentAlignment = Alignment.TopStart
        ) {
            if (showHandle) {
                Icon(
                    Icons.Default.DragIndicator,
                    contentDescription = "Drag to reorder",
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (isTextBased) {
                val textStyle = when (block.type) {
                    BlockType.HEADING -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    BlockType.HEADING_2 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    BlockType.HEADING_3 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    BlockType.HEADING_4 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    BlockType.QUOTE -> MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp)
                    BlockType.CALLOUT -> MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF1B5E20))
                    BlockType.TOGGLE -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    BlockType.TODO -> MaterialTheme.typography.bodyLarge.copy(color = if (block.isChecked) Color.Gray else MaterialTheme.colorScheme.onBackground, textDecoration = if (block.isChecked) TextDecoration.LineThrough else null)
                    else -> MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground)
                }

                val bgColorCallout = if (block.type == BlockType.CALLOUT) Color(0xFFF2FBF5)
                else if (block.type == BlockType.QUOTE) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else Color.Transparent
                val shape = if (block.type == BlockType.CALLOUT || block.type == BlockType.QUOTE) RoundedCornerShape(12.dp) else RoundedCornerShape(0.dp)
                val shadow = if (block.type == BlockType.CALLOUT) 4.dp else 0.dp

                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = bgColorCallout, shape = shape,
                        modifier = Modifier.fillMaxWidth().padding(vertical = if (block.type == BlockType.QUOTE) 4.dp else 0.dp).shadow(shadow, shape, spotColor = if (block.type == BlockType.CALLOUT) Color(0xFF4CAF50) else Color.Black)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .then(if (block.type == BlockType.QUOTE) Modifier.height(IntrinsicSize.Min) else Modifier)
                                .padding(if (block.type == BlockType.CALLOUT) 14.dp else if (block.type == BlockType.QUOTE) 16.dp else 0.dp),
                            verticalAlignment = if (block.type == BlockType.CALLOUT) Alignment.CenterVertically else Alignment.Top
                        ) {
                            if (block.type == BlockType.QUOTE) {
                                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary)); Spacer(Modifier.width(16.dp))
                            } else if (block.type == BlockType.BULLET) {
                                Text("•", modifier = Modifier.padding(end = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            } else if (block.type == BlockType.NUMBER) {
                                Text("$numberIndex.", modifier = Modifier.padding(end = 8.dp), fontWeight = FontWeight.Bold)
                            } else if (block.type == BlockType.TODO) {
                                Icon(imageVector = if (block.isChecked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp).padding(end = 8.dp).clickable { onUpdate(currentBlock.copy(isChecked = !block.isChecked)) })
                            } else if (block.type == BlockType.CALLOUT) {
                                BasicTextField(value = block.icon, onValueChange = { if (it.length <= 4) onUpdate(currentBlock.copy(icon = it)) }, textStyle = MaterialTheme.typography.titleMedium, modifier = Modifier.width(32.dp).padding(end = 8.dp))
                            } else if (block.type == BlockType.TOGGLE) {
                                Icon(imageVector = if (block.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(24.dp).clickable { onUpdate(currentBlock.copy(isExpanded = !block.isExpanded)) })
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                BasicTextField(
                                    value = textState, onValueChange = handleTextChange,
                                    textStyle = textStyle,
                                    modifier = commonModifier.padding(start = if (block.type == BlockType.TOGGLE) 4.dp else 0.dp),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = placeholderUI
                                )
                                if (block.type == BlockType.QUOTE) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    BasicTextField(
                                        value = block.secondaryContent, onValueChange = { onUpdate(currentBlock.copy(secondaryContent = it)) },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.align(Alignment.End), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        decorationBox = { inner -> Box { if (block.secondaryContent.isEmpty()) Text("- Author", color = Color.LightGray) else Row{ Text("- "); inner() } } }
                                    )
                                }
                            }
                        }
                    }

                    if (block.type == BlockType.TOGGLE && block.isExpanded) {
                        Column(modifier = Modifier.padding(start = 28.dp, top = 4.dp)) {
                            block.children.forEachIndexed { childIndex, childBlock ->
                                val cNumberedIndex = if (childBlock.type == BlockType.NUMBER) {
                                    var cCount = 1
                                    for (i in childIndex - 1 downTo 0) {
                                        if (block.children[i].type == BlockType.NUMBER) cCount++ else break
                                    }
                                    cCount
                                } else 1

                                key(childBlock.id) {
                                    BlockRenderer(
                                        block = childBlock, numberIndex = cNumberedIndex, allSpaces = allSpaces, currentSpaceId = currentSpaceId, recentTableColors = recentTableColors,
                                        focusRequesters = focusRequesters, focusTargetId = focusTargetId,
                                        allBlocks = allBlocks,
                                        onScrollToBlock = onScrollToBlock,
                                        onMove = { offset ->
                                            val currentIndex = block.children.indexOfFirst { it.id == childBlock.id }
                                            if (currentIndex != -1) {
                                                val newIndex = currentIndex + offset
                                                if (newIndex in 0 until block.children.size) {
                                                    val newChildren = block.children.toMutableList()
                                                    val temp = newChildren[currentIndex]
                                                    newChildren[currentIndex] = newChildren[newIndex]
                                                    newChildren[newIndex] = temp
                                                    onUpdate(currentBlock.copy(children = newChildren))
                                                }
                                            }
                                        },
                                        onFocusRequest = onFocusRequest,
                                        onScrollToTop = onScrollToTop, onNavigateToSpace = onNavigateToSpace, onCreateLinkedSpace = onCreateLinkedSpace,
                                        onUpdate = { updatedChild ->
                                            val newChildren = block.children.toMutableList()
                                            val safeIndex = newChildren.indexOfFirst { it.id == updatedChild.id }
                                            if (safeIndex != -1) { newChildren[safeIndex] = updatedChild; onUpdate(currentBlock.copy(children = newChildren)) }
                                        },
                                        onSplitBlock = { currentContent, nextType, nextContent ->
                                            val newChildren = block.children.toMutableList()
                                            val safeIndex = newChildren.indexOfFirst { it.id == childBlock.id }
                                            if (safeIndex != -1) {
                                                newChildren[safeIndex] = newChildren[safeIndex].copy(content = currentContent)
                                                val newBlockId = UUID.randomUUID().toString()
                                                newChildren.add(safeIndex + 1, SandboxBlock(id = newBlockId, type = nextType, content = nextContent))
                                                onUpdate(currentBlock.copy(children = newChildren))
                                                onFocusRequest(newBlockId)
                                            }
                                        },
                                        onDeleteBlock = { remainder, deleteEntirely ->
                                            val newChildren = block.children.toMutableList()
                                            val safeIndex = newChildren.indexOfFirst { it.id == childBlock.id }
                                            if (safeIndex != -1 && safeIndex < newChildren.size) {
                                                if (deleteEntirely) {
                                                    val prevId = if (safeIndex > 0) newChildren[safeIndex - 1].id else block.id
                                                    newChildren.removeAt(safeIndex)
                                                    onUpdate(currentBlock.copy(children = newChildren))
                                                    onFocusRequest(prevId)
                                                    try { focusRequesters[prevId]?.requestFocus() } catch(e:Exception){}
                                                } else if (safeIndex > 0) {
                                                    val prevChild = newChildren[safeIndex - 1]
                                                    val isPrevTextBased = prevChild.type in listOf(BlockType.TEXT, BlockType.HEADING, BlockType.HEADING_2, BlockType.HEADING_3, BlockType.HEADING_4, BlockType.BULLET, BlockType.NUMBER, BlockType.TODO, BlockType.QUOTE, BlockType.CALLOUT, BlockType.TOGGLE)
                                                    if (isPrevTextBased) {
                                                        val prevId = prevChild.id
                                                        newChildren[safeIndex - 1] = prevChild.copy(content = prevChild.content + remainder)
                                                        newChildren.removeAt(safeIndex)
                                                        onUpdate(currentBlock.copy(children = newChildren))
                                                        onFocusRequest(prevId)
                                                        try { focusRequesters[prevId]?.requestFocus() } catch(e:Exception){}
                                                    } else {
                                                        newChildren.removeAt(safeIndex - 1)
                                                        onUpdate(currentBlock.copy(children = newChildren))
                                                    }
                                                } else if (safeIndex == 0) {
                                                    onUpdate(currentBlock.copy(content = block.content + remainder, children = newChildren.drop(1)))
                                                    onFocusRequest(block.id)
                                                }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                            if (block.children.isEmpty()) {
                                TextButton(onClick = { onUpdate(currentBlock.copy(children = listOf(SandboxBlock(content = ZWSP)))) }) { Text("+ Add inside toggle", color = Color.Gray) }
                            }
                        }
                    }
                }
            } else {
                when (block.type) {
                    BlockType.CODE -> {
                        Surface(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                BasicTextField(
                                    value = textState, onValueChange = handleTextChange,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFD4D4D4), fontFamily = FontFamily.Monospace),
                                    modifier = commonModifier.padding(16.dp).padding(end = 24.dp), cursorBrush = SolidColor(Color.White),
                                    decorationBox = { inner -> Box{ if (textState.text.isEmpty()) Text("// write code here", color = Color.Gray, fontFamily = FontFamily.Monospace); inner() } }
                                )
                                IconButton(
                                    onClick = { onDeleteBlock("", true) },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Delete Code", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    BlockType.CALENDAR -> {
                        var currentMonth by remember { mutableStateOf(YearMonth.now()) }
                        var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
                        var isCalendarEditMode by remember { mutableStateOf(false) }

                        Surface(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, "Prev") }
                                    Text("${currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, "Next") }
                                        IconButton(onClick = { showBlockMenu = true }) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                                        DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                            DropdownMenuItem(text = { Text("Delete Calendar", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                                        Text(it, style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))

                                val daysInMonth = currentMonth.lengthOfMonth()
                                val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value

                                var dayCounter = 1
                                for (row in 0..5) {
                                    if (dayCounter > daysInMonth) break
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        for (col in 1..7) {
                                            if ((row == 0 && col < firstDayOfWeek) || dayCounter > daysInMonth) {
                                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                            } else {
                                                val date = currentMonth.atDay(dayCounter)
                                                val dateStr = date.toString()
                                                val events = block.calendarEvents[dateStr] ?: emptyList()

                                                Surface(
                                                    modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp).combinedClickable(
                                                        onClick = { selectedDate = date; isCalendarEditMode = false },
                                                        onLongClick = { selectedDate = date; isCalendarEditMode = true }
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (events.isNotEmpty()) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                                                    border = if (date == LocalDate.now()) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50)) else null
                                                ) {
                                                    Column(modifier = Modifier.padding(4.dp)) {
                                                        Text(dayCounter.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (events.isNotEmpty()) Color(0xFF2E7D32) else Color.Unspecified)
                                                        if (events.isNotEmpty()) {
                                                            Text("${events.size} card${if(events.size>1)"s" else ""}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontSize = 8.sp)
                                                        }
                                                    }
                                                }
                                                dayCounter++
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedDate != null) {
                            val dateStr = selectedDate.toString()
                            val eventsForDay = block.calendarEvents[dateStr] ?: emptyList()
                            var expandedImage by remember { mutableStateOf<String?>(null) }

                            AlertDialog(
                                onDismissRequest = { selectedDate = null },
                                title = {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(dateStr, fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { isCalendarEditMode = !isCalendarEditMode }) {
                                            Icon(if(isCalendarEditMode) Icons.Outlined.Visibility else Icons.Outlined.Edit, "Toggle Mode")
                                        }
                                    }
                                },
                                text = {
                                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                                        if (eventsForDay.isEmpty()) {
                                            Text("No cards for this day.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                                        }

                                        eventsForDay.forEachIndexed { eIndex, event ->
                                            ElevatedCard(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = CardDefaults.elevatedCardColors(containerColor = if (isCalendarEditMode) MaterialTheme.colorScheme.surface else Color(0xFFF2FBF5))
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {

                                                    if (!isCalendarEditMode) {
                                                        if (event.type == "Image" && event.imageUrl.isNotEmpty()) {
                                                            AsyncImage(
                                                                model = event.imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)).clickable { expandedImage = event.imageUrl }
                                                            )
                                                            if (event.title.isNotEmpty()) Text(event.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                                                        } else {
                                                            if (event.title.isNotEmpty()) Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                                            if (event.details.isNotEmpty()) Text(event.details, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))

                                                            if (event.type == "Checklist") {
                                                                Spacer(Modifier.height(8.dp))
                                                                event.checklist.forEachIndexed { cIndex, item ->
                                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                                                        Icon(
                                                                            if(item.isChecked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                                                                            tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp).clickable {
                                                                                val newChecklist = event.checklist.toMutableList()
                                                                                newChecklist[cIndex] = item.copy(isChecked = !item.isChecked)
                                                                                val newEvents = eventsForDay.toMutableList()
                                                                                newEvents[eIndex] = event.copy(checklist = newChecklist)
                                                                                onUpdate(currentBlock.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                                                            }
                                                                        )
                                                                        Spacer(Modifier.width(8.dp))
                                                                        Text(item.text, style = MaterialTheme.typography.bodyMedium.copy(textDecoration = if (item.isChecked) TextDecoration.LineThrough else null, color = if(item.isChecked) Color.Gray else MaterialTheme.colorScheme.onSurface))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                                                                Text(event.type, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                            }
                                                            IconButton(onClick = {
                                                                val newEvents = eventsForDay.toMutableList().apply { removeAt(eIndex) }
                                                                onUpdate(currentBlock.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                                            }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "Delete", tint = MaterialTheme.colorScheme.error) }
                                                        }

                                                        OutlinedTextField(
                                                            value = event.title, onValueChange = { t ->
                                                                val newEvents = eventsForDay.toMutableList()
                                                                newEvents[eIndex] = event.copy(title = t)
                                                                onUpdate(currentBlock.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                                            },
                                                            label = { Text("Title") }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                        )

                                                        if (event.type == "Task" || event.type == "Note") {
                                                            OutlinedTextField(
                                                                value = event.details, onValueChange = { d ->
                                                                    val newEvents = eventsForDay.toMutableList()
                                                                    newEvents[eIndex] = event.copy(details = d)
                                                                    onUpdate(currentBlock.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                                                },
                                                                label = { Text("Details") }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                            )
                                                        }

                                                        if (event.type == "Checklist") {
                                                            Spacer(Modifier.height(8.dp))
                                                            event.checklist.forEachIndexed { cIndex, item ->
                                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                                                    Icon(if(item.isChecked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                                                                    Spacer(Modifier.width(8.dp))
                                                                    BasicTextField(
                                                                        value = item.text, onValueChange = { newT ->
                                                                            val newChecklist = event.checklist.toMutableList()
                                                                            newChecklist[cIndex] = item.copy(text = newT)
                                                                            val newEvents = eventsForDay.toMutableList()
                                                                            newEvents[eIndex] = event.copy(checklist = newChecklist)
                                                                            onUpdate(currentBlock.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                                                        },
                                                                        modifier = Modifier.weight(1f).border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)).padding(4.dp),
                                                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                                                                    )
                                                                    IconButton(onClick = {
                                                                        val newChecklist = event.checklist.toMutableList().apply { removeAt(cIndex) }
                                                                        val newEvents = eventsForDay.toMutableList()
                                                                        newEvents[eIndex] = event.copy(checklist = newChecklist)
                                                                        onUpdate(currentBlock.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                                                    }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Remove, "Remove", tint = Color.Red) }
                                                                }
                                                            }
                                                            TextButton(onClick = {
                                                                val newChecklist = event.checklist.toMutableList().apply { add(CalendarChecklistItem()) }
                                                                val newEvents = eventsForDay.toMutableList()
                                                                newEvents[eIndex] = event.copy(checklist = newChecklist)
                                                                onUpdate(currentBlock.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                                            }) { Text("+ Add Item") }
                                                        }

                                                        if (event.type == "Image" && event.imageUrl.isNotEmpty()) {
                                                            AsyncImage(
                                                                model = event.imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                                                modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).clickable { expandedImage = event.imageUrl }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (isCalendarEditMode) {
                                            var showAddMenu by remember { mutableStateOf(false) }
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                Button(onClick = { showAddMenu = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("+ Create Card") }
                                                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                                                    DropdownMenuItem(text = { Text("Task") }, onClick = {
                                                        val newEvents = eventsForDay.toMutableList().apply { add(CalendarEvent(type = "Task")) }
                                                        onUpdate(currentBlock.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                                        showAddMenu = false
                                                    })
                                                    DropdownMenuItem(text = { Text("Checklist") }, onClick = {
                                                        val newEvents = eventsForDay.toMutableList().apply { add(CalendarEvent(type = "Checklist", checklist = listOf(CalendarChecklistItem()))) }
                                                        onUpdate(currentBlock.copy(calendarEvents = block.calendarEvents + (dateStr to newEvents)))
                                                        showAddMenu = false
                                                    })
                                                    DropdownMenuItem(text = { Text("Image") }, onClick = {
                                                        pendingCalendarDate = dateStr
                                                        calendarImageLauncher.launch("image/*")
                                                        showAddMenu = false
                                                    })
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = { TextButton(onClick = { selectedDate = null }) { Text("Close") } }
                            )

                            if (expandedImage != null) {
                                AlertDialog(
                                    onDismissRequest = { expandedImage = null },
                                    text = { AsyncImage(model = expandedImage, contentDescription = null, modifier = Modifier.fillMaxWidth()) },
                                    confirmButton = { TextButton(onClick = { expandedImage = null }) { Text("Close") } }
                                )
                            }
                        }
                    }
                    BlockType.CHART -> {
                        var chartType by remember { mutableStateOf(block.secondaryContent.ifEmpty { "BAR" }) }

                        val rawText = textState.text.replace(ZWSP, "")
                        val chartData = rawText.lines().mapNotNull { line ->
                            val parts = line.split(":", ",").map { it.trim() }
                            if (parts.size >= 2) {
                                val value = parts[1].toFloatOrNull()
                                if (value != null) parts[0] to value else null
                            } else null
                        }

                        Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
                            // Header & Controls
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) {
                                    listOf("BAR" to "📊", "LINE" to "📈", "PIE" to "🥧").forEach { (type, icon) ->
                                        val isSelected = chartType == type
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .clickable {
                                                    chartType = type
                                                    onUpdate(currentBlock.copy(secondaryContent = type))
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(icon, fontSize = 16.sp)
                                        }
                                    }
                                }
                                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                            }

                            DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                DropdownMenuItem(text = { Text("Delete Chart", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val primaryColor = MaterialTheme.colorScheme.primary
                            // Multi-color palette for all charts
                            val chartColors = listOf(primaryColor, Color(0xFF8BC34A), Color(0xFFFF9800), Color(0xFF03A9F4), Color(0xFFE91E63), Color(0xFF9C27B0))
                            val textColor = MaterialTheme.colorScheme.onBackground.toArgb()

                            // Chart Visualization Area
                            Box(modifier = Modifier.fillMaxWidth().height(240.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.2f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                if (chartData.isEmpty()) {
                                    Text("Add data below to see the chart", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                        val maxValue = chartData.maxOf { it.second }.coerceAtLeast(1f)

                                        val labelPaint = android.graphics.Paint().apply {
                                            textSize = 32f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            color = textColor
                                        }

                                        when (chartType) {
                                            "BAR" -> {
                                                val barWidth = (size.width / (chartData.size * 2f)).coerceAtMost(120f)
                                                chartData.forEachIndexed { index, dataPoint ->
                                                    val barHeight = (dataPoint.second / maxValue) * (size.height - 80f)
                                                    val xOffset = (index * (size.width / chartData.size)) + ((size.width / chartData.size) / 2f)

                                                    drawRoundRect(
                                                        color = chartColors[index % chartColors.size],
                                                        topLeft = Offset(xOffset - (barWidth / 2f), size.height - 40f - barHeight),
                                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                                    )

                                                    // Item Label under the bar
                                                    drawContext.canvas.nativeCanvas.drawText(
                                                        dataPoint.first,
                                                        xOffset,
                                                        size.height - 10f,
                                                        labelPaint
                                                    )
                                                    // Count Value on top of the bar
                                                    drawContext.canvas.nativeCanvas.drawText(
                                                        if (dataPoint.second % 1 == 0f) dataPoint.second.toInt().toString() else dataPoint.second.toString(),
                                                        xOffset,
                                                        size.height - 50f - barHeight,
                                                        labelPaint
                                                    )
                                                }
                                            }
                                            "LINE" -> {
                                                val xStep = size.width / (chartData.size - 1).coerceAtLeast(1)
                                                val path = Path()
                                                chartData.forEachIndexed { index, dataPoint ->
                                                    val x = if (chartData.size == 1) size.width / 2f else index * xStep
                                                    val y = size.height - 40f - ((dataPoint.second / maxValue) * (size.height - 80f))
                                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                                    drawCircle(color = chartColors[index % chartColors.size], radius = 8.dp.toPx(), center = Offset(x, y))

                                                    // Item Label
                                                    drawContext.canvas.nativeCanvas.drawText(dataPoint.first, x, size.height - 10f, labelPaint)
                                                    // Count Value
                                                    drawContext.canvas.nativeCanvas.drawText(if (dataPoint.second % 1 == 0f) dataPoint.second.toInt().toString() else dataPoint.second.toString(), x, y - 20f, labelPaint)
                                                }
                                                if (chartData.size > 1) {
                                                    drawPath(path, color = primaryColor, style = Stroke(width = 3.dp.toPx()))
                                                }
                                            }
                                            "PIE" -> {
                                                val total = chartData.sumOf { it.second.toDouble() }.toFloat()
                                                var startAngle = -90f
                                                chartData.forEachIndexed { index, dataPoint ->
                                                    val sweepAngle = (dataPoint.second / total) * 360f
                                                    drawArc(
                                                        color = chartColors[index % chartColors.size],
                                                        startAngle = startAngle,
                                                        sweepAngle = sweepAngle,
                                                        useCenter = true
                                                    )
                                                    startAngle += sweepAngle
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // NEW UI: Data Input Field List
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                Text("Data Points", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Spacer(Modifier.height(8.dp))

                                chartData.forEachIndexed { index, data ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        // Visual color indicator linking the UI row to the chart
                                        Box(modifier = Modifier.size(12.dp).background(chartColors[index % chartColors.size], CircleShape))
                                        Spacer(Modifier.width(8.dp))

                                        // Label Field
                                        BasicTextField(
                                            value = data.first,
                                            onValueChange = { newName ->
                                                val newLines = chartData.toMutableList()
                                                newLines[index] = newName to data.second
                                                val newText = newLines.joinToString("\n") { "${it.first}: ${if (it.second % 1 == 0f) it.second.toInt() else it.second}" }
                                                textState = TextFieldValue(ZWSP + newText, TextRange((ZWSP + newText).length))
                                                onUpdate(currentBlock.copy(content = ZWSP + newText))
                                            },
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                                            modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(4.dp)).padding(8.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))

                                        // Number Value Field
                                        BasicTextField(
                                            value = if (data.second % 1 == 0f) data.second.toInt().toString() else data.second.toString(),
                                            onValueChange = { newVal ->
                                                val parsed = newVal.toFloatOrNull() ?: 0f
                                                val newLines = chartData.toMutableList()
                                                newLines[index] = data.first to parsed
                                                val newText = newLines.joinToString("\n") { "${it.first}: ${if (it.second % 1 == 0f) it.second.toInt() else it.second}" }
                                                textState = TextFieldValue(ZWSP + newText, TextRange((ZWSP + newText).length))
                                                onUpdate(currentBlock.copy(content = ZWSP + newText))
                                            },
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center),
                                            modifier = Modifier.width(60.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(4.dp)).padding(8.dp)
                                        )

                                        // Delete Row Button
                                        IconButton(onClick = {
                                            val newLines = chartData.toMutableList().apply { removeAt(index) }
                                            val newText = newLines.joinToString("\n") { "${it.first}: ${if (it.second % 1 == 0f) it.second.toInt() else it.second}" }
                                            textState = TextFieldValue(ZWSP + newText, TextRange((ZWSP + newText).length))
                                            onUpdate(currentBlock.copy(content = ZWSP + newText))
                                        }, modifier = Modifier.size(24.dp).padding(start = 8.dp)) {
                                            Icon(Icons.Default.Close, "Remove", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // Add New Item Button
                                TextButton(onClick = {
                                    val currentLines = if (rawText.isBlank()) mutableListOf() else chartData.toMutableList()
                                    currentLines.add("Item ${currentLines.size + 1}" to 10f)
                                    val newText = currentLines.joinToString("\n") { "${it.first}: ${if (it.second % 1 == 0f) it.second.toInt() else it.second}" }
                                    textState = TextFieldValue(ZWSP + newText, TextRange((ZWSP + newText).length))
                                    onUpdate(currentBlock.copy(content = ZWSP + newText))
                                }, modifier = Modifier.padding(top = 4.dp)) {
                                    Text("+ Add Data Point", color = Color.Gray)
                                }
                            }
                        }
                    }
                    BlockType.MATH -> {
                        var showMathSymbols by remember { mutableStateOf(false) }
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    Text("∑", fontSize = 24.sp, fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { showMathSymbols = true })
                                    DropdownMenu(expanded = showMathSymbols, onDismissRequest = { showMathSymbols = false }, modifier = Modifier.width(280.dp).background(MaterialTheme.colorScheme.surface)) {
                                        val symbols = listOf("∫", "∬", "√", "∞", "≈", "≠", "≤", "≥", "±", "×", "÷", "π", "θ", "α", "β", "Δ", "λ", "μ", "σ", "°")
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Math Symbols", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                                            symbols.chunked(5).forEach { rowSymbols ->
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                    rowSymbols.forEach { sym ->
                                                        Text(sym, fontSize = 20.sp, modifier = Modifier.padding(8.dp).clickable {
                                                            val start = textState.selection.start.coerceIn(0, textState.text.length)
                                                            val newContent = textState.text.substring(0, start) + sym + textState.text.substring(start)
                                                            textState = TextFieldValue(newContent, TextRange(start + sym.length))
                                                            onUpdate(currentBlock.copy(content = newContent))
                                                            showMathSymbols = false
                                                        })
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                BasicTextField(
                                    value = textState, onValueChange = handleTextChange,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 20.sp),
                                    modifier = commonModifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    visualTransformation = MathVisualTransformation(),
                                    decorationBox = placeholderUI
                                )
                            }
                        }
                    }
                    BlockType.PROGRESS -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicTextField(
                                    value = textState, onValueChange = handleTextChange,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold),
                                    modifier = commonModifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    decorationBox = placeholderUI
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("${(block.progress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                                DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                    DropdownMenuItem(text = { Text("Delete Tracker", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                                }
                            }
                            Slider(
                                value = block.progress, onValueChange = { onUpdate(currentBlock.copy(progress = it)) },
                                modifier = Modifier.fillMaxWidth().height(24.dp), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    BlockType.SKETCH -> {
                        var localStrokes by remember { mutableStateOf(block.sketchStrokes) }
                        var recentColors by remember { mutableStateOf(listOf("#000000")) }
                        var currentColor by remember { mutableStateOf("#000000") }
                        var currentWidth by remember { mutableFloatStateOf(8f) }
                        var showColorWheel by remember { mutableStateOf(false) }

                        LaunchedEffect(block.sketchStrokes) {
                            if (block.sketchStrokes.isEmpty() && localStrokes.isNotEmpty()) {
                                localStrokes = emptyList()
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                BasicTextField(
                                    value = textState, onValueChange = handleTextChange,
                                    textStyle = MaterialTheme.typography.labelLarge.copy(color = Color.Gray),
                                    modifier = commonModifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    decorationBox = placeholderUI
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    recentColors.take(4).forEach { hex ->
                                        Box(modifier = Modifier.size(24.dp).padding(2.dp).background(Color(android.graphics.Color.parseColor(hex)), CircleShape).border(if (currentColor == hex) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape).clickable { currentColor = hex })
                                    }
                                    IconButton(onClick = { showColorWheel = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Palette, "Custom Color", tint = Color.Gray, modifier = Modifier.size(20.dp)) }
                                    IconButton(onClick = { currentColor = "#FAFAFA" }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.RadioButtonUnchecked, "Eraser", tint = if (currentColor == "#FAFAFA") MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(20.dp)) }
                                    TextButton(onClick = {
                                        localStrokes = emptyList()
                                        onUpdate(currentBlock.copy(sketchStrokes = emptyList()))
                                    }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                                    IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                                    DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                        DropdownMenuItem(text = { Text("Delete Canvas", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Brush Size:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Slider(value = currentWidth, onValueChange = { currentWidth = it }, valueRange = 2f..40f, modifier = Modifier.padding(horizontal = 8.dp).weight(1f).height(24.dp))
                            }

                            var currentStroke by remember { mutableStateOf<SketchStroke?>(null) }

                            Surface(modifier = Modifier.fillMaxWidth().height(300.dp), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), color = Color(0xFFFAFAFA)) {
                                Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentStroke = SketchStroke(color = currentColor, width = currentWidth, points = listOf(SketchPoint(offset.x, offset.y)))
                                        },
                                        onDrag = { change, _ ->
                                            currentStroke = currentStroke?.let { it.copy(points = it.points + SketchPoint(change.position.x, change.position.y)) }
                                        },
                                        onDragEnd = {
                                            currentStroke?.let { stroke ->
                                                val newStrokes = localStrokes + stroke
                                                localStrokes = newStrokes
                                                onUpdate(currentBlock.copy(sketchStrokes = newStrokes))
                                                currentStroke = null
                                            }
                                        }
                                    )
                                }) {
                                    val drawStroke = { stroke: SketchStroke ->
                                        if (stroke.points.size > 1) {
                                            val path = Path().apply {
                                                moveTo(stroke.points.first().x, stroke.points.first().y)
                                                stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
                                            }
                                            drawPath(path, color = try { Color(android.graphics.Color.parseColor(stroke.color)) } catch(e:Exception){Color.Black}, style = Stroke(width = stroke.width, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                                        }
                                    }
                                    localStrokes.forEach { drawStroke(it) }
                                    currentStroke?.let { drawStroke(it) }
                                }
                            }
                        }

                        if (showColorWheel) {
                            var hue by remember { mutableFloatStateOf(210f) }
                            var saturation by remember { mutableFloatStateOf(0.9f) }
                            var value by remember { mutableFloatStateOf(0.9f) }

                            AlertDialog(
                                onDismissRequest = { showColorWheel = false },
                                title = { Text("Custom Color") },
                                text = {
                                    Column {
                                        Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(Color.hsv(hue, 1f, 1f))
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, _ ->
                                                    change.consume()
                                                    saturation = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                    value = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                                }
                                            }
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                                                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                                                drawCircle(Color.White, 6.dp.toPx(), Offset(saturation * size.width, (1f - value) * size.height), style = Stroke(2.dp.toPx()))
                                            }
                                        }
                                        Spacer(Modifier.height(16.dp))
                                        Canvas(modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(50))
                                            .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); hue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f } }
                                        ) {
                                            drawRect(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                                            drawCircle(Color.White, 12.dp.toPx(), Offset((hue / 360f) * size.width, size.height / 2), style = Stroke(3.dp.toPx()))
                                        }
                                        Spacer(Modifier.height(16.dp))
                                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                            val hex = String.format("#%06X", (0xFFFFFF and Color.hsv(hue, saturation, value).toArgb()))
                                            recentColors = (listOf(hex) + recentColors).distinct().take(4)
                                            currentColor = hex
                                            showColorWheel = false
                                        }) { Text("Use Color") }
                                    }
                                },
                                confirmButton = { TextButton(onClick = { showColorWheel = false }) { Text("Cancel") } }
                            )
                        }
                    }
                    BlockType.KANBAN -> {
                        var editingCard by remember { mutableStateOf<Triple<Int, Int, KanbanCard>?>(null) }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                BasicTextField(
                                    value = textState, onValueChange = handleTextChange,
                                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = commonModifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    decorationBox = placeholderUI
                                )
                                IconButton(onClick = { showBlockMenu = true }) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                                DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                    DropdownMenuItem(text = { Text("Delete Board", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                                }
                            }

                            val columns = if (block.kanbanColumns.isEmpty()) listOf(KanbanColumn(title = "To Do"), KanbanColumn(title = "In Progress"), KanbanColumn(title = "Done")) else block.kanbanColumns

                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                columns.forEachIndexed { colIndex, col ->
                                    Column(
                                        modifier = Modifier
                                            .width(280.dp)
                                            .fillMaxHeight()
                                            .defaultMinSize(minHeight = 150.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            BasicTextField(
                                                value = col.title, onValueChange = { t ->
                                                    val newCols = columns.toMutableList(); newCols[colIndex] = col.copy(title = t)
                                                    onUpdate(currentBlock.copy(kanbanColumns = newCols))
                                                },
                                                textStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                                                decorationBox = { inner -> Box{ if (col.title.isEmpty()) Text("COLUMN NAME", color = Color.Gray, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)); inner() } }
                                            )
                                            IconButton(onClick = {
                                                val newCols = columns.toMutableList().apply{ removeAt(colIndex) }
                                                if (newCols.isEmpty()) { onDeleteBlock("", true) } else { onUpdate(currentBlock.copy(kanbanColumns = newCols)) }
                                            }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Close, "Delete Column", tint = Color.Gray, modifier = Modifier.size(14.dp)) }
                                        }

                                        col.cards.forEachIndexed { cardIndex, card ->
                                            val priorityColor = when (card.priority) { 1 -> Color(0xFF4CAF50); 2 -> Color(0xFF8BC34A); 3 -> Color(0xFFFFEB3B); 4 -> Color(0xFFFF9800); 5 -> Color(0xFFF44336); else -> Color.Gray }
                                            ElevatedCard(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).combinedClickable(onClick = { editingCard = Triple(colIndex, cardIndex, card) }, onLongClick = { editingCard = Triple(colIndex, cardIndex, card) }),
                                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(50)).background(priorityColor)); Spacer(Modifier.width(12.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(card.text.ifBlank { "New Task" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                                            if (card.description.isNotBlank()) Text(card.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                    }

                                                    if (card.subtasks.isNotEmpty()) {
                                                        Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 4.dp)) {
                                                            card.subtasks.forEachIndexed { sIndex, subtask ->
                                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                                                                    val newSub = card.subtasks.toMutableList().apply { set(sIndex, subtask.copy(isChecked = !subtask.isChecked)) }
                                                                    val newCards = col.cards.toMutableList().apply { set(cardIndex, card.copy(subtasks = newSub)) }
                                                                    val newCols = columns.toMutableList().apply { set(colIndex, col.copy(cards = newCards)) }
                                                                    onUpdate(currentBlock.copy(kanbanColumns = newCols))
                                                                }.padding(vertical = 2.dp)) {
                                                                    Icon(if (subtask.isChecked) Icons.Outlined.CheckCircle else Icons.Outlined.Circle, null, modifier = Modifier.size(16.dp), tint = if (subtask.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                                                    Spacer(Modifier.width(8.dp))
                                                                    Text(subtask.text, style = MaterialTheme.typography.bodySmall.copy(textDecoration = if (subtask.isChecked) TextDecoration.LineThrough else TextDecoration.None), color = if(subtask.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                            if (card.tags.isNotEmpty()) {
                                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).weight(1f, fill = false)) {
                                                                    card.tags.forEach { tagName ->
                                                                        val tagHash = Math.abs(tagName.hashCode()) % 5
                                                                        val tagColor = when(tagHash) { 0->Color(0xFF2196F3); 1->Color(0xFF9C27B0); 2->Color(0xFFFF9800); 3->Color(0xFF00BCD4); else->Color(0xFFE91E63) }
                                                                        Surface(color = tagColor.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) { Text(tagName, style = MaterialTheme.typography.labelSmall, color = tagColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                                                            if (colIndex > 0) {
                                                                IconButton(onClick = {
                                                                    val newCols = columns.toMutableList()
                                                                    newCols[colIndex] = newCols[colIndex].copy(cards = newCols[colIndex].cards.toMutableList().apply { removeAt(cardIndex) })
                                                                    newCols[colIndex - 1] = newCols[colIndex - 1].copy(cards = newCols[colIndex - 1].cards + card)
                                                                    onUpdate(currentBlock.copy(kanbanColumns = newCols))
                                                                }, modifier = Modifier.size(28.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBackIos, "Move Left", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                                            }
                                                            if (colIndex < columns.size - 1) {
                                                                IconButton(onClick = {
                                                                    val newCols = columns.toMutableList()
                                                                    newCols[colIndex] = newCols[colIndex].copy(cards = newCols[colIndex].cards.toMutableList().apply { removeAt(cardIndex) })
                                                                    newCols[colIndex + 1] = newCols[colIndex + 1].copy(cards = newCols[colIndex + 1].cards + card)
                                                                    onUpdate(currentBlock.copy(kanbanColumns = newCols))
                                                                }, modifier = Modifier.size(28.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, "Move Right", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        TextButton(onClick = {
                                            val newCols = columns.toMutableList()
                                            newCols[colIndex] = newCols[colIndex].copy(cards = newCols[colIndex].cards + KanbanCard(text = "New Card"))
                                            onUpdate(currentBlock.copy(kanbanColumns = newCols))
                                        }, modifier = Modifier.fillMaxWidth()) { Text("+ Add Card", color = Color.Gray) }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .defaultMinSize(minHeight = 150.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .clickable { onUpdate(currentBlock.copy(kanbanColumns = columns + KanbanColumn(title = "New Column"))) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Add, "Add Column", tint = Color.Gray)
                                }
                            }

                            if (editingCard != null) {
                                val (cIdx, rIdx, cardToEdit) = editingCard!!
                                EditKanbanCardDialog(
                                    card = cardToEdit,
                                    onDismiss = { editingCard = null },
                                    onSave = { updatedCard ->
                                        val newCols = columns.toMutableList()
                                        val newCards = newCols[cIdx].cards.toMutableList()
                                        newCards[rIdx] = updatedCard
                                        newCols[cIdx] = newCols[cIdx].copy(cards = newCards)
                                        onUpdate(currentBlock.copy(kanbanColumns = newCols))
                                        editingCard = null
                                    },
                                    onDelete = {
                                        val newCols = columns.toMutableList()
                                        val newCards = newCols[cIdx].cards.toMutableList()
                                        newCards.removeAt(rIdx)
                                        newCols[cIdx] = newCols[cIdx].copy(cards = newCards)
                                        onUpdate(currentBlock.copy(kanbanColumns = newCols))
                                        editingCard = null
                                    }
                                )
                            }
                        }
                    }
                    BlockType.VIDEO -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (block.url.isEmpty()) {
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("▶️", fontSize = 24.sp, modifier = Modifier.padding(end = 16.dp))
                                        TextButton(onClick = { videoLauncher.launch("video/*") }) { Text("Select Video") }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = { onDeleteBlock("", true) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                                    AndroidView(
                                        factory = { ctx ->
                                            VideoView(ctx).apply {
                                                setVideoURI(Uri.parse(block.url))
                                                val mediaController = MediaController(ctx)
                                                mediaController.setAnchorView(this)
                                                setMediaController(mediaController)
                                                seekTo(1)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(onClick = { onUpdate(currentBlock.copy(url = "")) }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha=0.5f), CircleShape)) {
                                        Icon(Icons.Default.Close, "Clear", tint = Color.White)
                                    }
                                }
                            }
                            BasicTextField(
                                value = textState, onValueChange = handleTextChange,
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                modifier = commonModifier.padding(top = 4.dp, start = 4.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = placeholderUI
                            )
                        }
                    }
                    BlockType.TIMER -> {
                        var isWorkMode by remember { mutableStateOf(true) }
                        val maxTime = if (isWorkMode) 25 * 60 else 5 * 60
                        var timeLeft by remember { mutableIntStateOf(maxTime) }
                        var isRunning by remember { mutableStateOf(false) }

                        LaunchedEffect(isRunning, isWorkMode) {
                            while (isRunning && timeLeft > 0) {
                                kotlinx.coroutines.delay(1000)
                                timeLeft--
                            }
                            if (timeLeft == 0) isRunning = false
                        }

                        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Pomodoro Timer", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                            }

                            DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                DropdownMenuItem(text = { Text("Delete Timer", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).padding(4.dp)) {
                                TextButton(onClick = { isWorkMode = true; timeLeft = 25 * 60; isRunning = false }, modifier = Modifier.background(if (isWorkMode) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(50))) { Text("Work", color = if (isWorkMode) Color.White else Color.Gray) }
                                TextButton(onClick = { isWorkMode = false; timeLeft = 5 * 60; isRunning = false }, modifier = Modifier.background(if (!isWorkMode) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(50))) { Text("Break", color = if (!isWorkMode) Color.White else Color.Gray) }
                            }

                            Spacer(Modifier.height(16.dp))

                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                                CircularProgressIndicator(progress = { timeLeft.toFloat() / maxTime.toFloat() }, modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary, strokeWidth = 6.dp, trackColor = MaterialTheme.colorScheme.outlineVariant)
                                Text(String.format("%02d:%02d", timeLeft / 60, timeLeft % 60), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(Modifier.height(16.dp))

                            IconButton(onClick = { isRunning = !isRunning }, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) {
                                Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.White)
                            }
                        }
                    }
                    BlockType.SPACE -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (block.url.isEmpty()) {
                                var showSpacePicker by remember { mutableStateOf(false) }
                                Button(onClick = { showSpacePicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)) {
                                    Icon(Icons.Default.Dashboard, null); Spacer(Modifier.width(8.dp)); Text("Select or Create a Space to Link")
                                }

                                if (showSpacePicker) {
                                    AlertDialog(
                                        onDismissRequest = { showSpacePicker = false },
                                        title = { Text("Link a Space") },
                                        text = {
                                            LazyColumn {
                                                item {
                                                    TextButton(onClick = {
                                                        val newId = onCreateLinkedSpace()
                                                        onUpdate(currentBlock.copy(url = newId))
                                                        onNavigateToSpace(newId)
                                                        showSpacePicker = false
                                                    }) { Text("+ Create New Space", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                                                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                                }
                                                items(allSpaces) { s ->
                                                    Row(modifier = Modifier.fillMaxWidth().clickable {
                                                        onUpdate(currentBlock.copy(url = s.id))
                                                        showSpacePicker = false
                                                    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Text(s.icon, fontSize = 24.sp); Spacer(Modifier.width(16.dp))
                                                        Text(s.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium)
                                                        if (s.id == currentSpaceId) {
                                                            Spacer(Modifier.width(8.dp))
                                                            Text("(Current Space)", color = Color.Gray, fontSize = 12.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        confirmButton = { TextButton(onClick = { showSpacePicker = false }) { Text("Cancel") } }
                                    )
                                }
                            } else {
                                val linkedSpace = allSpaces.find { it.id == block.url }
                                if (linkedSpace != null) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            if (linkedSpace.id == currentSpaceId) onScrollToTop() else onNavigateToSpace(linkedSpace.id)
                                        },
                                        shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(linkedSpace.icon, fontSize = 24.sp); Spacer(Modifier.width(12.dp))
                                            Text(linkedSpace.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                            if (linkedSpace.id == currentSpaceId) {
                                                Text("(Current)", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                                            }
                                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            IconButton(onClick = { showBlockMenu = true }) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                                            DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                                DropdownMenuItem(text = { Text("Delete Block", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                                                DropdownMenuItem(text = { Text("Change Space") }, onClick = { onUpdate(currentBlock.copy(url = "")); showBlockMenu = false })
                                            }
                                        }
                                    }
                                } else {
                                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(12.dp))
                                            Text("Space not found or deleted.", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                                            IconButton(onClick = { onDeleteBlock("", true) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    BlockType.LINK -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (block.url.isEmpty()) {
                                OutlinedTextField(
                                    value = block.secondaryContent,
                                    onValueChange = { onUpdate(currentBlock.copy(secondaryContent = it)) },
                                    placeholder = { Text("Paste web link here (e.g. https://google.com)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val finalUrl = if (!block.secondaryContent.startsWith("http")) "https://${block.secondaryContent}" else block.secondaryContent
                                            onUpdate(currentBlock.copy(url = finalUrl))
                                        }) { Icon(Icons.Default.Check, "Save") }
                                    }
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(block.url))) } catch (e: Exception) {}
                                    },
                                    shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Public, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(block.url.removePrefix("https://").removePrefix("http://").substringBefore("/"), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(block.url, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        IconButton(onClick = { showBlockMenu = true }) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                                        DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                            DropdownMenuItem(text = { Text("Delete Link", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                                            DropdownMenuItem(text = { Text("Edit Link") }, onClick = { onUpdate(currentBlock.copy(url = "", secondaryContent = block.url)); showBlockMenu = false })
                                        }
                                    }
                                }
                            }

                            BasicTextField(
                                value = textState, onValueChange = handleTextChange,
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                modifier = commonModifier.padding(top = 4.dp, start = 4.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = placeholderUI
                            )
                        }
                    }
                    BlockType.IMAGE -> {
                        Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Image", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options") }
                            }

                            DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                            }

                            if (block.url.isEmpty()) {
                                TextButton(onClick = { mediaLauncher.launch("image/*") }) { Text("Select Image") }
                            } else {
                                coil.compose.AsyncImage(
                                    model = block.url,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(8.dp))
                                )
                            }

                            BasicTextField(
                                value = textState, onValueChange = handleTextChange, textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                modifier = commonModifier.padding(top = 8.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = placeholderUI
                            )
                        }
                    }

                    BlockType.AUDIO -> {
                        val fileName = remember(block.url) {
                            var name = "Audio Recording"
                            if (block.url.isNotEmpty()) {
                                try {
                                    val uri = android.net.Uri.parse(block.url)
                                    if (block.url.startsWith("content://")) {
                                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                            if (cursor.moveToFirst() && nameIndex != -1) name = cursor.getString(nameIndex)
                                        }
                                    } else name = uri.lastPathSegment ?: "Audio Recording"
                                } catch (e: Exception) {}
                            }
                            name
                        }

                        // NEW: Extract Album Art Cover Metadata
                        var albumArt by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                        LaunchedEffect(block.url) {
                            if (block.url.isNotEmpty()) {
                                withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val mmr = android.media.MediaMetadataRetriever()
                                        mmr.setDataSource(context, android.net.Uri.parse(block.url))
                                        val bytes = mmr.embeddedPicture
                                        if (bytes != null) albumArt = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        mmr.release()
                                    } catch (e: Exception) {}
                                }
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Audio", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options") }
                            }

                            DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                            }

                            if (block.url.isEmpty()) {
                                var isRecording by remember { mutableStateOf(false) }
                                var recorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
                                var recordingFile by remember { mutableStateOf<java.io.File?>(null) }

                                val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                                    if (isGranted) {
                                        try {
                                            val file = java.io.File(context.filesDir, "audio_${java.util.UUID.randomUUID()}.mp4")
                                            recordingFile = file
                                            val newRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) android.media.MediaRecorder(context) else @Suppress("DEPRECATION") android.media.MediaRecorder()
                                            newRecorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                                            newRecorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                                            newRecorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                                            newRecorder.setOutputFile(file.absolutePath)
                                            newRecorder.prepare()
                                            newRecorder.start()
                                            recorder = newRecorder
                                            isRecording = true
                                        } catch (e: Exception) {}
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    if (isRecording) {
                                        Button(onClick = {
                                            try { recorder?.stop(); recorder?.release() } catch(e: Exception){}
                                            recorder = null
                                            isRecording = false
                                            recordingFile?.let { onUpdate(currentBlock.copy(url = android.net.Uri.fromFile(it).toString())) }
                                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Stop Recording") }
                                    } else {
                                        Button(onClick = { audioLauncher.launch(android.Manifest.permission.RECORD_AUDIO) }) { Text("🎤 Record") }
                                        Button(onClick = { mediaLauncher.launch("audio/*") }) { Text("Upload") }
                                    }
                                }
                            } else {
                                var isPlaying by remember { mutableStateOf(false) }
                                var progress by remember { mutableFloatStateOf(0f) }
                                var currentPosition by remember { mutableIntStateOf(0) }
                                var duration by remember { mutableIntStateOf(0) }
                                var playbackSpeed by remember { mutableFloatStateOf(1f) }
                                val mediaPlayer = remember { android.media.MediaPlayer() }

                                DisposableEffect(block.url) {
                                    try {
                                        mediaPlayer.setDataSource(context, android.net.Uri.parse(block.url))
                                        mediaPlayer.prepare()
                                        duration = mediaPlayer.duration
                                    } catch (e: Exception) {}
                                    onDispose {
                                        try {
                                            if (mediaPlayer.isPlaying) mediaPlayer.stop()
                                            mediaPlayer.release()
                                        } catch (e: Exception) {}
                                    }
                                }

                                LaunchedEffect(isPlaying) {
                                    while (isPlaying) {
                                        try {
                                            currentPosition = mediaPlayer.currentPosition
                                            if (duration > 0) progress = currentPosition.toFloat() / duration.toFloat()
                                        } catch (e: Exception) {}
                                        kotlinx.coroutines.delay(100)
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    // Show Album Art if available
                                    if (albumArt != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = albumArt!!.asImageBitmap(),
                                            contentDescription = "Cover",
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(12.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fileName, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 8.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        if (isPlaying) {
                                                            mediaPlayer.pause()
                                                            isPlaying = false
                                                        } else {
                                                            if (progress >= 1f) mediaPlayer.seekTo(0)
                                                            mediaPlayer.start()
                                                            isPlaying = true
                                                            mediaPlayer.setOnCompletionListener {
                                                                isPlaying = false
                                                                progress = 1f
                                                            }
                                                        }
                                                    } catch (e: Exception) { isPlaying = false }
                                                },
                                                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                                            ) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(20.dp)) }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(String.format("%02d:%02d", currentPosition / 1000 / 60, currentPosition / 1000 % 60), style = MaterialTheme.typography.labelSmall)

                                            Slider(
                                                value = if (progress.isNaN()) 0f else progress,
                                                onValueChange = {
                                                    progress = it
                                                    val seekTo = (it * duration).toInt()
                                                    mediaPlayer.seekTo(seekTo)
                                                    currentPosition = seekTo
                                                },
                                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                            )

                                            Text(String.format("%02d:%02d", duration / 1000 / 60, duration / 1000 % 60), style = MaterialTheme.typography.labelSmall)

                                            TextButton(onClick = {
                                                playbackSpeed = when (playbackSpeed) { 1f -> 1.5f; 1.5f -> 2f; else -> 1f }
                                                if (android.os.Build.VERSION.SDK_INT >= 23) {
                                                    try { mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(playbackSpeed) } catch (e: Exception){}
                                                }
                                            }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(40.dp)) { Text("${playbackSpeed}x", style = MaterialTheme.typography.labelSmall) }
                                        }
                                    }
                                }
                            }

                            BasicTextField(
                                value = textState, onValueChange = handleTextChange, textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                modifier = commonModifier.padding(top = 8.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = placeholderUI
                            )
                        }
                    }

                    // 3. THE FILE BLOCK (Full Screen PDF Dialog + Exact MIME Types)
                    BlockType.FILE -> {
                        var pdfBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                        var isPdf by remember { mutableStateOf(false) }
                        var showPdfDialog by remember { mutableStateOf(false) }

                        val fileName = remember(block.url) {
                            var name = "Attached File"
                            if (block.url.isNotEmpty()) {
                                try {
                                    val uri = android.net.Uri.parse(block.url)
                                    if (block.url.startsWith("content://")) {
                                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                            if (cursor.moveToFirst() && nameIndex != -1) name = cursor.getString(nameIndex)
                                        }
                                    } else name = uri.lastPathSegment ?: "Attached File"
                                } catch (e: Exception) {}
                            }
                            name
                        }

                        LaunchedEffect(block.url) {
                            if (block.url.isNotEmpty()) {
                                withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val uri = android.net.Uri.parse(block.url)
                                        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                                        if (pfd != null) {
                                            val renderer = android.graphics.pdf.PdfRenderer(pfd)
                                            if (renderer.pageCount > 0) {
                                                isPdf = true
                                                val page = renderer.openPage(0)
                                                val bitmap = android.graphics.Bitmap.createBitmap(page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888)
                                                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                                page.close()
                                                pdfBitmap = bitmap
                                            }
                                            renderer.close()
                                            pfd.close()
                                        }
                                    } catch (e: Exception) { isPdf = false }
                                }
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("File Attachment", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                            }

                            DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Open External") },
                                    onClick = {
                                        try {
                                            val uri = android.net.Uri.parse(block.url)
                                            val finalUri = if (block.url.startsWith("file://")) {
                                                androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", java.io.File(java.net.URI(block.url)))
                                            } else { uri }

                                            // FIX: Get the exact MIME type so only appropriate apps are shown!
                                            val resolvedMimeType = context.contentResolver.getType(finalUri) ?: "*/*"

                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(finalUri, resolvedMimeType)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Open File"))
                                        } catch (e: Exception) {}
                                        showBlockMenu = false
                                    }
                                )
                                DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                            }

                            if (block.url.isEmpty()) {
                                TextButton(onClick = { mediaLauncher.launch("*/*") }) { Text("Select File") }
                            } else {
                                if (isPdf && pdfBitmap != null) {
                                    // PDF Thumbnail that you tap to open full screen
                                    Box(
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).clickable { showPdfDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Image(bitmap = pdfBitmap!!.asImageBitmap(), contentDescription = "PDF Preview", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                                        Box(modifier = Modifier.background(Color.Black.copy(alpha=0.6f), CircleShape).padding(8.dp)) {
                                            Icon(Icons.Default.Fullscreen, contentDescription = "Expand", tint = Color.White, modifier = Modifier.size(32.dp))
                                        }
                                    }
                                }

                                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(androidx.compose.material.icons.Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(fileName, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            BasicTextField(
                                value = textState, onValueChange = handleTextChange, textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                modifier = commonModifier.padding(top = 8.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = placeholderUI
                            )
                        }

                        // NEW: Full Screen Interactive PDF Reader
                        if (showPdfDialog) {
                            Dialog(
                                onDismissRequest = { showPdfDialog = false },
                                properties = DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Toolbar
                                        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { showPdfDialog = false }) { Icon(Icons.Default.Close, "Close") }
                                            Spacer(Modifier.width(16.dp))
                                            Text(fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }

                                        // Lazy Loading the pages dynamically
                                        var pageCount by remember { mutableIntStateOf(0) }
                                        var pdfRenderer by remember { mutableStateOf<android.graphics.pdf.PdfRenderer?>(null) }
                                        var pfd by remember { mutableStateOf<android.os.ParcelFileDescriptor?>(null) }

                                        DisposableEffect(block.url) {
                                            try {
                                                val uri = android.net.Uri.parse(block.url)
                                                val fd = context.contentResolver.openFileDescriptor(uri, "r")
                                                if (fd != null) {
                                                    pfd = fd
                                                    val renderer = android.graphics.pdf.PdfRenderer(fd)
                                                    pdfRenderer = renderer
                                                    pageCount = renderer.pageCount
                                                }
                                            } catch (e: Exception) {}
                                            onDispose {
                                                pdfRenderer?.close()
                                                pfd?.close()
                                            }
                                        }

                                        if (pageCount > 0 && pdfRenderer != null) {
                                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                items(pageCount) { index ->
                                                    var pageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

                                                    LaunchedEffect(index) {
                                                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                            try {
                                                                val page = pdfRenderer!!.openPage(index)
                                                                // Rendering at 2x resolution for crisp zoomable text
                                                                val bitmap = android.graphics.Bitmap.createBitmap(page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888)
                                                                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                                                page.close()
                                                                pageBitmap = bitmap
                                                            } catch (e: Exception) {}
                                                        }
                                                    }

                                                    if (pageBitmap != null) {
                                                        androidx.compose.foundation.Image(
                                                            bitmap = pageBitmap!!.asImageBitmap(),
                                                            contentDescription = "Page ${index + 1}",
                                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)),
                                                            contentScale = ContentScale.FillWidth
                                                        )
                                                    } else {
                                                        Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                                                            CircularProgressIndicator()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    BlockType.DIVIDER -> {
                        Box(modifier = Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
                            HorizontalDivider(modifier = Modifier.padding(end = 40.dp), color = Color.LightGray)
                            IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)) {
                                Icon(Icons.Default.MoreVert, "Options", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                DropdownMenuItem(text = { Text("Delete Divider", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                            }
                        }
                    }
                    BlockType.TABLE -> {
                        var cellOptionsTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }
                        var focusedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
                        var showTableOptions by remember { mutableStateOf(false) }

                        val colCount = block.tableData.firstOrNull()?.size ?: 3
                        val rowCount = block.tableData.size

                        val colWidths = if (block.colWidths.size == colCount) block.colWidths else List(colCount) { 120f }
                        val rowHeights = if (block.rowHeights.size == rowCount) block.rowHeights else List(rowCount) { 48f }

                        Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Box {
                                    IconButton(onClick = { showTableOptions = true }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.MoreHoriz, "Table Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    DropdownMenu(expanded = showTableOptions, onDismissRequest = { showTableOptions = false }) {
                                        DropdownMenuItem(text = { Text("Delete Table", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                Column {
                                    block.tableData.forEachIndexed { rIndex, row ->
                                        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
                                            row.forEachIndexed { cIndex, rawCell ->
                                                val cell = parseCell(rawCell)

                                                Box(modifier = Modifier.width(colWidths[cIndex].dp).height(rowHeights[rIndex].dp)) {
                                                    if (cell.isDeleted) {
                                                        Box(modifier = Modifier.fillMaxSize().padding(4.dp))
                                                    } else {
                                                        val bgColor = cell.colorHex?.let { try { Color(android.graphics.Color.parseColor(it)) } catch(e:Exception){Color.Transparent} } ?: Color.Transparent
                                                        Surface(
                                                            modifier = Modifier.fillMaxSize().border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                                            color = bgColor
                                                        ) {
                                                            Box(modifier = Modifier.fillMaxSize()) {
                                                                BasicTextField(
                                                                    value = cell.text,
                                                                    onValueChange = { newText ->
                                                                        val newData = block.tableData.map { it.toMutableList() }.toMutableList()
                                                                        newData[rIndex][cIndex] = encodeCell(cell.copy(text = newText))
                                                                        onUpdate(currentBlock.copy(tableData = newData))
                                                                    },
                                                                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).onFocusChanged { if (it.isFocused) focusedCell = rIndex to cIndex }.padding(8.dp),
                                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground)
                                                                )
                                                                if (focusedCell == (rIndex to cIndex)) {
                                                                    IconButton(onClick = { cellOptionsTarget = rIndex to cIndex }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp)) {
                                                                        Icon(Icons.Default.MoreVert, "Options", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = {
                                    val newData = block.tableData.toMutableList().apply { add(List(colCount) { encodeCell(TableCellData()) }) }
                                    val newHeights = rowHeights.toMutableList().apply { add(48f) }
                                    onUpdate(currentBlock.copy(tableData = newData, rowHeights = newHeights))
                                }) { Text("+ Add Row") }
                                TextButton(onClick = {
                                    val newData = block.tableData.map { row -> row.toMutableList().apply { add(encodeCell(TableCellData())) } }
                                    val newWidths = colWidths.toMutableList().apply { add(120f) }
                                    onUpdate(currentBlock.copy(tableData = newData, colWidths = newWidths))
                                }) { Text("+ Add Column") }
                            }
                        }

                        if (cellOptionsTarget != null) {
                            val (r, c) = cellOptionsTarget!!
                            val currentCell = parseCell(block.tableData[r][c])
                            var showColorWheel by remember { mutableStateOf(false) }

                            AlertDialog(
                                onDismissRequest = { cellOptionsTarget = null },
                                title = { Text("Cell Settings") },
                                text = {
                                    if (showColorWheel) {
                                        var hue by remember { mutableFloatStateOf(210f) }
                                        var saturation by remember { mutableFloatStateOf(0.9f) }
                                        var value by remember { mutableFloatStateOf(0.9f) }

                                        Column {
                                            Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(Color.hsv(hue, 1f, 1f))
                                                .pointerInput(Unit) {
                                                    detectDragGestures { change, _ ->
                                                        change.consume()
                                                        saturation = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                        value = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                                    }
                                                }
                                            ) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                                                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                                                    drawCircle(Color.White, 6.dp.toPx(), Offset(saturation * size.width, (1f - value) * size.height), style = Stroke(2.dp.toPx()))
                                                }
                                            }
                                            Spacer(Modifier.height(16.dp))
                                            Canvas(modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(50))
                                                .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); hue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f } }
                                            ) {
                                                drawRect(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                                                drawCircle(Color.White, 12.dp.toPx(), Offset((hue / 360f) * size.width, size.height / 2), style = Stroke(3.dp.toPx()))
                                            }
                                            Spacer(Modifier.height(16.dp))
                                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                                val pickedColor = Color.hsv(hue, saturation, value)
                                                if (!recentTableColors.contains(pickedColor)) {
                                                    recentTableColors.add(0, pickedColor)
                                                    if (recentTableColors.size > 5) recentTableColors.removeAt(recentTableColors.size - 1)
                                                }
                                                val hex = String.format("#%08X", pickedColor.toArgb())
                                                val newData = block.tableData.map { it.toMutableList() }.toMutableList()
                                                newData[r][c] = encodeCell(currentCell.copy(colorHex = hex))
                                                onUpdate(currentBlock.copy(tableData = newData))
                                                cellOptionsTarget = null
                                            }) { Text("Apply Color") }
                                        }
                                    } else {
                                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                            Text("Dimensions", style = MaterialTheme.typography.labelMedium)
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Width: ${colWidths[c].toInt()}")
                                                Row {
                                                    IconButton(onClick = { val n = colWidths.toMutableList(); n[c] = (n[c]-10f).coerceAtLeast(60f); onUpdate(currentBlock.copy(colWidths=n)) }) { Icon(Icons.Default.Remove, "") }
                                                    IconButton(onClick = { val n = colWidths.toMutableList(); n[c] += 10f; onUpdate(currentBlock.copy(colWidths=n)) }) { Icon(Icons.Default.Add, "") }
                                                }
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Height: ${rowHeights[r].toInt()}")
                                                Row {
                                                    IconButton(onClick = { val n = rowHeights.toMutableList(); n[r] = (n[r]-10f).coerceAtLeast(36f); onUpdate(currentBlock.copy(rowHeights=n)) }) { Icon(Icons.Default.Remove, "") }
                                                    IconButton(onClick = { val n = rowHeights.toMutableList(); n[r] += 10f; onUpdate(currentBlock.copy(rowHeights=n)) }) { Icon(Icons.Default.Add, "") }
                                                }
                                            }
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                            TextButton(onClick = {
                                                val newData = block.tableData.map { it.toMutableList() }.toMutableList()
                                                newData[r][c] = encodeCell(currentCell.copy(isDeleted = true))
                                                onUpdate(currentBlock.copy(tableData = newData))
                                                cellOptionsTarget = null
                                            }) { Text("Delete Cell (Leave Blank)") }
                                            TextButton(onClick = {
                                                val newData = block.tableData.toMutableList().apply { removeAt(r) }
                                                val newHeights = rowHeights.toMutableList().apply { removeAt(r) }
                                                onUpdate(currentBlock.copy(tableData = newData, rowHeights = newHeights))
                                                cellOptionsTarget = null
                                            }) { Text("Delete Entire Row") }
                                            TextButton(onClick = {
                                                val newData = block.tableData.map { it.toMutableList().apply { removeAt(c) } }
                                                val newWidths = colWidths.toMutableList().apply { removeAt(c) }
                                                onUpdate(currentBlock.copy(tableData = newData, colWidths = newWidths))
                                                cellOptionsTarget = null
                                            }) { Text("Delete Entire Column") }

                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Cell Color", style = MaterialTheme.typography.labelMedium)
                                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                val palette = listOf(Color.Transparent) + recentTableColors
                                                palette.take(6).forEach { color ->
                                                    Box(
                                                        modifier = Modifier.size(32.dp).background(color, CircleShape).border(1.dp, Color.LightGray, CircleShape)
                                                            .clickable {
                                                                val hex = if (color == Color.Transparent) null else String.format("#%08X", color.toArgb())
                                                                val newData = block.tableData.map { it.toMutableList() }.toMutableList()
                                                                newData[r][c] = encodeCell(currentCell.copy(colorHex = hex))
                                                                onUpdate(currentBlock.copy(tableData = newData))
                                                                cellOptionsTarget = null
                                                            }
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).clickable { showColorWheel = true },
                                                    contentAlignment = Alignment.Center
                                                ) { Text("+") }
                                            }
                                        }
                                    }
                                },
                                confirmButton = { TextButton(onClick = { cellOptionsTarget = null }) { Text("Close") } }
                            )
                        }
                    }
                    BlockType.TOC -> {
                        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Table of Contents", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                            }
                            DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                DropdownMenuItem(text = { Text("Delete TOC", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            val headings = extractHeadings(allBlocks)
                            if (headings.isEmpty()) {
                                Text("No headings found. Add H1, H2, H3, or H4 blocks.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            } else {
                                headings.forEach { hBlock ->
                                    val indent = when(hBlock.type) {
                                        BlockType.HEADING -> 0.dp
                                        BlockType.HEADING_2 -> 16.dp
                                        BlockType.HEADING_3 -> 32.dp
                                        BlockType.HEADING_4 -> 48.dp
                                        else -> 0.dp
                                    }
                                    val text = hBlock.content.replace(ZWSP, "").ifEmpty { "Untitled Heading" }
                                    Text(
                                        text = text,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = indent, top = 4.dp, bottom = 4.dp)
                                            .clickable { onScrollToBlock(hBlock.id) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            BasicTextField(
                                value = textState, onValueChange = handleTextChange,
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Transparent, fontSize = 1.sp),
                                modifier = commonModifier.height(1.dp), cursorBrush = SolidColor(Color.Transparent),
                                decorationBox = placeholderUI
                            )
                        }
                    }
                    BlockType.LINK -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (block.url.isEmpty()) {
                                OutlinedTextField(
                                    value = block.secondaryContent,
                                    onValueChange = { onUpdate(currentBlock.copy(secondaryContent = it)) },
                                    placeholder = { Text("Paste web link here (e.g. https://google.com)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val finalUrl = if (!block.secondaryContent.startsWith("http")) "https://${block.secondaryContent}" else block.secondaryContent
                                            onUpdate(currentBlock.copy(url = finalUrl))
                                        }) { Icon(Icons.Default.Check, "Save") }
                                    }
                                )
                            } else {
                                var metadata by remember { mutableStateOf<LinkMetadata?>(null) }
                                var isLoading by remember { mutableStateOf(true) }

                                // Fetch Open Graph data asynchronously
                                LaunchedEffect(block.url) {
                                    isLoading = true
                                    metadata = fetchUrlMetadata(block.url)
                                    isLoading = false
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(block.url))) } catch (e: Exception) {}
                                    },
                                    shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {

                                        // Left Side: Text Data
                                        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                                            if (isLoading) {
                                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), color = MaterialTheme.colorScheme.primary)
                                                Text(block.url, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            } else if (metadata != null && (metadata!!.title.isNotEmpty() || metadata!!.description.isNotEmpty())) {
                                                Text(metadata!!.title.ifEmpty { block.url }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                if (metadata!!.description.isNotEmpty()) {
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(metadata!!.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(block.url.removePrefix("https://").removePrefix("http://").substringBefore("/"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                }
                                            } else {
                                                // Fallback for sites with no metadata
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Public, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                                    Spacer(Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(block.url.removePrefix("https://").removePrefix("http://").substringBefore("/"), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text(block.url, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                            }
                                        }

                                        // Right Side: Cover Image & Options
                                        if (!isLoading && metadata?.imageUrl?.isNotEmpty() == true) {
                                            Box(modifier = Modifier.width(120.dp).fillMaxHeight()) {
                                                AsyncImage(
                                                    model = metadata!!.imageUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(MaterialTheme.colorScheme.surface.copy(alpha=0.8f), CircleShape)) {
                                                    Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        } else {
                                            Box(modifier = Modifier.padding(8.dp)) {
                                                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                            DropdownMenuItem(text = { Text("Delete Link", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                                            DropdownMenuItem(text = { Text("Edit Link") }, onClick = { onUpdate(currentBlock.copy(url = "", secondaryContent = block.url)); showBlockMenu = false })
                                        }
                                    }
                                }
                            }

                            BasicTextField(
                                value = textState, onValueChange = handleTextChange,
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                modifier = commonModifier.padding(top = 4.dp, start = 4.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = placeholderUI
                            )
                        }
                    }
                    else -> {}
                }
            }

            // --- FLOATING RICH TEXT TOOLBAR ---
            if (isFocused && textState.selection.length > 0) {
                Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, -220)) {
                    Surface(shape = RoundedCornerShape(8.dp), shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { applyFormat("BOLD", "") }, modifier = Modifier.size(32.dp)) {
                                Text("B", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { applyFormat("ITALIC", "") }, modifier = Modifier.size(32.dp)) {
                                Text("I", fontStyle = FontStyle.Italic, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { applyFormat("STRIKE", "") }, modifier = Modifier.size(32.dp)) {
                                Text("S", textDecoration = TextDecoration.LineThrough, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.LightGray))
                            Spacer(Modifier.width(8.dp))

                            IconButton(onClick = { showTextColorPicker = true }, modifier = Modifier.size(32.dp)) {
                                Text("A", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, textDecoration = TextDecoration.Underline)
                            }

                            IconButton(onClick = { showHighlightColorPicker = true }, modifier = Modifier.size(32.dp)) {
                                Text("A", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.surface, modifier = Modifier.background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp))
                            }

                            Spacer(Modifier.width(8.dp))

                            IconButton(onClick = {
                                val selStart = textState.selection.min
                                val selEnd = textState.selection.max
                                val cleared = currentBlock.textFormats.filterNot { maxOf(it.start, selStart) < minOf(it.end, selEnd) }
                                onUpdate(currentBlock.copy(textFormats = cleared))
                                textState = textState.copy(annotatedString = buildAnnotated(textState.text, cleared))
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.FormatClear, "Clear Formatting", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            if (showTextColorPicker || showHighlightColorPicker) {
                val isHighlight = showHighlightColorPicker
                var hue by remember { mutableFloatStateOf(210f) }
                var saturation by remember { mutableFloatStateOf(0.9f) }
                var value by remember { mutableFloatStateOf(0.9f) }

                AlertDialog(
                    onDismissRequest = { showTextColorPicker = false; showHighlightColorPicker = false },
                    title = { Text(if (isHighlight) "Highlight Color" else "Text Color") },
                    text = {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(Color.hsv(hue, 1f, 1f))
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ -> change.consume(); saturation = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f); value = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f) }
                                }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent))); drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                                    drawCircle(Color.White, 6.dp.toPx(), Offset(saturation * size.width, (1f - value) * size.height), style = Stroke(2.dp.toPx()))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Canvas(modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(50))
                                .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); hue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f } }
                            ) {
                                drawRect(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                                drawCircle(Color.White, 12.dp.toPx(), Offset((hue / 360f) * size.width, size.height / 2), style = Stroke(3.dp.toPx()))
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val palette = listOf(Color.Transparent) + recentTableColors
                                palette.take(6).forEach { color ->
                                    Box(
                                        modifier = Modifier.size(32.dp).background(color, CircleShape).border(1.dp, Color.LightGray, CircleShape)
                                            .clickable {
                                                if (color == Color.Transparent) {
                                                    applyFormat(if (isHighlight) "HIGHLIGHT" else "COLOR", "")
                                                } else {
                                                    val hex = String.format("#%06X", (0xFFFFFF and color.toArgb()))
                                                    applyFormat(if (isHighlight) "HIGHLIGHT" else "COLOR", hex)
                                                }
                                                showTextColorPicker = false; showHighlightColorPicker = false
                                            }
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                val pickedColor = Color.hsv(hue, saturation, value)
                                val hex = String.format("#%06X", (0xFFFFFF and pickedColor.toArgb()))
                                if (!recentTableColors.contains(pickedColor)) {
                                    recentTableColors.add(0, pickedColor)
                                    if (recentTableColors.size > 5) recentTableColors.removeAt(recentTableColors.size - 1)
                                }
                                applyFormat(if (isHighlight) "HIGHLIGHT" else "COLOR", hex)
                                showTextColorPicker = false; showHighlightColorPicker = false
                            }) { Text("Apply Color") }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showTextColorPicker = false; showHighlightColorPicker = false }) { Text("Close") } }
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.width(240.dp).heightIn(max = 350.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                properties = PopupProperties(focusable = false)
            ) {
                val applyType: (BlockType) -> Unit = { type ->
                    val cleanText = textState.text.substringBeforeLast('/')

                    textState = textState.copy(text = cleanText, selection = TextRange(cleanText.length))
                    val updated = currentBlock.copy(type = type, content = cleanText)
                    if (type == BlockType.TABLE) updated.tableData = List(3) { List(3) { encodeCell(TableCellData()) } }
                    onUpdate(updated)

                    showMenu = false
                    try { focusRequester.requestFocus() } catch(e: Exception){}
                    onFocusRequest(block.id)
                }

                val menuItems = listOf(
                    "Basic Blocks" to null,
                    BlockType.TEXT to ("Text" to "Aa"),
                    BlockType.HEADING to ("Heading 1" to "H1"),
                    BlockType.HEADING_2 to ("Heading 2" to "H2"),
                    BlockType.HEADING_3 to ("Heading 3" to "H3"),
                    BlockType.HEADING_4 to ("Heading 4" to "H4"),
                    BlockType.TODO to ("To-do List" to "☑"),
                    BlockType.BULLET to ("Bulleted List" to "•"),
                    BlockType.NUMBER to ("Numbered List" to "1."),
                    BlockType.TOGGLE to ("Toggle List" to "▶"),
                    BlockType.QUOTE to ("Quote" to "”"),
                    BlockType.DIVIDER to ("Divider" to "—"),

                    "Advanced Blocks" to null,
                    BlockType.TOC to ("Table of Contents" to "📑"),
                    BlockType.CHART to ("Chart" to "📈"),
                    BlockType.CALLOUT to ("Callout" to "💡"),
                    BlockType.TABLE to ("Table" to "⊞"),
                    BlockType.CALENDAR to ("Calendar" to "📅"),
                    BlockType.KANBAN to ("Kanban Board" to "📊"),
                    BlockType.SKETCH to ("Sketch Canvas" to "🎨"),
                    BlockType.MATH to ("Math Equation" to "🧮"),
                    BlockType.PROGRESS to ("Progress Tracker" to "⏳"),
                    BlockType.CODE to ("Code Snippet" to "⌨️"),
                    BlockType.TIMER to ("Pomodoro Timer" to "⏱️"),

                    "Media & Links" to null,
                    BlockType.IMAGE to ("Image" to "🖼️"),
                    BlockType.VIDEO to ("Video" to "▶️"),
                    BlockType.AUDIO to ("Audio" to "🎵"),
                    BlockType.FILE to ("File" to "📁"),
                    BlockType.SPACE to ("Link Space" to "🚀"),
                    BlockType.LINK to ("Web Link" to "🌐")
                )

                val filteredMenuItems = if (commandQuery.isEmpty()) {
                    menuItems
                } else {
                    menuItems.filter { (type, data) ->
                        if (data == null) false
                        else data.first.contains(commandQuery, ignoreCase = true) || type.toString().contains(commandQuery, ignoreCase = true)
                    }
                }

                if (filteredMenuItems.isEmpty()) {
                    DropdownMenuItem(text = { Text("No matching blocks found", color = Color.Gray, style = MaterialTheme.typography.bodySmall) }, onClick = {})
                } else {
                    filteredMenuItems.forEach { (type, data) ->
                        if (data == null) {
                            Text(type.toString(), style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                        } else {
                            val (label, iconStr) = data
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(iconStr, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(32.dp), color = MaterialTheme.colorScheme.primary)
                                        Text(label, style = MaterialTheme.typography.bodyMedium)
                                    }
                                },
                                onClick = { applyType(type as BlockType) },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}