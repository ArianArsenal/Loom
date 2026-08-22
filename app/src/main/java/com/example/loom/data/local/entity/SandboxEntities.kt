package com.example.loom.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.loom.data.models.SandboxSpace
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.BlockType
import com.example.loom.ui.sandbox.buildTree
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// 1. The Cleaned Space Table (Notice `blocksJson` is completely gone!)
@Entity(tableName = "notion_spaces")
data class NotionSpaceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val icon: String,
    val bannerUrl: String,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val tagsJson: String = "[]",
    val isPrivate: Boolean = false,
    val isDeleted: Boolean = false
) {
    // Basic mapping without blocks
    fun toDomainModelWithoutBlocks(): SandboxSpace {
        return SandboxSpace(
            id = id, title = title, icon = icon, bannerUrl = bannerUrl, updatedAt = updatedAt,
            isPinned = isPinned, tags = try { Json.decodeFromString(tagsJson) } catch (_: Exception) { emptyList() }, // --- FIX: Unused e ---
            isPrivate = isPrivate, isDeleted = isDeleted, blocks = emptyList() // Will be injected by the Relation
        )
    }

    companion object {
        fun fromDomainModel(space: SandboxSpace): NotionSpaceEntity {
            return NotionSpaceEntity(
                id = space.id, title = space.title, icon = space.icon, bannerUrl = space.bannerUrl,
                updatedAt = space.updatedAt, isPinned = space.isPinned, tagsJson = Json.encodeToString(space.tags),
                isPrivate = space.isPrivate, isDeleted = space.isDeleted
            )
        }
    }
}

// 2. The New Block Table (Every block gets its own row for 1ms database saving)
@Entity(
    tableName = "notion_blocks",
    foreignKeys = [
        ForeignKey(
            entity = NotionSpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE // If you delete a space, SQLite nukes all blocks instantly
        )
    ],
    indices = [Index("spaceId"), Index("parentId")]
)
data class SandboxBlockEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val parentId: String?,
    val orderIndex: Int,
    val type: String,
    val content: String,
    val textFormatsJson: String,
    val secondaryContent: String,
    val icon: String,
    val url: String,
    val isChecked: Boolean,
    val isExpanded: Boolean,
    val progress: Float,
    val tableDataJson: String,
    val colWidthsJson: String,
    val rowHeightsJson: String,
    val calendarEventsJson: String,
    val kanbanColumnsJson: String,
    val sketchStrokesJson: String,
    val galleryItemsJson: String,
    val pinnedTab: String,
    val isLocked: Boolean
) {
    fun toDomainModel(): SandboxBlock {
        return SandboxBlock(
            id = id, type = BlockType.valueOf(type), content = content,
            textFormats = try { Json.decodeFromString(textFormatsJson) } catch(_: Exception){ emptyList() }, // --- FIX: Unused e ---
            secondaryContent = secondaryContent, icon = icon, url = url, isChecked = isChecked,
            isExpanded = isExpanded, progress = progress,
            tableData = try { Json.decodeFromString(tableDataJson) } catch(_: Exception){ emptyList() }, // --- FIX: Unused e ---
            colWidths = try { Json.decodeFromString(colWidthsJson) } catch(_: Exception){ emptyList() }, // --- FIX: Unused e ---
            rowHeights = try { Json.decodeFromString(rowHeightsJson) } catch(_: Exception){ emptyList() }, // --- FIX: Unused e ---
            calendarEvents = try { Json.decodeFromString(calendarEventsJson) } catch(_: Exception){ emptyMap() }, // --- FIX: Unused e ---
            kanbanColumns = try { Json.decodeFromString(kanbanColumnsJson) } catch(_: Exception){ emptyList() }, // --- FIX: Unused e ---
            sketchStrokes = try { Json.decodeFromString(sketchStrokesJson) } catch(_: Exception){ emptyList() }, // --- FIX: Unused e ---
            galleryItems = try { Json.decodeFromString(galleryItemsJson) } catch(_: Exception){ emptyList() }, // --- FIX: Unused e ---
            pinnedTab = pinnedTab, isLocked = isLocked, children = emptyList()
        )
    }
}

// 3. The Room Relation (Room automatically joins the Tables into one object)
data class SpaceWithBlocks(
    @Embedded val space: NotionSpaceEntity,
    @Relation(parentColumn = "id", entityColumn = "spaceId")
    val blocks: List<SandboxBlockEntity>
) {
    fun toDomainModel(): SandboxSpace {
        val baseSpace = space.toDomainModelWithoutBlocks()
        val allDomainBlocks = blocks.map { it.toDomainModel() }
        // Snap the flat list of rows back into your nested tree structure!
        val nestedBlocks = buildTree(allDomainBlocks, blocks)
        return baseSpace.copy(blocks = nestedBlocks)
    }
}