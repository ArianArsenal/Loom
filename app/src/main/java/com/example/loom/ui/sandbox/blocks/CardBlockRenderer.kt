package com.example.loom.ui.sandbox.blocks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.edit // --- FIX: Extracted specific library import ---
import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import com.example.loom.ui.sandbox.BlockRenderer


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardBlockRenderer(
    currentBlock: SandboxBlock,
    textState: TextFieldValue,
    handleTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier, // --- FIX: Renamed from 'commonModifier' ---
    placeholderUI: @Composable (@Composable () -> Unit) -> Unit,
    isWidgetMode: Boolean,
    isReadOnly: Boolean,
    allSpaces: List<SandboxSpace>,
    currentSpaceId: String,
    recentTableColors: MutableList<Color>,
    focusRequesters: MutableMap<String, FocusRequester>,
    focusTargetId: String?,
    allBlocks: List<SandboxBlock>,
    onScrollToBlock: (String) -> Unit,
    onFocusRequest: (String) -> Unit,
    onScrollToTop: () -> Unit,
    onNavigateToSpace: (String) -> Unit,
    onCreateLinkedSpace: () -> String,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    if (isWidgetMode) {
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(value = textState, onValueChange = handleTextChange, textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant), modifier = modifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = placeholderUI, readOnly = isReadOnly)
                    if (currentBlock.isLocked) { Icon(Icons.Default.Lock, "Locked", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(16.dp)) }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                Column(modifier = Modifier.fillMaxWidth()) {
                    currentBlock.children.forEachIndexed { childIndex, childBlock ->
                        val cNumberedIndex = if (childBlock.type == BlockType.NUMBER) {
                            var cCount = 1
                            for (i in childIndex - 1 downTo 0) { if (currentBlock.children[i].type == BlockType.NUMBER) cCount++ else break }
                            cCount
                        } else 1

                        key(childBlock.id) {
                            BlockRenderer(
                                block = childBlock, numberIndex = cNumberedIndex, allSpaces = allSpaces, currentSpaceId = currentSpaceId, recentTableColors = recentTableColors, focusRequesters = focusRequesters, focusTargetId = focusTargetId, allBlocks = allBlocks, isWidgetMode = isWidgetMode, isReadOnly = isReadOnly || currentBlock.isLocked, onScrollToBlock = onScrollToBlock,
                                onMove = { offset ->
                                    val currentIndex = currentBlock.children.indexOfFirst { it.id == childBlock.id }
                                    if (currentIndex != -1) {
                                        val newIndex = currentIndex + offset
                                        if (newIndex in currentBlock.children.indices) { // --- FIX: Replaced bounds check with indices ---
                                            val newChildren = currentBlock.children.toMutableList()
                                            val temp = newChildren[currentIndex]
                                            newChildren[currentIndex] = newChildren[newIndex]
                                            newChildren[newIndex] = temp
                                            onUpdate(currentBlock.copy(children = newChildren))
                                        }
                                    }
                                },
                                onFocusRequest = onFocusRequest, onScrollToTop = onScrollToTop, onNavigateToSpace = onNavigateToSpace, onCreateLinkedSpace = onCreateLinkedSpace,
                                onUpdate = { updatedChild ->
                                    val newChildren = currentBlock.children.toMutableList()
                                    val safeIndex = newChildren.indexOfFirst { it.id == updatedChild.id }
                                    if (safeIndex != -1) { newChildren[safeIndex] = updatedChild; onUpdate(currentBlock.copy(children = newChildren)) }
                                },
                                onSplitBlock = { currentContent, nextType, nextContent ->
                                    val newChildren = currentBlock.children.toMutableList()
                                    val safeIndex = newChildren.indexOfFirst { it.id == childBlock.id }
                                    if (safeIndex != -1) {
                                        newChildren[safeIndex] = newChildren[safeIndex].copy(content = currentContent)
                                        val newBlockId = java.util.UUID.randomUUID().toString()
                                        newChildren.add(safeIndex + 1, SandboxBlock(id = newBlockId, type = nextType, content = nextContent))
                                        onUpdate(currentBlock.copy(children = newChildren))
                                        onFocusRequest(newBlockId)
                                    }
                                },
                                onDeleteBlock = { remainder, deleteEntirelyFlag -> // --- FIX: Renamed parameter to prevent shadowing ---
                                    val newChildren = currentBlock.children.toMutableList()
                                    val safeIndex = newChildren.indexOfFirst { it.id == childBlock.id }
                                    if (safeIndex != -1 && safeIndex < newChildren.size) {
                                        if (newChildren.size == 1 && safeIndex == 0 && (deleteEntirelyFlag || remainder.trim().replace("\u200B","").isEmpty())) {
                                            newChildren[0] = newChildren[0].copy(type = BlockType.TEXT, content = "\u200B")
                                            onUpdate(currentBlock.copy(children = newChildren))
                                            onFocusRequest(newChildren[0].id)
                                        } else {
                                            if (deleteEntirelyFlag) {
                                                val prevId = if (safeIndex > 0) newChildren[safeIndex - 1].id else currentBlock.id
                                                newChildren.removeAt(safeIndex)
                                                onUpdate(currentBlock.copy(children = newChildren))
                                                onFocusRequest(prevId)
                                                try { focusRequesters[prevId]?.requestFocus() } catch(_: Exception){} // --- FIX ---
                                            } else if (safeIndex > 0) {
                                                val prevChild = newChildren[safeIndex - 1]
                                                val isPrevTextBased = prevChild.type in listOf(BlockType.TEXT, BlockType.HEADING, BlockType.HEADING_2, BlockType.HEADING_3, BlockType.HEADING_4, BlockType.BULLET, BlockType.NUMBER, BlockType.TODO, BlockType.QUOTE, BlockType.CALLOUT, BlockType.TOGGLE)
                                                if (isPrevTextBased) {
                                                    val prevId = prevChild.id
                                                    newChildren[safeIndex - 1] = prevChild.copy(content = prevChild.content + remainder)
                                                    newChildren.removeAt(safeIndex)
                                                    onUpdate(currentBlock.copy(children = newChildren))
                                                    onFocusRequest(prevId)
                                                    try { focusRequesters[prevId]?.requestFocus() } catch(_: Exception){} // --- FIX ---
                                                } else {
                                                    newChildren.removeAt(safeIndex - 1)
                                                    onUpdate(currentBlock.copy(children = newChildren))
                                                }
                                            } else if (safeIndex == 0) {
                                                // --- FIX: The "always false" warning pointed out that 'deleteEntirelyFlag' was checked above, meaning it is impossible to be true here ---
                                                if (remainder.trim().replace("\u200B","").isEmpty()) {
                                                    newChildren.removeAt(0)
                                                    onUpdate(currentBlock.copy(children = newChildren))
                                                    onFocusRequest(currentBlock.id)
                                                } else {
                                                    newChildren[0] = newChildren[0].copy(content = "\u200B" + remainder)
                                                    onUpdate(currentBlock.copy(children = newChildren))
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    } else {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(value = textState, onValueChange = handleTextChange, textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground), modifier = modifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = placeholderUI, readOnly = isReadOnly)
                    IconButton(onClick = { onUpdate(currentBlock.copy(isLocked = !currentBlock.isLocked)) }, modifier = Modifier.size(32.dp)) { Icon(if (currentBlock.isLocked) Icons.Default.Lock else Icons.Outlined.LockOpen, "Lock", tint = if (currentBlock.isLocked) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(20.dp)) }
                    var showPinMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showPinMenu = true }, modifier = Modifier.size(32.dp)) { Icon(if (currentBlock.pinnedTab.isNotBlank()) Icons.Default.PushPin else Icons.Outlined.PushPin, "Pin", tint = if (currentBlock.pinnedTab.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(20.dp)) }
                    DropdownMenu(expanded = showPinMenu, onDismissRequest = { showPinMenu = false }) {
                        val tabsList = currentBlock.pinnedTab.split(",").filter { it.isNotBlank() }.toMutableSet()
                        val isHome = tabsList.contains("Home")
                        val isHealth = tabsList.contains("Health")
                        DropdownMenuItem(text = { Text("Home") }, trailingIcon = { if (isHome) Icon(Icons.Default.Check, null) }, onClick = { if (isHome) tabsList.remove("Home") else tabsList.add("Home"); onUpdate(currentBlock.copy(pinnedTab = tabsList.joinToString(","))); showPinMenu = false })
                        DropdownMenuItem(text = { Text("Health") }, trailingIcon = { if (isHealth) Icon(Icons.Default.Check, null) }, onClick = { if (isHealth) tabsList.remove("Health") else tabsList.add("Health"); onUpdate(currentBlock.copy(pinnedTab = tabsList.joinToString(","))); showPinMenu = false })
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = {
                        val prefs = context.getSharedPreferences("loom_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit { putBoolean("deleted_card_${currentBlock.id}", true) } // --- FIX: KTX Extension ---
                        onDeleteBlock("", true)
                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
                    currentBlock.children.forEachIndexed { childIndex, childBlock ->
                        val cNumberedIndex = if (childBlock.type == BlockType.NUMBER) {
                            var cCount = 1
                            for (i in childIndex - 1 downTo 0) { if (currentBlock.children[i].type == BlockType.NUMBER) cCount++ else break }
                            cCount
                        } else 1

                        key(childBlock.id) {
                            BlockRenderer(
                                block = childBlock, numberIndex = cNumberedIndex, allSpaces = allSpaces, currentSpaceId = currentSpaceId, recentTableColors = recentTableColors, focusRequesters = focusRequesters, focusTargetId = focusTargetId, allBlocks = allBlocks, isWidgetMode = isWidgetMode, isReadOnly = isReadOnly || currentBlock.isLocked, onScrollToBlock = onScrollToBlock,
                                onMove = { offset ->
                                    val currentIndex = currentBlock.children.indexOfFirst { it.id == childBlock.id }
                                    if (currentIndex != -1) {
                                        val newIndex = currentIndex + offset
                                        if (newIndex in currentBlock.children.indices) { // --- FIX: Indices optimization ---
                                            val newChildren = currentBlock.children.toMutableList()
                                            val temp = newChildren[currentIndex]
                                            newChildren[currentIndex] = newChildren[newIndex]
                                            newChildren[newIndex] = temp
                                            onUpdate(currentBlock.copy(children = newChildren))
                                        }
                                    }
                                },
                                onFocusRequest = onFocusRequest, onScrollToTop = onScrollToTop, onNavigateToSpace = onNavigateToSpace, onCreateLinkedSpace = onCreateLinkedSpace,
                                onUpdate = { updatedChild ->
                                    val newChildren = currentBlock.children.toMutableList()
                                    val safeIndex = newChildren.indexOfFirst { it.id == updatedChild.id }
                                    if (safeIndex != -1) { newChildren[safeIndex] = updatedChild; onUpdate(currentBlock.copy(children = newChildren)) }
                                },
                                onSplitBlock = { currentContent, nextType, nextContent ->
                                    val newChildren = currentBlock.children.toMutableList()
                                    val safeIndex = newChildren.indexOfFirst { it.id == childBlock.id }
                                    if (safeIndex != -1) {
                                        newChildren[safeIndex] = newChildren[safeIndex].copy(content = currentContent)
                                        val newBlockId = java.util.UUID.randomUUID().toString()
                                        newChildren.add(safeIndex + 1, SandboxBlock(id = newBlockId, type = nextType, content = nextContent))
                                        onUpdate(currentBlock.copy(children = newChildren))
                                        onFocusRequest(newBlockId)
                                    }
                                },
                                onDeleteBlock = { remainder, deleteEntirelyFlag ->
                                    val newChildren = currentBlock.children.toMutableList()
                                    val safeIndex = newChildren.indexOfFirst { it.id == childBlock.id }
                                    if (safeIndex != -1 && safeIndex < newChildren.size) {
                                        if (newChildren.size == 1 && safeIndex == 0 && (deleteEntirelyFlag || remainder.trim().replace("\u200B","").isEmpty())) {
                                            newChildren[0] = newChildren[0].copy(type = BlockType.TEXT, content = "\u200B")
                                            onUpdate(currentBlock.copy(children = newChildren))
                                            onFocusRequest(newChildren[0].id)
                                        } else {
                                            if (deleteEntirelyFlag) {
                                                val prevId = if (safeIndex > 0) newChildren[safeIndex - 1].id else currentBlock.id
                                                newChildren.removeAt(safeIndex)
                                                onUpdate(currentBlock.copy(children = newChildren))
                                                onFocusRequest(prevId)
                                                try { focusRequesters[prevId]?.requestFocus() } catch(_: Exception){} // --- FIX ---
                                            } else if (safeIndex > 0) {
                                                val prevChild = newChildren[safeIndex - 1]
                                                val isPrevTextBased = prevChild.type in listOf(BlockType.TEXT, BlockType.HEADING, BlockType.HEADING_2, BlockType.HEADING_3, BlockType.HEADING_4, BlockType.BULLET, BlockType.NUMBER, BlockType.TODO, BlockType.QUOTE, BlockType.CALLOUT, BlockType.TOGGLE)
                                                if (isPrevTextBased) {
                                                    val prevId = prevChild.id
                                                    newChildren[safeIndex - 1] = prevChild.copy(content = prevChild.content + remainder)
                                                    newChildren.removeAt(safeIndex)
                                                    onUpdate(currentBlock.copy(children = newChildren))
                                                    onFocusRequest(prevId)
                                                    try { focusRequesters[prevId]?.requestFocus() } catch(_: Exception){} // --- FIX ---
                                                } else {
                                                    newChildren.removeAt(safeIndex - 1)
                                                    onUpdate(currentBlock.copy(children = newChildren))
                                                }
                                            } else if (safeIndex == 0) {
                                                // --- FIX: Logic cleanup, deleteEntirelyFlag is impossible to be true here ---
                                                if (remainder.trim().replace("\u200B","").isEmpty()) {
                                                    newChildren.removeAt(0)
                                                    onUpdate(currentBlock.copy(children = newChildren))
                                                    onFocusRequest(currentBlock.id)
                                                } else {
                                                    newChildren[0] = newChildren[0].copy(content = "\u200B" + remainder)
                                                    onUpdate(currentBlock.copy(children = newChildren))
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    val lastChild = currentBlock.children.lastOrNull()
                    val showAddBtn = lastChild == null || lastChild.type != BlockType.TEXT || lastChild.content.replace("\u200B", "").isNotEmpty()
                    if (!isReadOnly && !currentBlock.isLocked && showAddBtn) { TextButton(onClick = { val newChildren = currentBlock.children.toMutableList(); val newId = java.util.UUID.randomUUID().toString(); newChildren.add(SandboxBlock(id = newId, content = "\u200B")); onUpdate(currentBlock.copy(children = newChildren)); onFocusRequest(newId) }) { Text("+ Add Block", color = Color.Gray) } }
                }
            }
        }
    }
}