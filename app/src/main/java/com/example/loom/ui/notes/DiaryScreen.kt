package com.example.loom.ui.notes

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.example.loom.data.local.entity.DiaryNote
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.ui.home.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

private const val ZWSP = "\u200B"

@Serializable
data class NoteBlock(
    val id: String,
    var type: String,
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
    var showPinFallback by remember { mutableStateOf(false) }

    // NEW UX STATE
    var showNoSecurityDialog by remember { mutableStateOf(false) }
    var isCardLocked by remember { mutableStateOf(false) }

    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<NoteTag?>(null) }

    val context = LocalContext.current
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    val filteredNotes = notes.filter {
        it.title.contains(searchQuery, true) || it.content.contains(searchQuery, true) || it.tag.contains(searchQuery, true)
    }

    val pinnedNotes = filteredNotes.filter { it.isPinned }
    val unpinnedNotes = filteredNotes.filter { !it.isPinned }

    // "Go to Settings" Warning Dialog
    if (showNoSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showNoSecurityDialog = false; isCardLocked = false },
            title = { Text("Security Not Configured", fontWeight = FontWeight.Bold) },
            text = { Text("This note is private, but you haven't set up an App PIN or Biometric unlock. Please go to Settings to configure your security.") },
            confirmButton = {
                Button(onClick = { showNoSecurityDialog = false; isCardLocked = false }) { Text("OK") }
            }
        )
    }

    // Fallback PIN Dialog
    if (showPinFallback && noteToUnlock != null) {
        val savedPin = com.example.loom.utils.SecurityManager.getPin(context)

        AlertDialog(
            onDismissRequest = { noteToUnlock = null; passwordInput = ""; passwordError = false; showPinFallback = false; isCardLocked = false },
            title = { Text("Private Note") },
            text = {
                Column {
                    // THE FIX: Check for isNullOrBlank
                    if (savedPin.isNullOrBlank()) {
                        Text("No PIN has been configured. Please go to Settings to set up an App PIN.", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Enter your custom PIN to unlock:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passwordInput, onValueChange = { passwordInput = it; passwordError = false },
                            isError = passwordError, visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        if (passwordError) Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                if (!savedPin.isNullOrBlank()) {
                    Button(onClick = {
                        if (passwordInput == savedPin) {
                            onNoteClick(noteToUnlock!!)
                            noteToUnlock = null
                            passwordInput = ""
                            passwordError = false
                            showPinFallback = false
                            isCardLocked = false // Unlock UI
                        } else passwordError = true
                    }) { Text("Unlock") }
                }
            },
            dismissButton = { TextButton(onClick = { noteToUnlock = null; passwordInput = ""; passwordError = false; showPinFallback = false; isCardLocked = false }) { Text("Cancel") } }
        )
    }

    if (tagToDelete != null) {
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("Delete Tag & Notes?") },
            text = { Text("Are you sure you want to delete '${tagToDelete?.name}'? This will permanently delete ALL notes associated with this tag.") },
            confirmButton = {
                Button(onClick = {
                    val associatedNotes = notes.filter { it.tag == tagToDelete!!.name }
                    associatedNotes.forEach { viewModel.deleteDiaryNote(it) }
                    viewModel.deleteNoteTag(tagToDelete!!)
                    if (searchQuery == tagToDelete?.name) searchQuery = ""
                    tagToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete All") }
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (searchQuery == tag.name) tagColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.combinedClickable(
                            onClick = { searchQuery = if (searchQuery == tag.name) "" else tag.name },
                            onLongClick = { tagToDelete = tag }
                        )
                    ) {
                        Text(tag.name, style = MaterialTheme.typography.labelMedium, color = if (searchQuery == tag.name) tagColor else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("It's quiet here. Tap + to start.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                val gridColumns = when (viewMode) { 0 -> 1; 1 -> 2; else -> 3 }
                val cardHeight = when (viewMode) { 0 -> 140.dp; 1 -> 180.dp; else -> 160.dp }

                val savedPin = com.example.loom.utils.SecurityManager.getPin(context)
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                val scope = rememberCoroutineScope()

                val handleUnlockRequest: (DiaryNote) -> Unit = { note ->
                    if (!isCardLocked) {
                        isCardLocked = true // Lock the UI
                        focusManager.clearFocus()

                        // THE FIX: Check if PIN is not null AND not empty
                        val hasPin = !savedPin.isNullOrBlank()
                        val hasBio = isBiometricEnabled && com.example.loom.utils.SecurityManager.hasBiometricHardware(context)

                        if (!hasPin && !hasBio) {
                            showNoSecurityDialog = true
                        } else if (hasBio) {
                            scope.launch {
                                kotlinx.coroutines.delay(100)
                                com.example.loom.utils.SecurityManager.authenticate(
                                    context = context,
                                    title = "Unlock Private Note",
                                    onSuccess = {
                                        onNoteClick(note)
                                        isCardLocked = false
                                    },
                                    onFallback = {
                                        if (hasPin) {
                                            noteToUnlock = note
                                            showPinFallback = true
                                        } else {
                                            showNoSecurityDialog = true
                                        }
                                    }
                                )
                            }
                        } else {
                            noteToUnlock = note
                            showPinFallback = true
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text("Pinned", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        }
                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCard(note, allTags, cardHeight, onNoteClick, onDeleteNote, handleUnlockRequest)
                        }
                    }

                    if (unpinnedNotes.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text("Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                        }
                        items(unpinnedNotes, key = { it.id }) { note ->
                            NoteCard(note, allTags, cardHeight, onNoteClick, onDeleteNote, handleUnlockRequest)
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
                    val blocks = try { Json.decodeFromString<List<NoteBlock>>(note.blocksJson) } catch (e: Exception) { emptyList() }
                    val previewText = blocks.filter { it.type == "text" || it.type == "check" }
                        .joinToString("\n") { if (it.type == "check") (if (it.isChecked) "[x] " else "[ ] ") + it.content.replace(ZWSP, "") else it.content.replace(ZWSP, "") }

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

    val initialBlocks = try { Json.decodeFromString<List<NoteBlock>>(initialNote?.blocksJson ?: "[]") } catch (e: Exception) { emptyList() }
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
            try { focusRequesters[id]?.requestFocus() } catch (e: Exception) {}
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
        delay(500)
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
                }) { Icon(Icons.Default.ArrowBack, "Back") }
            },
            actions = {
                IconButton(onClick = { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Default.Image, "Add Image", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {
                    val currentIndex = blocks.indexOfFirst { it.id == focusedBlockId }
                    if (currentIndex >= 0) {
                        val currentBlock = blocks[currentIndex]
                        if (currentBlock.type == "text") {
                            blocks[currentIndex] = currentBlock.copy(type = "check")
                        } else if (currentBlock.type == "check") {
                            blocks[currentIndex] = currentBlock.copy(type = "text")
                        } else {
                            val newBlockId = UUID.randomUUID().toString()
                            blocks.add(currentIndex + 1, NoteBlock(id = newBlockId, type = "check", content = ZWSP))
                            focusedBlockId = newBlockId
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
                        val tagColor = try { Color(android.graphics.Color.parseColor(tag.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

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
                                                        val currentIndex = blocks.indexOfFirst { it.id == block.id }
                                                        if (currentIndex >= 0) {
                                                            val newW = (block.width + dragAmount.x / density.density).coerceIn(100f, 400f)
                                                            val newH = (block.height + dragAmount.y / density.density).coerceIn(100f, 600f)
                                                            blocks[currentIndex] = block.copy(width = newW, height = newH)
                                                        }
                                                    }
                                                }
                                            ) { Dot(Modifier.align(Alignment.Center)) }
                                        }
                                    }
                                    if (isSelected) {
                                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF333333), modifier = Modifier.align(Alignment.TopCenter).offset(y = (-40).dp)) {
                                            Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                                                TextButton(onClick = {
                                                    val currentIndex = blocks.indexOfFirst { it.id == block.id }
                                                    if (currentIndex >= 0) blocks.removeAt(currentIndex)
                                                    focusedBlockId = null
                                                }) { Text("Delete", color = Color.White) }
                                                TextButton(onClick = {
                                                    val currentIndex = blocks.indexOfFirst { it.id == block.id }
                                                    if (currentIndex >= 0) blocks[currentIndex] = block.copy(isCropped = !block.isCropped)
                                                }) { Text(if (block.isCropped) "Fit" else "Crop", color = Color.White) }
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