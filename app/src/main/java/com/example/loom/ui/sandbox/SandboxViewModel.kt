package com.example.loom.ui.sandbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.dao.SandboxDao
import com.example.loom.data.local.entity.NotionSpaceEntity
import com.example.loom.data.models.SandboxSpace
import com.example.loom.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SandboxViewModel @Inject constructor(
    private val sandboxDao: SandboxDao,
    userPreferences: UserPreferences
) : ViewModel() {

    val isBiometricEnabled: StateFlow<Boolean> = userPreferences.biometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Decoding every block's JSON is heavy: keep it off the main thread
    val allSpaces: StateFlow<List<SandboxSpace>> = sandboxDao.getActiveSpaces()
        .map { entities -> entities.map { it.toDomainModel() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Spaces without their blocks: for navigation, link pickers and anything that only needs titles. */
    val spaceSummaries: StateFlow<List<SandboxSpace>> = sandboxDao.getActiveSpaceRows()
        .map { rows -> rows.map { it.toDomainModelWithoutBlocks() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Only this space's blocks are loaded and decoded while its page is open. */
    fun observeSpace(spaceId: String): Flow<SandboxSpace?> = sandboxDao.observeSpace(spaceId)
        .map { it?.toDomainModel() }
        .flowOn(Dispatchers.Default)

    val trashedSpaces: StateFlow<List<SandboxSpace>> = sandboxDao.getTrashedSpaces()
        .map { entities -> entities.map { it.toDomainModel() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // One debounce slot per space: saving space B must never discard a pending edit to space A
    private val pendingSaves = mutableMapOf<String, SandboxSpace>()
    private val saveJobs = mutableMapOf<String, Job>()

    fun saveSpace(space: SandboxSpace) {
        pendingSaves[space.id] = space.copy(updatedAt = System.currentTimeMillis())
        saveJobs[space.id]?.cancel()
        saveJobs[space.id] = viewModelScope.launch {
            delay(500.milliseconds)
            flushPending(space.id)
        }
    }

    /** Writes a brand-new space without the debounce so it exists by the time we navigate into it. */
    fun createSpace(space: SandboxSpace) {
        enqueueWrite(space.copy(updatedAt = System.currentTimeMillis()))
    }

    /** Bumps "last opened" without rewriting every block of the space. */
    fun touchSpace(spaceId: String) {
        val now = System.currentTimeMillis()
        writes.trySend { sandboxDao.touchSpace(spaceId, now) }
    }

    private fun flushPending(spaceId: String) {
        saveJobs.remove(spaceId)?.cancel()
        pendingSaves.remove(spaceId)?.let { enqueueWrite(it) }
    }

    override fun onCleared() {
        super.onCleared()
        pendingSaves.keys.toList().forEach { flushPending(it) }
    }

    fun moveToTrash(space: SandboxSpace) {
        // Keep any not-yet-saved edits to this space instead of overwriting them with a stale copy
        val latest = takePending(space)
        enqueueWrite(latest.copy(isDeleted = true))
    }

    fun restoreSpace(space: SandboxSpace) {
        val latest = takePending(space)
        enqueueWrite(latest.copy(isDeleted = false))
    }

    private fun takePending(space: SandboxSpace): SandboxSpace {
        saveJobs.remove(space.id)?.cancel()
        return pendingSaves.remove(space.id) ?: space
    }

    fun permanentlyDeleteSpace(space: SandboxSpace) {
        takePending(space)
        writes.trySend {
            sandboxDao.permanentlyDeleteSpace(NotionSpaceEntity.fromDomainModel(space))
        }
    }

    fun emptyTrash() {
        writes.trySend { sandboxDao.emptyTrash() }
    }

    private fun enqueueWrite(space: SandboxSpace) {
        writes.trySend {
            val spaceEntity = NotionSpaceEntity.fromDomainModel(space)
            val blockEntities = flattenTree(space.blocks, space.id)
            sandboxDao.syncSpaceWithBlocks(spaceEntity, blockEntities)
        }
    }

    companion object {
        /**
         * All space writes go through one FIFO queue on a process-wide scope, so they
         * survive leaving the screen and an older save can never land after a newer one.
         */
        private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

        init {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                for (write in writes) {
                    try {
                        write()
                    } catch (e: Exception) {
                        android.util.Log.e("LoomDatabase", "Fatal error saving space", e)
                    }
                }
            }
        }
    }
}
