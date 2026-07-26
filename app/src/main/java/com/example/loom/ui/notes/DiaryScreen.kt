package com.example.loom.ui.notes

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.loom.data.local.entity.DiaryNote
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.ui.home.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

// --- BLOCK EDITOR DATA MODEL ---
@Serializable
data class NoteBlock(
    val id: String,
    var type: String, // "text", "check", "image"
    var content: String = "",
    var isChecked: Boolean = false,
    var width: Float = 250f,
    var height: Float = 250f,
    var isCropped: Boolean = true
)

@Composable
fun DiaryScreen(viewModel: HomeViewModel) {
    val allNotes by viewModel.allDiaryNotes.collectAsState()
    val allTags by viewModel.allNoteTags.collectAsState()
    val viewMode by viewModel.diaryViewMode.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<DiaryNote?>(null) }

    if (isEditing) {
        NoteEditor(
            initialNote = noteToEdit,
            allTags = allTags,
            viewModel = viewModel,
            onNavigateBack = { isEditing = false; noteToEdit = null }
        )
    } else {
        NoteListScreen(
            notes = allNotes,
            allTags = allTags,
            viewMode = viewMode,
            viewModel = viewModel,
            onAddClick = { noteToEdit = null; isEditing = true },
            onNoteClick = { noteToEdit = it; isEditing = true },
            onDeleteNote = { viewModel.deleteDiaryNote(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    notes: List<DiaryNote>, allTags: List<NoteTag>, viewMode: Int, viewModel: HomeViewModel,
    onAddClick: () -> Unit, onNoteClick: (DiaryNote) -> Unit, onDeleteNote: (DiaryNote) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var noteToUnlock by remember { mutableStateOf<DiaryNote?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<NoteTag?>(null) }

    val filteredNotes = notes.filter {
        it.title.contains(searchQuery, true) || it.content.contains(searchQuery, true) || it.tag.contains(searchQuery, true)
    }

    val pinnedNotes = filteredNotes.filter { it.isPinned }
    val unpinnedNotes = filteredNotes.filter { !it.isPinned }

    if (noteToUnlock != null) {
        AlertDialog(
            onDismissRequest = { noteToUnlock = null; passwordInput = ""; passwordError = false },
            title = { Text("Private Note") },
            text = {
                Column {
                    Text("Enter PIN to unlock (Default: 0000)", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput, onValueChange = { passwordInput = it; passwordError = false },
                        isError = passwordError, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError) Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (passwordInput == "0000") { onNoteClick(noteToUnlock!!); noteToUnlock = null; passwordInput = ""; passwordError = false }
                    else passwordError = true
                }) { Text("Unlock") }
            },
            dismissButton = { TextButton(onClick = { noteToUnlock = null; passwordInput = ""; passwordError = false }) { Text("Cancel") } }
        )
    }

    if (tagToDelete != null) {
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("Delete Tag?") },
            text = { Text("Are you sure you want to delete '${tagToDelete?.name}'? Your notes will remain.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteNoteTag(tagToDelete!!); tagToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { tagToDelete = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Add, "Add Note", tint = MaterialTheme.colorScheme.onPrimary) }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Diary & Notes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

                IconButton(onClick = { viewModel.setDiaryViewMode((viewMode + 1) % 3) }) {
                    val icon = when (viewMode) { 0 -> Icons.Default.ViewAgenda; 1 -> Icons.Default.GridView; else -> Icons.Default.Window }
                    Icon(icon, contentDescription = "Toggle View")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Search notes...") }, leadingIcon = { Icon(Icons.Default.Search, "Search") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline, focusedBorderColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allTags) { tag ->
                    val tagColor = try { Color(android.graphics.Color.parseColor(tag.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                    FilterChip(
                        selected = searchQuery == tag.name,
                        onClick = { searchQuery = if (searchQuery == tag.name) "" else tag.name },
                        label = { Text(tag.name) },
                        modifier = Modifier.combinedClickable(onClick = { searchQuery = if (searchQuery == tag.name) "" else tag.name }, onLongClick = { tagToDelete = tag }),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = tagColor.copy(alpha = 0.2f), selectedLabelColor = tagColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("It's quiet here. Tap + to start.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                val gridColumns = when (viewMode) { 0 -> 1; 1 -> 2; else -> 3 }
                val cardHeight = when (viewMode) { 0 -> 140.dp; 1 -> 180.dp; else -> 160.dp } // Uniform height

                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pinned Section
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text("Pinned", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        }
                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCard(note, allTags, cardHeight, onNoteClick, onDeleteNote, { noteToUnlock = it })
                        }
                    }

                    // Unpinned Section
                    if (unpinnedNotes.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text("Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                        }
                        items(unpinnedNotes, key = { it.id }) { note ->
                            NoteCard(note, allTags, cardHeight, onNoteClick, onDeleteNote, { noteToUnlock = it })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(note: DiaryNote, allTags: List<NoteTag>, height: androidx.compose.ui.unit.Dp, onNoteClick: (DiaryNote) -> Unit, onDeleteNote: (DiaryNote) -> Unit, onUnlockRequest: (DiaryNote) -> Unit) {
    val tagObj = allTags.find { it.name == note.tag }
    val tagColor = try { tagObj?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } } catch (e: Exception) { null }
    val containerColor = tagColor?.copy(alpha = 0.2f) ?: MaterialTheme.colorScheme.primaryContainer

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { onDeleteNote(note); true } else false }
    )

    SwipeToDismissBox(
        state = dismissState, enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else Color.Transparent
            Box(modifier = Modifier.fillMaxSize().background(color, RoundedCornerShape(16.dp)).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onError)
            }
        }
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().height(height).clickable { if (note.isPrivate) onUnlockRequest(note) else onNoteClick(note) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        if (note.isPinned) Icon(Icons.Default.PushPin, "Pinned", modifier = Modifier.size(16.dp).padding(end = 4.dp), tint = MaterialTheme.colorScheme.primary)
                        if (note.isFavorite) Icon(Icons.Default.Favorite, "Fav", tint = Color.Red, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                        Text(note.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (note.isPrivate) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, "Private", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Locked", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                    }
                } else {
                    // Extract preview text from blocks
                    val blocks = try { Json.decodeFromString<List<NoteBlock>>(note.blocksJson) } catch (e: Exception) { emptyList() }
                    val previewText = blocks.filter { it.type == "text" || it.type == "check" }
                        .joinToString("\n") { if (it.type == "check") (if (it.isChecked) "[x] " else "[ ] ") + it.content else it.content }

                    Text(if (previewText.isBlank()) note.content else previewText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }

                Spacer(modifier = Modifier.weight(1f))
                Surface(color = containerColor, shape = RoundedCornerShape(8.dp)) {
                    Text(note.tag, style = MaterialTheme.typography.labelSmall, color = tagColor ?: MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteEditor(initialNote: DiaryNote?, allTags: List<NoteTag>, viewModel: HomeViewModel, onNavigateBack: () -> Unit) {
    var currentId by remember { mutableIntStateOf(initialNote?.id ?: 0) }
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var selectedTag by remember { mutableStateOf(initialNote?.tag ?: "Personal") }
    var isFavorite by remember { mutableStateOf(initialNote?.isFavorite ?: false) }
    var isPrivate by remember { mutableStateOf(initialNote?.isPrivate ?: false) }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }

    // --- BLOCK SYSTEM ---
    val initialBlocks = try { Json.decodeFromString<List<NoteBlock>>(initialNote?.blocksJson ?: "[]") } catch (e: Exception) { emptyList() }
    val blocks = remember { mutableStateListOf<NoteBlock>().apply {
        if (initialBlocks.isEmpty() && initialNote?.content?.isNotBlank() == true) add(NoteBlock(id = UUID.randomUUID().toString(), type = "text", content = initialNote.content))
        else if (initialBlocks.isEmpty()) add(NoteBlock(id = UUID.randomUUID().toString(), type = "text"))
        else addAll(initialBlocks)
    }}

    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val insertIndex = blocks.indexOfFirst { it.id == focusedBlockId }.takeIf { it >= 0 }?.plus(1) ?: blocks.size
            blocks.add(insertIndex, NoteBlock(id = UUID.randomUUID().toString(), type = "image", content = uri.toString()))
            blocks.add(insertIndex + 1, NoteBlock(id = UUID.randomUUID().toString(), type = "text")) // Auto add text block after image
        }
    }

    // Auto-Save
    LaunchedEffect(title, selectedTag, isFavorite, isPrivate, isPinned, blocks.toList()) {
        delay(500)
        if (title.isNotBlank() || blocks.any { it.content.isNotBlank() || it.type == "image" }) {
            val blocksJson = Json.encodeToString(blocks.toList())
            val returnedId = viewModel.saveDiaryNoteWithReturn(currentId, title, "", selectedTag, "[]", blocksJson, isFavorite, isPrivate, isPinned, false)
            if (currentId == 0) currentId = returnedId.toInt()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { isPinned = !isPinned }) { Icon(if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, "Pin", tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = { isPrivate = !isPrivate }) { Icon(if (isPrivate) Icons.Default.Lock else Icons.Outlined.Lock, "Toggle Private", tint = if (isPrivate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = { isFavorite = !isFavorite }) { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Toggle Favorite", tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                IconButton(onClick = { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Default.Image, "Add Image") }
                IconButton(onClick = {
                    val insertIndex = blocks.indexOfFirst { it.id == focusedBlockId }.takeIf { it >= 0 }?.plus(1) ?: blocks.size
                    blocks.add(insertIndex, NoteBlock(id = UUID.randomUUID().toString(), type = "check"))
                }) { Icon(Icons.Default.Checklist, "Add Checklist") }
                IconButton(onClick = {
                    val insertIndex = blocks.indexOfFirst { it.id == focusedBlockId }.takeIf { it >= 0 }?.plus(1) ?: blocks.size
                    blocks.add(insertIndex, NoteBlock(id = UUID.randomUUID().toString(), type = "text"))
                }) { Icon(Icons.Default.TextFields, "Add Text") }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus(); focusedBlockId = null }) }) {

            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                item { AssistChip(onClick = { showTagDialog = true }, label = { Text("+ New Tag") }) }
                items(allTags) { tag ->
                    val tagColor = try { Color(android.graphics.Color.parseColor(tag.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                    FilterChip(
                        selected = selectedTag == tag.name, onClick = { selectedTag = tag.name }, label = { Text(tag.name) },
                        modifier = Modifier.combinedClickable(onClick = { selectedTag = tag.name }, onLongClick = { viewModel.deleteNoteTag(tag) }),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = tagColor.copy(alpha = 0.2f), selectedLabelColor = tagColor)
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp))

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                placeholder = { Text("Title", style = MaterialTheme.typography.titleLarge) },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)
            )

            // --- BLOCK EDITOR RENDERER ---
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                itemsIndexed(blocks) { index, block ->
                    when (block.type) {
                        "text" -> {
                            OutlinedTextField(
                                value = block.content,
                                onValueChange = { newContent -> blocks[index] = block.copy(content = newContent) },
                                placeholder = { Text("Type something...") },
                                modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) focusedBlockId = block.id },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)
                            )
                        }
                        "check" -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                IconButton(onClick = { blocks[index] = block.copy(isChecked = !block.isChecked) }, modifier = Modifier.size(32.dp)) {
                                    Icon(if (block.isChecked) Icons.Outlined.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                OutlinedTextField(
                                    value = block.content,
                                    onValueChange = { newContent -> blocks[index] = block.copy(content = newContent) },
                                    modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) focusedBlockId = block.id },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(textDecoration = if (block.isChecked) TextDecoration.LineThrough else TextDecoration.None),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)
                                )
                                IconButton(onClick = { blocks.removeAt(index) }) { Icon(Icons.Default.Close, "Remove Check") }
                            }
                        }
                        "image" -> {
                            val isSelected = focusedBlockId == block.id
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.width(block.width.dp).height(block.height.dp).pointerInput(Unit) { detectTapGestures(onTap = { focusedBlockId = block.id; focusManager.clearFocus() }) }) {
                                    AsyncImage(
                                        model = block.content, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                        contentScale = if (block.isCropped) ContentScale.Crop else ContentScale.Fit
                                    )
                                    if (isSelected) {
                                        val blue = Color(0xFF4285F4)
                                        Box(modifier = Modifier.fillMaxSize().border(2.dp, blue, RoundedCornerShape(8.dp)))
                                        @Composable fun Dot(modifier: Modifier) { Box(modifier = modifier.size(10.dp).background(Color.White, CircleShape).border(2.dp, blue, CircleShape)) }

                                        Dot(Modifier.align(Alignment.TopStart).offset((-5).dp, (-5).dp))
                                        Dot(Modifier.align(Alignment.TopEnd).offset(5.dp, (-5).dp))
                                        Dot(Modifier.align(Alignment.BottomStart).offset((-5).dp, 5.dp))

                                        Box(modifier = Modifier.align(Alignment.BottomEnd).offset(5.dp, 5.dp).size(30.dp)
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val newW = (block.width + dragAmount.x / density.density).coerceIn(100f, 400f)
                                                    val newH = (block.height + dragAmount.y / density.density).coerceIn(100f, 600f)
                                                    blocks[index] = block.copy(width = newW, height = newH)
                                                }
                                            }
                                        ) { Dot(Modifier.align(Alignment.Center)) }
                                    }
                                }
                                if (isSelected) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF333333), modifier = Modifier.align(Alignment.TopCenter).offset(y = (-40).dp)) {
                                        Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                                            TextButton(onClick = { blocks.removeAt(index); focusedBlockId = null }) { Text("Delete", color = Color.White) }
                                            TextButton(onClick = { blocks[index] = block.copy(isCropped = !block.isCropped) }) { Text(if (block.isCropped) "Fit" else "Crop", color = Color.White) }
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