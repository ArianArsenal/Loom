package com.example.loom.ui.sandbox.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock
import com.example.loom.ui.sandbox.ZWSP

fun extractHeadings(blocks: List<SandboxBlock>): List<SandboxBlock> {
    val result = mutableListOf<SandboxBlock>()
    for (b in blocks) {
        if (b.type in listOf(BlockType.HEADING, BlockType.HEADING_2, BlockType.HEADING_3, BlockType.HEADING_4)) result.add(b)
        if (b.children.isNotEmpty()) result.addAll(extractHeadings(b.children))
    }
    return result
}

@Composable
fun TocBlockRenderer(
    textState: TextFieldValue,
    handleTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier, // --- FIX: Renamed modifier ---
    placeholderUI: @Composable (@Composable () -> Unit) -> Unit,
    isReadOnly: Boolean,
    allBlocks: List<SandboxBlock>,
    onScrollToBlock: (String) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    var showBlockMenu by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Table of Contents", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            if (!isReadOnly) { IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) } }
        }
        DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) { DropdownMenuItem(text = { Text("Delete TOC", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) }) }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        val headings = extractHeadings(allBlocks)
        if (headings.isEmpty()) { Text("No headings found. Add H1, H2, H3, or H4 blocks.", color = Color.Gray, style = MaterialTheme.typography.bodySmall) } else {
            headings.forEach { hBlock -> val indent = when(hBlock.type) { BlockType.HEADING -> 0.dp; BlockType.HEADING_2 -> 16.dp; BlockType.HEADING_3 -> 32.dp; BlockType.HEADING_4 -> 48.dp; else -> 0.dp }; val text = hBlock.content.replace(ZWSP, "").ifEmpty { "Untitled Heading" }; Text(text = text, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().padding(start = indent, top = 4.dp, bottom = 4.dp).clickable { onScrollToBlock(hBlock.id) }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        BasicTextField(
            value = textState,
            onValueChange = handleTextChange,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Transparent, fontSize = 1.sp),
            modifier = modifier.height(1.dp), // --- FIX: Applied renamed modifier ---
            cursorBrush = SolidColor(Color.Transparent),
            decorationBox = placeholderUI,
            readOnly = isReadOnly
        )
    }
}