package com.example.loom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.loom.data.models.SandboxSpace
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "notion_spaces")
data class NotionSpaceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val icon: String,
    val bannerUrl: String,
    val updatedAt: Long,
    val blocksJson: String,
    val isPinned: Boolean = false,
    val tagsJson: String = "[]",
    val isPrivate: Boolean = false,

    // 1. NEW: TRASH FLAG FOR DATABASE
    val isDeleted: Boolean = false
) {
    fun toDomainModel(): SandboxSpace {
        return SandboxSpace(
            id = id,
            title = title,
            icon = icon,
            bannerUrl = bannerUrl,
            updatedAt = updatedAt,
            isPinned = isPinned,
            tags = try { Json.decodeFromString(tagsJson) } catch (e: Exception) { emptyList() },
            blocks = try { Json.decodeFromString(blocksJson) } catch (e: Exception) { emptyList() },
            isPrivate = isPrivate,

            // 2. NEW: MAP FROM DATABASE TO APP
            isDeleted = isDeleted
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
                isPinned = space.isPinned,
                tagsJson = Json.encodeToString(space.tags),
                blocksJson = Json.encodeToString(space.blocks),
                isPrivate = space.isPrivate,

                // 3. NEW: MAP FROM APP TO DATABASE
                isDeleted = space.isDeleted
            )
        }
    }
}