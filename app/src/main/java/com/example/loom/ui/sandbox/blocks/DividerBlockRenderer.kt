package com.example.loom.ui.sandbox.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DividerBlockRenderer(isReadOnly: Boolean, onDeleteBlock: (String, Boolean) -> Unit) {
    var showBlockMenu by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
        HorizontalDivider(modifier = Modifier.padding(end = 40.dp), color = Color.LightGray)
        if (!isReadOnly) {
            // --- FIX: Wrapped in anchored bounding Box ---
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, "Options", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = showBlockMenu,
                    onDismissRequest = { showBlockMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(text = { Text("Delete Divider", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                }
            }
        }
    }
}