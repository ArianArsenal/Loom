package com.example.loom.ui.sandbox.blocks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace

@Composable
fun SpaceBlockRenderer(currentBlock: SandboxBlock, isReadOnly: Boolean, allSpaces: List<SandboxSpace>, currentSpaceId: String, onUpdate: (SandboxBlock) -> Unit, onDeleteBlock: (String, Boolean) -> Unit, onScrollToTop: () -> Unit, onNavigateToSpace: (String) -> Unit, onCreateLinkedSpace: () -> String) {
    var showBlockMenu by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (currentBlock.url.isEmpty()) {
            var showSpacePicker by remember { mutableStateOf(false) }
            if (!isReadOnly) { Button(onClick = { showSpacePicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)) { Icon(Icons.Default.Dashboard, null); Spacer(Modifier.width(8.dp)); Text("Select or Create a Space to Link") } }
            if (showSpacePicker) {
                AlertDialog(
                    onDismissRequest = { showSpacePicker = false },
                    title = { Text("Link a Space") },
                    text = {
                        LazyColumn {
                            item { TextButton(onClick = { val newId = onCreateLinkedSpace(); onUpdate(currentBlock.copy(url = newId)); onNavigateToSpace(newId); showSpacePicker = false }) { Text("+ Create New Space", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }; HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                            items(allSpaces) { s -> Row(modifier = Modifier.fillMaxWidth().clickable { onUpdate(currentBlock.copy(url = s.id)); showSpacePicker = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(s.icon, fontSize = 24.sp); Spacer(Modifier.width(16.dp)); Text(s.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium); if (s.id == currentSpaceId) { Spacer(Modifier.width(8.dp)); Text("(Current Space)", color = Color.Gray, fontSize = 12.sp) } } }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showSpacePicker = false }) { Text("Cancel") } }
                )
            }
        } else {
            val linkedSpace = allSpaces.find { it.id == currentBlock.url }
            if (linkedSpace != null) {
                Surface(modifier = Modifier.fillMaxWidth().clickable { if (linkedSpace.id == currentSpaceId) onScrollToTop() else onNavigateToSpace(linkedSpace.id) }, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(linkedSpace.icon, fontSize = 24.sp); Spacer(Modifier.width(12.dp)); Text(linkedSpace.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); if (linkedSpace.id == currentSpaceId) { Text("(Current)", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp)) }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        if (!isReadOnly) { IconButton(onClick = { showBlockMenu = true }) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }; DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) { DropdownMenuItem(text = { Text("Delete Block", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) }); DropdownMenuItem(text = { Text("Change Space") }, onClick = { onUpdate(currentBlock.copy(url = "")); showBlockMenu = false }) } }
                    }
                }
            } else {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(12.dp)); Text("Space not found or deleted.", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f)); if (!isReadOnly) { IconButton(onClick = { onDeleteBlock("", true) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) } } }
                }
            }
        }
    }
}