package com.example.loom.data.local.dao

import androidx.room.*
import com.example.loom.data.local.entity.NotionSpaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SandboxDao {
    // Only fetch spaces that are NOT in the trash
    @Query("SELECT * FROM notion_spaces WHERE isDeleted = 0")
    fun getActiveSpaces(): Flow<List<NotionSpaceEntity>>

    // Fetch spaces that ARE in the trash
    @Query("SELECT * FROM notion_spaces WHERE isDeleted = 1")
    fun getTrashedSpaces(): Flow<List<NotionSpaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSpace(space: NotionSpaceEntity)

    // This is now used for Permanent Deletion
    @Delete
    suspend fun permanentlyDeleteSpace(space: NotionSpaceEntity)

    @Query("DELETE FROM notion_spaces WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM notion_spaces")
    suspend fun wipeAllSpaces()
}