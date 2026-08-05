package com.example.loom.data.local.dao

import androidx.room.*
import com.example.loom.data.local.entity.NotionSpaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SandboxDao {
    @Query("SELECT * FROM notion_spaces")
    fun getAllSpaces(): Flow<List<NotionSpaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSpace(space: NotionSpaceEntity)

    @Delete
    suspend fun deleteSpace(space: NotionSpaceEntity)


    @Query("DELETE FROM notion_spaces")
    suspend fun wipeAllSpaces()
}