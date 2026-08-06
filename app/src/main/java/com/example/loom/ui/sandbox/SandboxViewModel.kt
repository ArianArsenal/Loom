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
    private val userPreferences: UserPreferences // <-- NEW: Injected to read security state
) : ViewModel() {

    // NEW: Expose biometric state to the Sandbox UI
    val isBiometricEnabled: StateFlow<Boolean> = userPreferences.biometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val allSpaces: StateFlow<List<SandboxSpace>> = sandboxDao.getAllSpaces()
        .map { entities -> entities.map { it.toDomainModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveSpace(space: SandboxSpace) {
        viewModelScope.launch { sandboxDao.saveSpace(NotionSpaceEntity.fromDomainModel(space)) }
    }

    fun deleteSpace(space: SandboxSpace) {
        viewModelScope.launch { sandboxDao.deleteSpace(NotionSpaceEntity.fromDomainModel(space)) }
    }
}

class SandboxViewModelFactory(
    private val sandboxDao: SandboxDao,
    private val userPreferences: UserPreferences // <-- NEW: Update your MainActivity/NavGraph to pass this!
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SandboxViewModel::class.java)) {
            return SandboxViewModel(sandboxDao, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}