package com.example.loom.ui.sandbox

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import com.example.loom.data.models.TextFormatSpan
import com.example.loom.ui.sandbox.blocks.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
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
        } else if (type in listOf("BOLD", "ITALIC", "STRIKE", "UNDERLINE")) { // --- FIX: Added Underline support! ---
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

        val needsZWSP = block.type in listOf(BlockType.TEXT, BlockType.HEADING, BlockType.HEADING_2, BlockType.HEADING_3, BlockType.HEADING_4, BlockType.BULLET, BlockType.NUMBER, BlockType.TODO, BlockType.QUOTE, BlockType.CALLOUT, BlockType.TOGGLE, BlockType.IMAGE, BlockType.AUDIO, BlockType.FILE, BlockType.SPACE, BlockType.LINK, BlockType.PROGRESS, BlockType.MATH, BlockType.VIDEO, BlockType.KANBAN, BlockType.SKETCH, BlockType.GALLERY, BlockType.CARD, BlockType.TIMELINE)
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
                val isDeletion = newText.length < oldText.length // --- FIX: Line Combining Bug Check ---

                if (isDeletion) {
                    val remainder = newText.replace(ZWSP, "")

                    // 1. Protection for rigid blocks
                    if (block.type == BlockType.CARD || block.type == BlockType.TIMELINE) {
                        val safeText = ZWSP + remainder
                        textState = newTextState.copy(annotatedString = buildAnnotated(safeText, newFormats), selection = TextRange(1.coerceAtMost(safeText.length)))
                        onUpdate(currentBlock.copy(content = safeText, textFormats = newFormats))
                        return@handleTextChange
                    }

                    // --- FIX 2: If we delete the start of a formatting block, downgrade it to TEXT first! ---
                    if (block.type in listOf(BlockType.HEADING, BlockType.HEADING_2, BlockType.HEADING_3, BlockType.HEADING_4, BlockType.BULLET, BlockType.NUMBER, BlockType.TODO, BlockType.QUOTE, BlockType.CALLOUT, BlockType.TOGGLE)) {
                        textState = newTextState.copy(
                            annotatedString = buildAnnotated(ZWSP + remainder, newFormats),
                            selection = TextRange(1)
                        )
                        onUpdate(currentBlock.copy(type = BlockType.TEXT, content = ZWSP + remainder, textFormats = newFormats))
                        return@handleTextChange
                    }

                    // 3. Delete empty media/special blocks
                    if (block.type in listOf(BlockType.IMAGE, BlockType.AUDIO, BlockType.FILE, BlockType.SPACE, BlockType.LINK, BlockType.PROGRESS, BlockType.MATH, BlockType.VIDEO, BlockType.KANBAN, BlockType.SKETCH, BlockType.GALLERY) && remainder.isEmpty()) {
                        onDeleteBlock("", true)
                        keyboardController?.show()
                        return@handleTextChange
                    }

                    // 4. Merge text blocks
                    onDeleteBlock(remainder, false)
                    keyboardController?.show()
                    return@handleTextChange

                } else {
                    // Restore ZWSP if user typed before it
                    val restoredText = ZWSP + newText.replace(ZWSP, "")
                    val newCursor = newTextState.selection.start.coerceAtLeast(1)

                    textState = newTextState.copy(
                        annotatedString = buildAnnotated(restoredText, newFormats),
                        selection = TextRange(newCursor)
                    )
                    onUpdate(currentBlock.copy(content = restoredText, textFormats = newFormats))
                    return@handleTextChange
                }
            }

            // --- FIX: Ensure we only split if a NEW newline was typed ---
            val newlyAddedNewline = newText.length > oldText.length && newText.count { it == '\n' } > oldText.count { it == '\n' }

            if (newlyAddedNewline) {
                if (isProcessingEnter) return@handleTextChange
                isProcessingEnter = true

                val parts = newText.substring(1).split('\n', limit = 2)
                val currentLine = parts[0]
                val nextContent = ZWSP + (parts.getOrNull(1) ?: "")

                if (currentLine.isEmpty() && block.type != BlockType.TEXT && block.type != BlockType.TOGGLE && block.type != BlockType.CARD) {
                    textState = newTextState.copy(text = ZWSP, selection = TextRange(1))
                    onUpdate(currentBlock.copy(type = BlockType.TEXT, content = ZWSP, textFormats = emptyList()))
                    scope.launch { kotlinx.coroutines.delay(50); isProcessingEnter = false }
                } else {
                    val nextType = if (block.type in listOf(BlockType.TODO, BlockType.BULLET, BlockType.NUMBER)) block.type else BlockType.TEXT

                    // --- FIX: Rebuild formats instantly so color isn't lost during enter ---
                    textState = TextFieldValue(
                        annotatedString = buildAnnotated(ZWSP + currentLine, newFormats),
                        selection = TextRange((ZWSP + currentLine).length)
                    )

                    onSplitBlock(ZWSP + currentLine, nextType, nextContent)
                    scope.launch { kotlinx.coroutines.delay(50); isProcessingEnter = false }
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

                val calloutBaseColor = try {
                    if (block.secondaryContent.isNotEmpty()) Color(android.graphics.Color.parseColor(block.secondaryContent))
                    else Color(0xFF4CAF50)
                } catch (e: Exception) { Color(0xFF4CAF50) }

                val bgColorCallout = if (block.type == BlockType.CALLOUT) calloutBaseColor.copy(alpha = 0.1f) else if (block.type == BlockType.QUOTE) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent
                val shape = if (block.type == BlockType.CALLOUT || block.type == BlockType.QUOTE) RoundedCornerShape(12.dp) else RoundedCornerShape(0.dp)

                val shadowElevation = if (block.type == BlockType.CALLOUT) 16.dp else 0.dp
                val spotShadowColor = if (block.type == BlockType.CALLOUT) calloutBaseColor else Color.Black

                var showCalloutPicker by remember { mutableStateOf(false) }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = bgColorCallout, shape = shape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (block.type == BlockType.QUOTE || block.type == BlockType.CALLOUT) 4.dp else 0.dp)
                            .shadow(shadowElevation, shape, ambientColor = spotShadowColor, spotColor = spotShadowColor)
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

                            if (block.type == BlockType.CALLOUT && !isReadOnly) {
                                IconButton(onClick = { showCalloutPicker = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.MoreVert, "Change Color", tint = calloutBaseColor)
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

                    if (showCalloutPicker) {
                        CalloutColorPicker(
                            currentBlock = currentBlock,
                            onUpdate = onUpdate,
                            onDismiss = { showCalloutPicker = false }
                        )
                    }
                }
            } else {
                when (block.type) {
                    BlockType.CALENDAR -> CalendarBlockRenderer(currentBlock, onUpdate, onDeleteBlock)
                    BlockType.SKETCH -> SketchBlockRenderer(currentBlock, textState, handleTextChange, commonModifier, placeholderUI, onUpdate, onDeleteBlock)
                    BlockType.KANBAN -> KanbanBlockRenderer(currentBlock, textState, handleTextChange, commonModifier, placeholderUI, onUpdate, onDeleteBlock)
                    BlockType.TABLE -> TableBlockRenderer(currentBlock, recentTableColors, onUpdate, onDeleteBlock)
                    BlockType.TIMELINE -> TimelineBlockRenderer(currentBlock, allSpaces, currentSpaceId, recentTableColors, focusRequesters, onUpdate, onDeleteBlock)
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

                    BlockType.CARD -> CardBlockRenderer(currentBlock, textState, handleTextChange, commonModifier, placeholderUI, isWidgetMode, isReadOnly, allSpaces, currentSpaceId, recentTableColors, focusRequesters, focusTargetId, allBlocks, onScrollToBlock, onFocusRequest, onScrollToTop, onNavigateToSpace, onCreateLinkedSpace, onUpdate, onDeleteBlock)
                    BlockType.MATH -> MathBlockRenderer(currentBlock, textState, handleTextChange, commonModifier, placeholderUI, isReadOnly, onUpdate)
                    BlockType.CODE -> CodeBlockRenderer(textState, handleTextChange, commonModifier, isReadOnly, onDeleteBlock)
                    BlockType.SPACE -> SpaceBlockRenderer(currentBlock, isReadOnly, allSpaces, currentSpaceId, onUpdate, onDeleteBlock, onScrollToTop, onNavigateToSpace, onCreateLinkedSpace)
                    BlockType.DIVIDER -> DividerBlockRenderer(isReadOnly, onDeleteBlock)
                    BlockType.TOC -> TocBlockRenderer(textState, handleTextChange, commonModifier, placeholderUI, isReadOnly, allBlocks, onScrollToBlock, onDeleteBlock)
                    else -> {}
                }
            }

            TextFormatToolbar(isFocused, textState, currentBlock, recentTableColors, onUpdate, applyFormat)
            SlashCommandMenu(showMenu, { showMenu = false }, commandQuery) { type ->
                val cleanText = textState.text.substringBeforeLast('/')
                textState = textState.copy(text = cleanText, selection = TextRange(cleanText.length))
                val updated = currentBlock.copy(type = type, content = cleanText)
                if (type == BlockType.TABLE) updated.tableData = List(3) { List(3) { encodeCell(TableCellData()) } }
                onUpdate(updated)
                showMenu = false
                try { focusRequester.requestFocus() } catch(e: Exception){}
                onFocusRequest(block.id)
            }
        }
    }
}