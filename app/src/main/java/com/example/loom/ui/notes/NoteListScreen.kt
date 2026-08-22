package com.example.loom.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Window
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager // --- FIX ---
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt // --- FIX ---
import com.example.loom.data.local.entity.DiaryNote
import com.example.loom.data.local.entity.NoteTag
import com.example.loom.ui.home.HomeViewModel
import com.example.loom.util.SecurityManager // --- FIX ---
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat // --- FIX ---
import java.util.Date // --- FIX ---
import java.util.Locale // --- FIX ---
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    notes: List<DiaryNote>, allTags: List<NoteTag>, viewMode: Int, viewModel: HomeViewModel,
    onAddClick: () -> Unit, onNoteClick: (DiaryNote) -> Unit, onDeleteNote: (DiaryNote) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var noteToUnlock by remember { mutableStateOf<DiaryNote?>(null) }
    var showPinFallback by remember { mutableStateOf(false) }

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

    if (showPinFallback && noteToUnlock != null) {
        val savedPin = SecurityManager.getPin(context)

        AlertDialog(
            onDismissRequest = { noteToUnlock = null; passwordInput = ""; passwordError = false; showPinFallback = false; isCardLocked = false },
            title = { Text("Private Note") },
            text = {
                Column {
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
                            isCardLocked = false
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
                    val tagColor = try { Color(tag.colorHex.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
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

                val savedPin = SecurityManager.getPin(context)
                val focusManager = LocalFocusManager.current
                val scope = rememberCoroutineScope()

                val handleUnlockRequest: (DiaryNote) -> Unit = { note ->
                    if (!isCardLocked) {
                        isCardLocked = true
                        focusManager.clearFocus()

                        val hasPin = !savedPin.isNullOrBlank()
                        val hasBio = isBiometricEnabled && SecurityManager.hasBiometricHardware(context)

                        if (!hasPin && !hasBio) {
                            showNoSecurityDialog = true
                        } else if (hasBio) {
                            scope.launch {
                                delay(100.milliseconds) // --- FIX ---
                                SecurityManager.authenticate(
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
    val tagColor = try { tagObj?.colorHex?.let { Color(it.toColorInt()) } } catch (_: Exception) { null } // --- FIX ---
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
                        Text(note.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) // --- FIX ---
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
                    val blocks = try { Json.decodeFromString<List<NoteBlock>>(note.blocksJson) } catch (_: Exception) { emptyList() }
                    val previewText = blocks.filter { it.type == "text" || it.type == "check" }
                        .joinToString("\n") { if (it.type == "check") (if (it.isChecked) "[x] " else "[ ] ") + it.content.replace(ZWSP, "") else it.content.replace(ZWSP, "") }

                    Text(previewText.ifBlank { note.content }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis) // --- FIX ---
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Surface(color = containerColor, shape = RoundedCornerShape(8.dp)) {
                        Text(note.tag, style = MaterialTheme.typography.labelSmall, color = tagColor ?: MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }

                    val timeStr = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()).format(Date(note.timestamp)) // --- FIX ---
                    Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f))
                }
            }
        }
    }
}