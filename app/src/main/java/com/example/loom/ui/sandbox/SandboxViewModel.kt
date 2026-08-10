package com.example.loom.ui.sandbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.dao.SandboxDao
import com.example.loom.data.local.entity.NotionSpaceEntity
import com.example.loom.data.models.SandboxSpace
import com.example.loom.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SandboxViewModel(
    private val sandboxDao: SandboxDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val isBiometricEnabled: StateFlow<Boolean> = userPreferences.biometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Now uses getActiveSpaces()
    val allSpaces: StateFlow<List<SandboxSpace>> = sandboxDao.getActiveSpaces()
        .map { entities -> entities.map { it.toDomainModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // NEW: Trashed Spaces state
    val trashedSpaces: StateFlow<List<SandboxSpace>> = sandboxDao.getTrashedSpaces()
        .map { entities -> entities.map { it.toDomainModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveSpace(space: SandboxSpace) {
        viewModelScope.launch { sandboxDao.saveSpace(NotionSpaceEntity.fromDomainModel(space)) }
    }

    // NEW: Soft Delete
    fun moveToTrash(space: SandboxSpace) {
        saveSpace(space.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
    }

    // NEW: Restore
    fun restoreSpace(space: SandboxSpace) {
        saveSpace(space.copy(isDeleted = false, updatedAt = System.currentTimeMillis()))
    }

    // UPDATE: Hard Delete
    fun permanentlyDeleteSpace(space: SandboxSpace) {
        viewModelScope.launch { sandboxDao.permanentlyDeleteSpace(NotionSpaceEntity.fromDomainModel(space)) }
    }

    fun emptyTrash() {
        viewModelScope.launch { sandboxDao.emptyTrash() }
    }
}

class SandboxViewModelFactory(
    private val sandboxDao: SandboxDao,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SandboxViewModel::class.java)) {
            return SandboxViewModel(sandboxDao, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}