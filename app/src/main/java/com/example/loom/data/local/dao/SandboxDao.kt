package com.example.loom.data.local.dao

import androidx.room.*
import com.example.loom.data.local.entity.NotionSpaceEntity
import com.example.loom.data.local.entity.SandboxBlockEntity
import com.example.loom.data.local.entity.SpaceWithBlocks
import kotlinx.coroutines.flow.Flow

@Dao
interface SandboxDao {
    // 1. Returns the combined Space + Blocks
    @Transaction
    @Query("SELECT * FROM notion_spaces WHERE isDeleted = 0")
    fun getActiveSpaces(): Flow<List<SpaceWithBlocks>>

    @Transaction
    @Query("SELECT * FROM notion_spaces WHERE isDeleted = 1")
    fun getTrashedSpaces(): Flow<List<SpaceWithBlocks>>

    @Transaction
    @Query("SELECT * FROM notion_spaces")
    suspend fun getAllSpacesOnce(): List<SpaceWithBlocks>

    /** Space rows only (no blocks): cheap enough to observe for lists and link pickers. */
    @Query("SELECT * FROM notion_spaces WHERE isDeleted = 0")
    fun getActiveSpaceRows(): Flow<List<NotionSpaceEntity>>

    /** One space with its blocks, for the canvas. */
    @Transaction
    @Query("SELECT * FROM notion_spaces WHERE id = :spaceId")
    fun observeSpace(spaceId: String): Flow<SpaceWithBlocks?>

    /** Spaces containing a link block that points at [spaceId]. */
    @Query("""
        SELECT DISTINCT s.* FROM notion_spaces s JOIN notion_blocks b ON b.spaceId = s.id
        WHERE b.type = 'SPACE' AND b.url = :spaceId AND s.isDeleted = 0 AND s.id != :spaceId
    """)
    fun getBacklinks(spaceId: String): Flow<List<NotionSpaceEntity>>

    @Query("SELECT * FROM notion_blocks WHERE spaceId = :spaceId")
    suspend fun getBlocksForSpace(spaceId: String): List<SandboxBlockEntity>

    @Query("DELETE FROM notion_blocks WHERE spaceId = :spaceId AND id IN (:blockIds)")
    suspend fun deleteBlocks(spaceId: String, blockIds: List<String>)

    // Upsert, not REPLACE: REPLACE deletes the space row first, which cascades to all its blocks
    @Upsert
    suspend fun upsertSpace(space: NotionSpaceEntity)

    @Upsert
    suspend fun upsertBlocks(blocks: List<SandboxBlockEntity>)

    /**
     * Saves a space by writing only blocks that changed and deleting only removed ones, instead of
     * deleting and re-inserting every block of the space on each autosave.
     */
    @Transaction
    suspend fun syncSpaceWithBlocks(space: NotionSpaceEntity, blocks: List<SandboxBlockEntity>) {
        upsertSpace(space)
        val existing = getBlocksForSpace(space.id).associateBy { it.id }
        val newIds = blocks.mapTo(HashSet()) { it.id }
        existing.keys.filter { it !in newIds }.chunked(500).forEach { deleteBlocks(space.id, it) }
        val changed = blocks.filter { existing[it.id] != it }
        if (changed.isNotEmpty()) upsertBlocks(changed)
    }

    // 2. Base Saves
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: NotionSpaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<SandboxBlockEntity>)

    @Query("DELETE FROM notion_blocks WHERE spaceId = :spaceId")
    suspend fun deleteBlocksForSpace(spaceId: String)

    // 3. THE MASTER SAVE (Runs instantly inside SQLite)
    @Transaction
    suspend fun saveSpaceWithBlocks(space: NotionSpaceEntity, blocks: List<SandboxBlockEntity>) {
        insertSpace(space)
        deleteBlocksForSpace(space.id) // Clears out old blocks
        insertBlocks(blocks)           // Injects new blocks
    }

    @Delete
    suspend fun permanentlyDeleteSpace(space: NotionSpaceEntity)

    @Query("DELETE FROM notion_spaces WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM notion_spaces")
    suspend fun wipeAllSpaces()

    @Query("UPDATE notion_spaces SET updatedAt = :updatedAt WHERE id = :spaceId")
    suspend fun touchSpace(spaceId: String, updatedAt: Long)

    @Update
    suspend fun updateSingleBlock(block: SandboxBlockEntity)
}