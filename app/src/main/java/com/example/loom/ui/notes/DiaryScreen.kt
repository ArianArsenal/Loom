package com.example.loom.ui.notes

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import com.example.loom.data.local.entity.DiaryNote
import com.example.loom.ui.home.HomeViewModel

@Composable
fun DiaryScreen(viewModel: HomeViewModel) {
    val allNotes by viewModel.allDiaryNotes.collectAsStateWithLifecycle()
    val allTags by viewModel.allNoteTags.collectAsStateWithLifecycle()
    val viewMode by viewModel.diaryViewMode.collectAsStateWithLifecycle()

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