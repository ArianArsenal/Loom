package com.example.loom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.loom.data.models.SandboxSpace
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// We use a new table name to prevent migration crashes from our previous code
@Entity(tableName = "notion_spaces")
data class NotionSpaceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val icon: String,
    val blocksJson: String
) {
    fun toDomainModel(): SandboxSpace {
        return SandboxSpace(
            id = id,
            title = title,
            icon = icon,
            blocks = try { Json.decodeFromString(blocksJson) } catch (e: Exception) { emptyList() }
        )
    }

    companion object {
        fun fromDomainModel(space: SandboxSpace): NotionSpaceEntity {
            return NotionSpaceEntity(
                id = space.id,
                title = space.title,
                icon = space.icon,
                blocksJson = Json.encodeToString(space.blocks)
            )
        }
    }
}