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

    @Update
    suspend fun updateSingleBlock(block: SandboxBlockEntity)
}