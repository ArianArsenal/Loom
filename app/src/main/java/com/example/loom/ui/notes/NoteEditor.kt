package com.example.loom.ui.notes

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // --- FIX ---
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft // --- FIX ---
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight // --- FIX ---
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt // --- FIX ---
import coil.compose.AsyncImage
import com.example.loom.data.local.entity.DiaryNote
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.ui.home.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat // --- FIX ---
import java.util.Date // --- FIX ---
import java.util.Locale // --- FIX ---
import java.util.UUID // --- FIX ---
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoteEditor(initialNote: DiaryNote?, allTags: List<NoteTag>, viewModel: HomeViewModel, onNavigateBack: () -> Unit) {
    var currentId by remember { mutableIntStateOf(initialNote?.id ?: 0) }
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var selectedTag by remember { mutableStateOf(initialNote?.tag ?: "Personal") }
    var isFavorite by remember { mutableStateOf(initialNote?.isFavorite ?: false) }
    var isPrivate by remember { mutableStateOf(initialNote?.isPrivate ?: false) }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }

    val initialBlocks = try { Json.decodeFromString<List<NoteBlock>>(initialNote?.blocksJson ?: "[]") } catch (_: Exception) { emptyList() }
    val blocks = remember { mutableStateListOf<NoteBlock>().apply {
        if (initialBlocks.isEmpty() && initialNote?.content?.isNotBlank() == true) {
            add(NoteBlock(id = UUID.randomUUID().toString(), type = "text", content = ZWSP + initialNote.content))
        } else if (initialBlocks.isEmpty()) {
            add(NoteBlock(id = UUID.randomUUID().toString(), type = "text", content = ZWSP))
        } else {
            addAll(initialBlocks.map {
                if (it.type == "text" || it.type == "check") {
                    it.copy(content = if (!it.content.startsWith(ZWSP)) ZWSP + it.content else it.content)
                } else it
            })
        }
    }}

    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<NoteTag?>(null) }

    val focusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val density = LocalDensity.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(focusedBlockId) {
        focusedBlockId?.let { id ->
            try { focusRequesters[id]?.requestFocus() } catch (_: Exception) {}
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val insertIndex = blocks.indexOfFirst { it.id == focusedBlockId }.takeIf { it >= 0 }?.plus(1) ?: blocks.size
            blocks.add(insertIndex, NoteBlock(id = UUID.randomUUID().toString(), type = "image", content = uri.toString()))

            val newTextId = UUID.randomUUID().toString()
            blocks.add(insertIndex + 1, NoteBlock(id = newTextId, type = "text", content = ZWSP))
            focusedBlockId = newTextId
        }
    }

    val performSave: suspend () -> Unit = {
        val cleanBlocks = blocks.map {
            if (it.type == "text" || it.type == "check") it.copy(content = it.content.replace(ZWSP, "")) else it
        }
        if (currentId != 0 || title.isNotBlank() || cleanBlocks.any { it.content.isNotBlank() || it.type == "image" }) {
            val blocksJson = Json.encodeToString(cleanBlocks)
            val returnedId = viewModel.saveDiaryNoteWithReturn(currentId, title, "", selectedTag, "[]", blocksJson, isFavorite, isPrivate, isPinned, false)
            if (currentId == 0) currentId = returnedId.toInt()
        }
    }

    LaunchedEffect(title, selectedTag, isFavorite, isPrivate, isPinned, blocks.toList()) {
        delay(500.milliseconds) // --- FIX ---
        performSave()
    }

    BackHandler {
        scope.launch {
            performSave()
            onNavigateBack()
        }
    }

    if (showTagDialog) {
        var newTagName by remember { mutableStateOf("") }
        var hue by remember { mutableFloatStateOf(210f) }
        var saturation by remember { mutableFloatStateOf(0.9f) }
        var value by remember { mutableFloatStateOf(0.9f) }
        val dynamicColor = Color.hsv(hue, saturation, value)
        val hexString = String.format("#%02X%02X%02X", (dynamicColor.red * 255).toInt(), (dynamicColor.green * 255).toInt(), (dynamicColor.blue * 255).toInt())

        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Create Custom Tag") },
            text = {
                Column {
                    OutlinedTextField(value = newTagName, onValueChange = { newTagName = it }, label = { Text("Tag Name") }, singleLine = true)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(Color.hsv(hue, 1f, 1f))
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume(); saturation = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f); value = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                            }
                        }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(brush = Brush.horizontalGradient(colors = listOf(Color.White, Color.Transparent)))
                            drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black)))
                            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(saturation * size.width, (1f - value) * size.height), style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Canvas(modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(50))
                        .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); hue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f } }
                    ) {
                        drawRect(brush = Brush.horizontalGradient(colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                        drawCircle(color = Color.White, radius = 12.dp.toPx(), center = Offset((hue / 360f) * size.width, size.height / 2), style = Stroke(width = 3.dp.toPx()))
                    }
                }
            },
            confirmButton = { Button(onClick = { if (newTagName.isNotBlank()) { viewModel.addNoteTag(newTagName, hexString); selectedTag = newTagName; showTagDialog = false } }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showTagDialog = false }) { Text("Cancel") } }
        )
    }

    if (tagToDelete != null) {
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("Delete Tag?") },
            text = { Text("Are you sure you want to delete '${tagToDelete?.name}'? It will be removed from the system.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteNoteTag(tagToDelete!!)
                    if (selectedTag == tagToDelete?.name) selectedTag = "Personal"
                    tagToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { tagToDelete = null }) { Text("Cancel") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("") },
            navigationIcon = {
                IconButton(onClick = {
                    scope.launch {
                        performSave()
                        onNavigateBack()
                    }
                }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } // --- FIX ---
            },
            actions = {
                IconButton(onClick = { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Default.Image, "Add Image", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {
                    val currentIndex = blocks.indexOfFirst { it.id == focusedBlockId }
                    if (currentIndex >= 0) {
                        val currentBlock = blocks[currentIndex]
                        // --- FIX: Converted to 'when' for faster bytecode execution ---
                        when (currentBlock.type) {
                            "text" -> blocks[currentIndex] = currentBlock.copy(type = "check")
                            "check" -> blocks[currentIndex] = currentBlock.copy(type = "text")
                            else -> {
                                val newBlockId = UUID.randomUUID().toString()
                                blocks.add(currentIndex + 1, NoteBlock(id = newBlockId, type = "check", content = ZWSP))
                                focusedBlockId = newBlockId
                            }
                        }
                    } else {
                        val newBlockId = UUID.randomUUID().toString()
                        blocks.add(NoteBlock(id = newBlockId, type = "check", content = ZWSP))
                        focusedBlockId = newBlockId
                    }
                }) {
                    Icon(Icons.Default.Checklist, "Add Checklist", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                IconButton(onClick = { isPinned = !isPinned }) { Icon(if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, "Pin", tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = { isPrivate = !isPrivate }) { Icon(if (isPrivate) Icons.Default.Lock else Icons.Outlined.Lock, "Toggle Private", tint = if (isPrivate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = { isFavorite = !isFavorite }) { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Toggle Favorite", tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                            val lastBlock = blocks.lastOrNull()
                            if (lastBlock != null && lastBlock.type == "text") {
                                focusedBlockId = lastBlock.id
                            } else {
                                val newBlockId = UUID.randomUUID().toString()
                                blocks.add(NoteBlock(id = newBlockId, type = "text", content = ZWSP))
                                focusedBlockId = newBlockId
                            }
                        })
                    }
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item { AssistChip(onClick = { showTagDialog = true }, label = { Text("+ New Tag") }) }
                    items(allTags) { tag ->
                        val tagColor = try { Color(tag.colorHex.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.primary }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTag == tag.name) tagColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.combinedClickable(
                                onClick = { selectedTag = tag.name },
                                onLongClick = { tagToDelete = tag }
                            )
                        ) {
                            Text(tag.name, style = MaterialTheme.typography.labelMedium, color = if (selectedTag == tag.name) tagColor else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp, start = 20.dp, end = 20.dp))

                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    placeholder = { Text("Title", style = MaterialTheme.typography.titleLarge) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)
                )

                if (initialNote != null) {
                    val dateStr = SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(initialNote.timestamp)) // --- FIX ---
                    Text(
                        text = "Last edited: $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 36.dp).padding(bottom = 16.dp)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    blocks.forEach { block ->
                        when (block.type) {
                            "text" -> {
                                val requester = focusRequesters.getOrPut(block.id) { FocusRequester() }
                                var textState by remember { mutableStateOf(TextFieldValue(block.content, TextRange(block.content.length))) }

                                if (textState.text != block.content) {
                                    textState = textState.copy(text = block.content, selection = TextRange(block.content.length))
                                }

                                BasicTextField(
                                    value = textState,
                                    onValueChange = { newTextState ->
                                        val currentIndex = blocks.indexOfFirst { it.id == block.id }
                                        if (currentIndex == -1) return@BasicTextField

                                        val newText = newTextState.text

                                        if (newText != blocks[currentIndex].content) {
                                            if (!newText.startsWith(ZWSP) || newText.isEmpty()) {
                                                val remainder = newText.replace(ZWSP, "")
                                                if (currentIndex > 0) {
                                                    val prevBlock = blocks[currentIndex - 1]
                                                    if (prevBlock.type == "text" || prevBlock.type == "check") {
                                                        blocks[currentIndex - 1] = prevBlock.copy(content = prevBlock.content + remainder)
                                                        blocks.removeAt(currentIndex)
                                                        focusedBlockId = prevBlock.id
                                                    } else {
                                                        textState = newTextState.copy(text = ZWSP + remainder)
                                                        blocks[currentIndex] = blocks[currentIndex].copy(content = ZWSP + remainder)
                                                    }
                                                } else {
                                                    textState = newTextState.copy(text = ZWSP + remainder)
                                                    blocks[currentIndex] = blocks[currentIndex].copy(content = ZWSP + remainder)
                                                }
                                            } else if (newText.contains('\n')) {
                                                val parts = newText.substring(1).split('\n', limit = 2)
                                                blocks[currentIndex] = blocks[currentIndex].copy(content = ZWSP + parts[0])
                                                val newBlockId = UUID.randomUUID().toString()
                                                blocks.add(currentIndex + 1, NoteBlock(id = newBlockId, type = "text", content = ZWSP + parts.getOrElse(1) { "" }))
                                                focusedBlockId = newBlockId
                                            } else {
                                                textState = newTextState
                                                blocks[currentIndex] = blocks[currentIndex].copy(content = newText)
                                            }
                                        } else {
                                            textState = newTextState
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(requester)
                                        .onFocusChanged { if (it.isFocused) focusedBlockId = block.id }
                                        .padding(vertical = 2.dp),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                                )
                            }
                            "check" -> {
                                val requester = focusRequesters.getOrPut(block.id) { FocusRequester() }
                                var textState by remember { mutableStateOf(TextFieldValue(block.content, TextRange(block.content.length))) }

                                if (textState.text != block.content) {
                                    textState = textState.copy(text = block.content, selection = TextRange(block.content.length))
                                }

                                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Icon(
                                        imageVector = if (block.isChecked) Icons.Outlined.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 1.dp).size(22.dp).clickable {
                                            val currentIndex = blocks.indexOfFirst { it.id == block.id }
                                            if (currentIndex >= 0) blocks[currentIndex] = block.copy(isChecked = !block.isChecked)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    BasicTextField(
                                        value = textState,
                                        onValueChange = { newTextState ->
                                            val currentIndex = blocks.indexOfFirst { it.id == block.id }
                                            if (currentIndex == -1) return@BasicTextField

                                            val newText = newTextState.text

                                            if (newText != blocks[currentIndex].content) {
                                                if (!newText.startsWith(ZWSP) || newText.isEmpty()) {
                                                    val remainder = newText.replace(ZWSP, "")
                                                    if (currentIndex > 0) {
                                                        val prevBlock = blocks[currentIndex - 1]
                                                        if (prevBlock.type == "text" || prevBlock.type == "check") {
                                                            blocks[currentIndex - 1] = prevBlock.copy(content = prevBlock.content + remainder)
                                                            blocks.removeAt(currentIndex)
                                                            focusedBlockId = prevBlock.id
                                                        } else {
                                                            textState = newTextState.copy(text = ZWSP + remainder)
                                                            blocks[currentIndex] = blocks[currentIndex].copy(type = "text", content = ZWSP + remainder)
                                                        }
                                                    } else {
                                                        textState = newTextState.copy(text = ZWSP + remainder)
                                                        blocks[currentIndex] = blocks[currentIndex].copy(type = "text", content = ZWSP + remainder)
                                                    }
                                                } else if (newText.contains('\n')) {
                                                    val parts = newText.substring(1).split('\n', limit = 2)
                                                    blocks[currentIndex] = blocks[currentIndex].copy(content = ZWSP + parts[0])
                                                    val newBlockId = UUID.randomUUID().toString()
                                                    blocks.add(currentIndex + 1, NoteBlock(id = newBlockId, type = "check", content = ZWSP + parts.getOrElse(1) { "" }))
                                                    focusedBlockId = newBlockId
                                                } else {
                                                    textState = newTextState
                                                    blocks[currentIndex] = blocks[currentIndex].copy(content = newText)
                                                }
                                            } else {
                                                textState = newTextState
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusRequester(requester)
                                            .onFocusChanged { if (it.isFocused) focusedBlockId = block.id }
                                            .padding(top = 2.dp),
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = if (block.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                            textDecoration = if (block.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                            "image" -> {
                                val isSelected = focusedBlockId == block.id
                                var currentWidth by remember(block.id) { mutableFloatStateOf(block.width) }
                                var currentHeight by remember(block.id) { mutableFloatStateOf(block.height) }

                                val boxAlignment = when (block.alignment) {
                                    "Start" -> Alignment.CenterStart
                                    "End" -> Alignment.CenterEnd
                                    else -> Alignment.Center
                                }

                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    contentAlignment = boxAlignment
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(currentWidth.dp)
                                            .height(currentHeight.dp)
                                    ) {
                                        AsyncImage(
                                            model = block.content,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .pointerInput(Unit) {
                                                    detectTapGestures(onTap = {
                                                        focusedBlockId = block.id
                                                        focusManager.clearFocus()
                                                    })
                                                },
                                            contentScale = if (block.isCropped) ContentScale.Crop else ContentScale.Fit
                                        )

                                        if (isSelected) {
                                            val highlightColor = Color(0xFF4285F4)
                                            Box(modifier = Modifier.fillMaxSize().border(2.dp, highlightColor, RoundedCornerShape(8.dp)))

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.Black.copy(alpha = 0.8f),
                                                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp).offset(y = (-45).dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {

                                                    // Move Up
                                                    IconButton(onClick = {
                                                        val idx = blocks.indexOfFirst { it.id == block.id }
                                                        if (idx > 0) {
                                                            val temp = blocks[idx]
                                                            blocks[idx] = blocks[idx - 1]
                                                            blocks[idx - 1] = temp
                                                        }
                                                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowUp, "Move Up", tint = Color.White, modifier = Modifier.size(18.dp)) }

                                                    // Move Down
                                                    IconButton(onClick = {
                                                        val idx = blocks.indexOfFirst { it.id == block.id }
                                                        if (idx in 0 until blocks.size - 1) {
                                                            val temp = blocks[idx]
                                                            blocks[idx] = blocks[idx + 1]
                                                            blocks[idx + 1] = temp
                                                        }
                                                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowDown, "Move Down", tint = Color.White, modifier = Modifier.size(18.dp)) }

                                                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.3f)).padding(horizontal = 4.dp))

                                                    // Align Left
                                                    IconButton(onClick = {
                                                        val idx = blocks.indexOfFirst { it.id == block.id }
                                                        if (idx >= 0) blocks[idx] = block.copy(alignment = "Start")
                                                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, "Left", tint = if (block.alignment == "Start") highlightColor else Color.White, modifier = Modifier.size(16.dp)) }

                                                    // Align Center
                                                    IconButton(onClick = {
                                                        val idx = blocks.indexOfFirst { it.id == block.id }
                                                        if (idx >= 0) blocks[idx] = block.copy(alignment = "Center")
                                                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.FormatAlignCenter, "Center", tint = if (block.alignment == "Center") highlightColor else Color.White, modifier = Modifier.size(16.dp)) }

                                                    // Align Right
                                                    IconButton(onClick = {
                                                        val idx = blocks.indexOfFirst { it.id == block.id }
                                                        if (idx >= 0) blocks[idx] = block.copy(alignment = "End")
                                                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.AutoMirrored.Filled.FormatAlignRight, "Right", tint = if (block.alignment == "End") highlightColor else Color.White, modifier = Modifier.size(16.dp)) }

                                                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.3f)).padding(horizontal = 4.dp))

                                                    // Crop & Delete
                                                    TextButton(
                                                        onClick = {
                                                            val idx = blocks.indexOfFirst { it.id == block.id }
                                                            if (idx >= 0) blocks[idx] = block.copy(isCropped = !block.isCropped)
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) { Text(if (block.isCropped) "Fit" else "Crop", color = Color.White, fontSize = 12.sp) }

                                                    TextButton(
                                                        onClick = {
                                                            val idx = blocks.indexOfFirst { it.id == block.id }
                                                            if (idx >= 0) { blocks.removeAt(idx); focusedBlockId = null }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) { Text("Delete", color = Color(0xFFE53935), fontSize = 12.sp) }
                                                }
                                            }

                                            // Drag Handle
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .offset(16.dp, 16.dp)
                                                    .size(48.dp)
                                                    .pointerInput(block.id) {
                                                        detectDragGestures(
                                                            onDragEnd = {
                                                                val currentIndex = blocks.indexOfFirst { it.id == block.id }
                                                                if (currentIndex >= 0) blocks[currentIndex] = block.copy(width = currentWidth, height = currentHeight)
                                                            }
                                                        ) { change, dragAmount ->
                                                            change.consume()
                                                            currentWidth = (currentWidth + dragAmount.x / density.density).coerceAtLeast(100f)
                                                            currentHeight = (currentHeight + dragAmount.y / density.density).coerceAtLeast(100f)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(modifier = Modifier.size(16.dp).background(Color.White, CircleShape).border(3.dp, highlightColor, CircleShape))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(300.dp))
            }
        }
    }
}