package com.example.loom.ui.sandbox

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import com.example.loom.data.models.TextFormatSpan
import com.example.loom.ui.sandbox.blocks.AudioBlockRenderer
import com.example.loom.ui.sandbox.blocks.BookmarkBlockRenderer
import com.example.loom.ui.sandbox.blocks.CalendarBlockRenderer
import com.example.loom.ui.sandbox.blocks.ChartBlockRenderer
import com.example.loom.ui.sandbox.blocks.CountdownBlockRenderer
import com.example.loom.ui.sandbox.blocks.FileBlockRenderer
import com.example.loom.ui.sandbox.blocks.GalleryBlockRenderer
import com.example.loom.ui.sandbox.blocks.ImageBlockRenderer
import com.example.loom.ui.sandbox.blocks.KanbanBlockRenderer
import com.example.loom.ui.sandbox.blocks.LinkBlockRenderer
import com.example.loom.ui.sandbox.blocks.ProgressBlockRenderer
import com.example.loom.ui.sandbox.blocks.SketchBlockRenderer
import com.example.loom.ui.sandbox.blocks.TableBlockRenderer
import com.example.loom.ui.sandbox.blocks.TimerBlockRenderer
import com.example.loom.ui.sandbox.blocks.VideoBlockRenderer
import com.example.loom.util.CodeSyntaxTransformation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    isWidgetMode: Boolean = false,
    isReadOnly: Boolean = false,
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

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(isFocused, block.type) {
        if (isFocused && !isReadOnly) {
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
        if (isReadOnly) return@handleTextChange

        val newText = newTextState.text
        val oldText = textState.text
        var newFormats = currentBlock.textFormats

        if (newText != oldText) {
            newFormats = adjustFormats(oldText, newText, currentBlock.textFormats, newTextState.selection.start)
        }

        val needsZWSP = block.type in listOf(BlockType.TEXT, BlockType.HEADING, BlockType.HEADING_2, BlockType.HEADING_3, BlockType.HEADING_4, BlockType.BULLET, BlockType.NUMBER, BlockType.TODO, BlockType.QUOTE, BlockType.CALLOUT, BlockType.TOGGLE, BlockType.IMAGE, BlockType.AUDIO, BlockType.FILE, BlockType.SPACE, BlockType.LINK, BlockType.PROGRESS, BlockType.MATH, BlockType.VIDEO, BlockType.KANBAN, BlockType.SKETCH, BlockType.GALLERY, BlockType.CARD)
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
                } else if (block.type in listOf(BlockType.IMAGE, BlockType.AUDIO, BlockType.FILE, BlockType.SPACE, BlockType.LINK, BlockType.PROGRESS, BlockType.MATH, BlockType.VIDEO, BlockType.KANBAN, BlockType.SKETCH, BlockType.GALLERY, BlockType.CARD) && remainder.isEmpty()) {
                    android.util.Log.e("LoomDeleteTrace", "Backspace triggered deletion for block ${block.id}")
                    onDeleteBlock("", true)
                    keyboardController?.show()
                    return@handleTextChange // FIX: Force stop to prevent ghost updates during deletion
                } else {
                    android.util.Log.e("LoomDeleteTrace", "Backspace triggered remainder merge for block ${block.id}")
                    onDeleteBlock(remainder, false)
                    keyboardController?.show()
                    return@handleTextChange // FIX: Force stop
                }
            }
            else if (newText.contains('\n')) {
                if (isProcessingEnter) return@handleTextChange
                isProcessingEnter = true

                val parts = newText.substring(1).split('\n', limit = 2)
                val currentLine = parts[0]
                val nextContent = ZWSP + (parts.getOrNull(1) ?: "")

                if (currentLine.isEmpty() && block.type != BlockType.TEXT && block.type != BlockType.TOGGLE && block.type != BlockType.CARD) {
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
            if (textState.text.replace(ZWSP, "").isEmpty() && isFocused && !isReadOnly) {
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
                    BlockType.CARD -> "Widget Name"
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

    val hideDragHandle = isWidgetMode && block.type == BlockType.CARD
    val showHandle = if (hideDragHandle) false else if (isTextBased) { !isEmptyText && (isFocused || isDragging) && !isReadOnly } else { !isReadOnly }

    val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp, label = "drag_elevation")
    val bgColor = if (isDragging) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent

    var rowModifier = Modifier.fillMaxWidth()
    if (isDragging) {
        rowModifier = rowModifier.zIndex(1f).shadow(elevation, RoundedCornerShape(8.dp)).background(bgColor, RoundedCornerShape(8.dp))
    }

    Row(modifier = rowModifier, verticalAlignment = Alignment.Top) {
        val handleTopPadding = when (block.type) {
            BlockType.HEADING -> 20.dp
            BlockType.HEADING_2 -> 16.dp
            BlockType.HEADING_3 -> 12.dp
            BlockType.HEADING_4 -> 10.dp
            BlockType.IMAGE, BlockType.VIDEO, BlockType.AUDIO, BlockType.FILE, BlockType.SPACE, BlockType.LINK, BlockType.CARD -> 22.dp
            BlockType.SKETCH, BlockType.KANBAN, BlockType.CALENDAR, BlockType.TABLE -> 16.dp
            BlockType.DIVIDER -> 6.dp
            else -> 6.dp
        }

        var dragOffset by remember { mutableFloatStateOf(0f) }

        if (!hideDragHandle) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .padding(top = handleTopPadding)
                    .pointerInput(block.id) {
                        if (!isReadOnly) {
                            detectVerticalDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = { isDragging = false; dragOffset = 0f },
                                onDragCancel = { isDragging = false; dragOffset = 0f }
                            ) { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                                if (dragOffset > 40f) { onMove(1); dragOffset = 0f }
                                else if (dragOffset < -40f) { onMove(-1); dragOffset = 0f }
                            }
                        }
                    },
                contentAlignment = Alignment.TopStart
            ) {
                if (showHandle) { Icon(Icons.Default.DragIndicator, "Drag to reorder", tint = Color.LightGray, modifier = Modifier.size(20.dp)) }
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

                val bgColorCallout = if (block.type == BlockType.CALLOUT) Color(0xFFF2FBF5) else if (block.type == BlockType.QUOTE) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent
                val shape = if (block.type == BlockType.CALLOUT || block.type == BlockType.QUOTE) RoundedCornerShape(12.dp) else RoundedCornerShape(0.dp)
                val shadow = if (block.type == BlockType.CALLOUT) 4.dp else 0.dp

                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = bgColorCallout, shape = shape,
                        modifier = Modifier.fillMaxWidth().padding(vertical = if (block.type == BlockType.QUOTE) 4.dp else 0.dp).shadow(shadow, shape, spotColor = if (block.type == BlockType.CALLOUT) Color(0xFF4CAF50) else Color.Black)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().then(if (block.type == BlockType.QUOTE) Modifier.height(IntrinsicSize.Min) else Modifier).padding(if (block.type == BlockType.CALLOUT) 14.dp else if (block.type == BlockType.QUOTE) 16.dp else 0.dp),
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
                                BasicTextField(value = block.icon, onValueChange = { if (it.length <= 4 && !isReadOnly) onUpdate(currentBlock.copy(icon = it)) }, textStyle = MaterialTheme.typography.titleMedium, modifier = Modifier.width(32.dp).padding(end = 8.dp), readOnly = isReadOnly)
                            } else if (block.type == BlockType.TOGGLE) {
                                Icon(imageVector = if (block.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(24.dp).clickable { onUpdate(currentBlock.copy(isExpanded = !block.isExpanded)) })
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                BasicTextField(
                                    value = textState, onValueChange = handleTextChange, textStyle = textStyle,
                                    modifier = commonModifier.padding(start = if (block.type == BlockType.TOGGLE) 4.dp else 0.dp),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = placeholderUI,
                                    readOnly = isReadOnly
                                )
                                if (block.type == BlockType.QUOTE) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    BasicTextField(
                                        value = block.secondaryContent, onValueChange = { if (!isReadOnly) onUpdate(currentBlock.copy(secondaryContent = it)) },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.align(Alignment.End), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        decorationBox = { inner -> Box { if (block.secondaryContent.isEmpty()) Text("- Author", color = Color.LightGray) else Row{ Text("- "); inner() } } },
                                        readOnly = isReadOnly
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
                                    for (i in childIndex - 1 downTo 0) { if (block.children[i].type == BlockType.NUMBER) cCount++ else break }
                                    cCount
                                } else 1

                                key(childBlock.id) {
                                    BlockRenderer(
                                        block = childBlock, numberIndex = cNumberedIndex, allSpaces = allSpaces, currentSpaceId = currentSpaceId, recentTableColors = recentTableColors,
                                        focusRequesters = focusRequesters, focusTargetId = focusTargetId, allBlocks = allBlocks,
                                        isWidgetMode = isWidgetMode, isReadOnly = isReadOnly,
                                        onScrollToBlock = onScrollToBlock,
                                        onMove = { offset ->
                                            val currentIndex = currentBlock.children.indexOfFirst { it.id == childBlock.id }
                                            if (currentIndex != -1) {
                                                val newIndex = currentIndex + offset
                                                if (newIndex in 0 until currentBlock.children.size) {
                                                    val newChildren = currentBlock.children.toMutableList()
                                                    val temp = newChildren[currentIndex]
                                                    newChildren[currentIndex] = newChildren[newIndex]
                                                    newChildren[newIndex] = temp
                                                    onUpdate(currentBlock.copy(children = newChildren))
                                                }
                                            }
                                        },
                                        onFocusRequest = onFocusRequest, onScrollToTop = onScrollToTop, onNavigateToSpace = onNavigateToSpace, onCreateLinkedSpace = onCreateLinkedSpace,
                                        onUpdate = { updatedChild ->
                                            val newChildren = currentBlock.children.toMutableList()
                                            val safeIndex = newChildren.indexOfFirst { it.id == updatedChild.id }
                                            if (safeIndex != -1) { newChildren[safeIndex] = updatedChild; onUpdate(currentBlock.copy(children = newChildren)) }
                                        },
                                        onSplitBlock = { currentContent, nextType, nextContent ->
                                            val newChildren = currentBlock.children.toMutableList()
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
                                            val newChildren = currentBlock.children.toMutableList()
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
                            if (!isReadOnly && block.children.isEmpty()) {
                                TextButton(onClick = { onUpdate(currentBlock.copy(children = listOf(SandboxBlock(content = ZWSP)))) }) { Text("+ Add inside toggle", color = Color.Gray) }
                            }
                        }
                    }
                }
            } else {
                when (block.type) {
                    BlockType.CALENDAR -> CalendarBlockRenderer(currentBlock, onUpdate, onDeleteBlock)
                    BlockType.SKETCH -> SketchBlockRenderer(currentBlock, textState, handleTextChange, commonModifier, placeholderUI, onUpdate, onDeleteBlock)
                    BlockType.KANBAN -> KanbanBlockRenderer(currentBlock, textState, handleTextChange, commonModifier, placeholderUI, onUpdate, onDeleteBlock)
                    BlockType.TABLE -> TableBlockRenderer(currentBlock, recentTableColors, onUpdate, onDeleteBlock)
                    BlockType.IMAGE -> ImageBlockRenderer(currentBlock, textState, handleTextChange, commonModifier, placeholderUI, mediaLauncher, onDeleteBlock)
                    BlockType.VIDEO -> VideoBlockRenderer(currentBlock, textState, handleTextChange, commonModifier, placeholderUI, videoLauncher, onUpdate, onDeleteBlock)
                    BlockType.AUDIO -> AudioBlockRenderer(currentBlock, textState, handleTextChange, commonModifier, placeholderUI, mediaLauncher, audioLauncher, onUpdate, onDeleteBlock)
                    BlockType.FILE -> FileBlockRenderer(currentBlock, textState, handleTextChange, commonModifier, placeholderUI, mediaLauncher, onDeleteBlock)
                    BlockType.CHART -> ChartBlockRenderer(currentBlock, textState, onUpdate, onDeleteBlock)
                    BlockType.PROGRESS -> ProgressBlockRenderer(currentBlock, textState, handleTextChange, commonModifier, placeholderUI, onUpdate, onDeleteBlock)
                    BlockType.TIMER -> TimerBlockRenderer(currentBlock, onDeleteBlock)
                    BlockType.COUNTDOWN -> CountdownBlockRenderer(currentBlock, onUpdate, onDeleteBlock)
                    BlockType.LINK -> LinkBlockRenderer(currentBlock, textState, handleTextChange, commonModifier, placeholderUI, onUpdate, onDeleteBlock)
                    BlockType.BOOKMARK -> BookmarkBlockRenderer(currentBlock, onUpdate, onDeleteBlock)
                    BlockType.GALLERY -> GalleryBlockRenderer(currentBlock, onUpdate, onDeleteBlock)
                    BlockType.CARD -> {
                        // --- UI UPGRADE: Solid Color Matching ---
                        // We use a pure, solid surfaceVariant without alpha for the Home widgets to perfectly match M3 Cards
                        val cardBgColor = if (isWidgetMode) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        val cardShape = if (isWidgetMode) RoundedCornerShape(24.dp) else RoundedCornerShape(16.dp)
                        val cardBorder = if (isWidgetMode) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

                        Surface(color = cardBgColor, shape = cardShape, border = cardBorder, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BasicTextField(
                                        value = textState, onValueChange = handleTextChange,
                                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground),
                                        modifier = commonModifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        decorationBox = placeholderUI,
                                        readOnly = isReadOnly
                                    )

                                    if (!isWidgetMode) {
                                        IconButton(onClick = { onUpdate(currentBlock.copy(isLocked = !currentBlock.isLocked)) }, modifier = Modifier.size(32.dp)) {
                                            Icon(if (block.isLocked) Icons.Default.Lock else Icons.Outlined.LockOpen, "Lock", tint = if (block.isLocked) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(20.dp))
                                        }

                                        var showPinMenu by remember { mutableStateOf(false) }
                                        IconButton(onClick = { showPinMenu = true }, modifier = Modifier.size(32.dp)) {
                                            Icon(if (block.pinnedTab.isNotBlank()) Icons.Default.PushPin else Icons.Outlined.PushPin, "Pin", tint = if (block.pinnedTab.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(20.dp))
                                        }
                                        DropdownMenu(expanded = showPinMenu, onDismissRequest = { showPinMenu = false }) {
                                            val tabsList = block.pinnedTab.split(",").filter { it.isNotBlank() }.toMutableSet()
                                            val isHome = tabsList.contains("Home")
                                            val isHealth = tabsList.contains("Health")

                                            DropdownMenuItem(
                                                text = { Text("Home") },
                                                trailingIcon = { if (isHome) Icon(Icons.Default.Check, null) },
                                                onClick = {
                                                    if (isHome) tabsList.remove("Home") else tabsList.add("Home")
                                                    val newPins = tabsList.joinToString(",")
                                                    onUpdate(currentBlock.copy(pinnedTab = newPins))
                                                    showPinMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Health") },
                                                trailingIcon = { if (isHealth) Icon(Icons.Default.Check, null) },
                                                onClick = {
                                                    if (isHealth) tabsList.remove("Health") else tabsList.add("Health")
                                                    val newPins = tabsList.joinToString(",")
                                                    onUpdate(currentBlock.copy(pinnedTab = newPins))
                                                    showPinMenu = false
                                                }
                                            )
                                        }

                                        Spacer(Modifier.width(4.dp))
                                        IconButton(onClick = {
                                            val prefs = context.getSharedPreferences("loom_prefs", android.content.Context.MODE_PRIVATE)
                                            prefs.edit().putBoolean("deleted_card_${currentBlock.id}", true).apply()
                                            onDeleteBlock("", true)
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        }
                                    } else if (block.isLocked) {
                                        Icon(Icons.Default.Lock, "Locked", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                    }
                                }

                                val dividerColor = if (isWidgetMode) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)

                                Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
                                    block.children.forEachIndexed { childIndex, childBlock ->
                                        val cNumberedIndex = if (childBlock.type == BlockType.NUMBER) {
                                            var cCount = 1
                                            for (i in childIndex - 1 downTo 0) { if (block.children[i].type == BlockType.NUMBER) cCount++ else break }
                                            cCount
                                        } else 1

                                        key(childBlock.id) {
                                            BlockRenderer(
                                                block = childBlock, numberIndex = cNumberedIndex, allSpaces = allSpaces, currentSpaceId = currentSpaceId, recentTableColors = recentTableColors,
                                                focusRequesters = focusRequesters, focusTargetId = focusTargetId, allBlocks = allBlocks,
                                                isWidgetMode = isWidgetMode,
                                                isReadOnly = isReadOnly || block.isLocked,
                                                onScrollToBlock = onScrollToBlock,
                                                onMove = { offset ->
                                                    val currentIndex = currentBlock.children.indexOfFirst { it.id == childBlock.id }
                                                    if (currentIndex != -1) {
                                                        val newIndex = currentIndex + offset
                                                        if (newIndex in 0 until currentBlock.children.size) {
                                                            val newChildren = currentBlock.children.toMutableList()
                                                            val temp = newChildren[currentIndex]
                                                            newChildren[currentIndex] = newChildren[newIndex]
                                                            newChildren[newIndex] = temp
                                                            onUpdate(currentBlock.copy(children = newChildren))
                                                        }
                                                    }
                                                },
                                                onFocusRequest = onFocusRequest, onScrollToTop = onScrollToTop, onNavigateToSpace = onNavigateToSpace, onCreateLinkedSpace = onCreateLinkedSpace,
                                                onUpdate = { updatedChild ->
                                                    val newChildren = currentBlock.children.toMutableList()
                                                    val safeIndex = newChildren.indexOfFirst { it.id == updatedChild.id }
                                                    if (safeIndex != -1) { newChildren[safeIndex] = updatedChild; onUpdate(currentBlock.copy(children = newChildren)) }
                                                },
                                                onSplitBlock = { currentContent, nextType, nextContent ->
                                                    val newChildren = currentBlock.children.toMutableList()
                                                    val safeIndex = newChildren.indexOfFirst { it.id == childBlock.id }
                                                    if (safeIndex != -1) {
                                                        newChildren[safeIndex] = newChildren[safeIndex].copy(content = currentContent)
                                                        val newBlockId = java.util.UUID.randomUUID().toString()
                                                        newChildren.add(safeIndex + 1, SandboxBlock(id = newBlockId, type = nextType, content = nextContent))
                                                        onUpdate(currentBlock.copy(children = newChildren))
                                                        onFocusRequest(newBlockId)
                                                    }
                                                },
                                                onDeleteBlock = { remainder, deleteEntirely ->
                                                    val newChildren = currentBlock.children.toMutableList()
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
                                                            if (deleteEntirely || remainder.trim().replace("\u200B","").isEmpty()) {
                                                                newChildren.removeAt(0)
                                                                onUpdate(currentBlock.copy(children = newChildren))
                                                                onFocusRequest(block.id)
                                                            } else {
                                                                newChildren[0] = newChildren[0].copy(content = "\u200B" + remainder)
                                                                onUpdate(currentBlock.copy(children = newChildren))
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }

                                    val lastChild = block.children.lastOrNull()
                                    val showAddBtn = lastChild == null || lastChild.type != BlockType.TEXT || lastChild.content.replace("\u200B", "").isNotEmpty()

                                    // Hides the "Add Block" button if it's on the Home screen to reduce clutter
                                    if (!isReadOnly && !block.isLocked && showAddBtn && !isWidgetMode) {
                                        TextButton(onClick = {
                                            val newChildren = block.children.toMutableList()
                                            val newId = java.util.UUID.randomUUID().toString()
                                            newChildren.add(SandboxBlock(id = newId, content = "\u200B"))
                                            onUpdate(currentBlock.copy(children = newChildren))
                                            onFocusRequest(newId)
                                        }) { Text("+ Add Block", color = Color.Gray) }
                                    }
                                }
                            }
                        }
                    }
                    BlockType.MATH -> {
                        var showMathSymbols by remember { mutableStateOf(false) }
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    Text("∑", fontSize = 24.sp, fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { if (!isReadOnly) showMathSymbols = true })
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
                                    decorationBox = placeholderUI,
                                    readOnly = isReadOnly
                                )
                            }
                        }
                    }
                    BlockType.CODE -> {
                        Surface(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                BasicTextField(
                                    value = textState,
                                    onValueChange = handleTextChange,
                                    visualTransformation = CodeSyntaxTransformation(),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFD4D4D4), fontFamily = FontFamily.Monospace),
                                    modifier = commonModifier.padding(16.dp).padding(end = 24.dp),
                                    cursorBrush = SolidColor(Color.White),
                                    readOnly = isReadOnly,
                                    decorationBox = { inner -> Box{ if (textState.text.isEmpty()) Text("// write code here", color = Color.Gray, fontFamily = FontFamily.Monospace); inner() } }
                                )
                                if (!isReadOnly) {
                                    IconButton(
                                        onClick = { onDeleteBlock("", true) },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, "Delete Code", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    BlockType.SPACE -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (block.url.isEmpty()) {
                                var showSpacePicker by remember { mutableStateOf(false) }
                                if (!isReadOnly) {
                                    Button(onClick = { showSpacePicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)) {
                                        Icon(Icons.Default.Dashboard, null); Spacer(Modifier.width(8.dp)); Text("Select or Create a Space to Link")
                                    }
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
                                            if (!isReadOnly) {
                                                IconButton(onClick = { showBlockMenu = true }) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                                                DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                                    DropdownMenuItem(text = { Text("Delete Block", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                                                    DropdownMenuItem(text = { Text("Change Space") }, onClick = { onUpdate(currentBlock.copy(url = "")); showBlockMenu = false })
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(12.dp))
                                            Text("Space not found or deleted.", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                                            if (!isReadOnly) {
                                                IconButton(onClick = { onDeleteBlock("", true) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
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
                            if (!isReadOnly) {
                                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)) {
                                    Icon(Icons.Default.MoreVert, "Options", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                                    DropdownMenuItem(text = { Text("Delete Divider", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                                }
                            }
                        }
                    }
                    BlockType.TOC -> {
                        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Table of Contents", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                if (!isReadOnly) {
                                    IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                                }
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
                                decorationBox = placeholderUI,
                                readOnly = isReadOnly
                            )
                        }
                    }
                    else -> {}
                }
            }

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
                                                if (color == Transparent) {
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
                    BlockType.CARD to ("Widget Card" to "🗂️"),
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
                    BlockType.COUNTDOWN to ("Countdown" to "⏳"),
                    BlockType.GALLERY to ("Photo Gallery" to "📸"),

                    "Media & Links" to null,
                    BlockType.IMAGE to ("Image" to "🖼️"),
                    BlockType.VIDEO to ("Video" to "▶️"),
                    BlockType.AUDIO to ("Audio" to "🎵"),
                    BlockType.FILE to ("File" to "📁"),
                    BlockType.SPACE to ("Link Space" to "🚀"),
                    BlockType.LINK to ("Web Link" to "🌐") ,
                    BlockType.BOOKMARK to ("Web Bookmark" to "🔗")
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