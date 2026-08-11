package com.example.loom.ui.sandbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.dao.SandboxDao
import com.example.loom.data.local.entity.NotionSpaceEntity
import com.example.loom.data.models.SandboxSpace
import com.example.loom.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// THE FIX: Hilt will now force this ViewModel to share the EXACT SAME database instance as the Home tab!
@HiltViewModel
class SandboxViewModel @Inject constructor(
    private val sandboxDao: SandboxDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val isBiometricEnabled: StateFlow<Boolean> = userPreferences.biometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val allSpaces: StateFlow<List<SandboxSpace>> = sandboxDao.getActiveSpaces()
        .map { entities -> entities.map { it.toDomainModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedSpaces: StateFlow<List<SandboxSpace>> = sandboxDao.getTrashedSpaces()
        .map { entities -> entities.map { it.toDomainModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var saveJob: Job? = null
    private var pendingSpaceToSave: SandboxSpace? = null

    fun saveSpace(space: SandboxSpace) {
        pendingSpaceToSave = space.copy(updatedAt = System.currentTimeMillis())
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            executePendingSave()
        }
    }

    private fun executePendingSave() {
        val spaceToSave = pendingSpaceToSave ?: return
        pendingSpaceToSave = null
        executeInstantSave(spaceToSave)
    }

    private fun executeInstantSave(space: SandboxSpace) {
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
            try {
                val spaceEntity = NotionSpaceEntity.fromDomainModel(space.copy(updatedAt = System.currentTimeMillis()))
                val blockEntities = flattenTree(space.blocks, space.id)
                sandboxDao.saveSpaceWithBlocks(spaceEntity, blockEntities)
            } catch (e: Exception) {
                android.util.Log.e("LoomDatabase", "Fatal error saving space", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        executePendingSave()
    }

    fun moveToTrash(space: SandboxSpace) {
        saveJob?.cancel()
        pendingSpaceToSave = null
        executeInstantSave(space.copy(isDeleted = true))
    }

    fun restoreSpace(space: SandboxSpace) {
        saveJob?.cancel()
        pendingSpaceToSave = null
        executeInstantSave(space.copy(isDeleted = false))
    }

    fun permanentlyDeleteSpace(space: SandboxSpace) {
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
            sandboxDao.permanentlyDeleteSpace(NotionSpaceEntity.fromDomainModel(space))
        }
    }

    fun emptyTrash() {
        viewModelScope.launch { sandboxDao.emptyTrash() }
    }
}

// NOTICE: The Factory class has been permanently deleted from the bottom of this file.