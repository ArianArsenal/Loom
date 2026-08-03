package com.example.loom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.loom.data.models.SandboxSpace
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.lang.reflect.Modifier.isPrivate

@Entity(tableName = "notion_spaces") // Keep whatever table name you currently have
data class NotionSpaceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val icon: String,
    val bannerUrl: String,
    val updatedAt: Long,
    val blocksJson: String,

    // 1. ADD THESE TWO NEW FIELDS TO THE DATABASE TABLE
    val isPinned: Boolean = false,
    val tagsJson: String = "[]",
    val isPrivate: Boolean = false
) {
    fun toDomainModel(): SandboxSpace {
        return SandboxSpace(
            id = id,
            title = title,
            icon = icon,
            bannerUrl = bannerUrl,
            updatedAt = updatedAt,


            // 2. READ THE DATA FROM THE DATABASE
            isPinned = isPinned,
            tags = try { Json.decodeFromString(tagsJson) } catch (e: Exception) { emptyList() },

            blocks = try { Json.decodeFromString(blocksJson) } catch (e: Exception) { emptyList() },
            isPrivate = isPrivate
        )
    }

    companion object {
        fun fromDomainModel(space: SandboxSpace): NotionSpaceEntity {
            return NotionSpaceEntity(
                id = space.id,
                title = space.title,
                icon = space.icon,
                bannerUrl = space.bannerUrl,
                updatedAt = space.updatedAt,

                // 3. WRITE THE DATA TO THE DATABASE
                isPinned = space.isPinned,
                tagsJson = Json.encodeToString(space.tags),

                blocksJson = Json.encodeToString(space.blocks),
                isPrivate = space.isPrivate
            )
        }
    }
}