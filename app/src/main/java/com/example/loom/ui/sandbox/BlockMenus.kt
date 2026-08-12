package com.example.loom.ui.sandbox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock


@Composable
fun CalloutColorPicker(currentBlock: SandboxBlock, onUpdate: (SandboxBlock) -> Unit, onDismiss: () -> Unit) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }

    val dynamicColor = Color.hsv(hue, saturation, value)
    val hexString = String.format("#%06X", (0xFFFFFF and dynamicColor.toArgb()))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Callout Color & Glow") },
        text = {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.hsv(hue, 1f, 1f))
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            saturation = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            value = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        drawCircle(Color.White, 6.dp.toPx(), Offset(saturation * size.width, (1f - value) * size.height), style = Stroke(2.dp.toPx()))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(50))
                    .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); hue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f } }
                ) {
                    drawRect(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                    drawCircle(Color.White, 12.dp.toPx(), Offset((hue / 360f) * size.width, size.height / 2), style = Stroke(3.dp.toPx()))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUpdate(currentBlock.copy(secondaryContent = hexString))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun TextFormatToolbar(isFocused: Boolean, textState: TextFieldValue, currentBlock: SandboxBlock, recentTableColors: MutableList<Color>, onUpdate: (SandboxBlock) -> Unit, applyFormat: (String, String) -> Unit) {
    if (isFocused && textState.selection.length > 0) {
        var showTextColorPicker by remember { mutableStateOf(false) }
        var showHighlightColorPicker by remember { mutableStateOf(false) }

        Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, -220)) {
            Surface(shape = RoundedCornerShape(8.dp), shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { applyFormat("BOLD", "") }, modifier = Modifier.size(32.dp)) { Text("B", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) }
                    IconButton(onClick = { applyFormat("ITALIC", "") }, modifier = Modifier.size(32.dp)) { Text("I", fontStyle = FontStyle.Italic, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) }
                    IconButton(onClick = { applyFormat("STRIKE", "") }, modifier = Modifier.size(32.dp)) { Text("S", textDecoration = TextDecoration.LineThrough, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) }
                    IconButton(onClick = { applyFormat("UNDERLINE", "") }, modifier = Modifier.size(32.dp)) { Text("U", textDecoration = TextDecoration.Underline, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) }
                    Spacer(Modifier.width(8.dp)); Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.LightGray)); Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showTextColorPicker = true }, modifier = Modifier.size(32.dp)) { Text("A", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, textDecoration = TextDecoration.Underline) }
                    IconButton(onClick = { showHighlightColorPicker = true }, modifier = Modifier.size(32.dp)) { Text("A", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.surface, modifier = Modifier.background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp)) }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        val selStart = textState.selection.min
                        val selEnd = textState.selection.max
                        val cleared = currentBlock.textFormats.filterNot { maxOf(it.start, selStart) < minOf(it.end, selEnd) }
                        onUpdate(currentBlock.copy(textFormats = cleared))
                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.FormatClear, "Clear Formatting", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                }
            }
        }

        if (showTextColorPicker || showHighlightColorPicker) {
            val isHighlight = showHighlightColorPicker
            var hue by remember { mutableFloatStateOf(210f) }
            var saturation by remember { mutableFloatStateOf(0.9f) }
            var value by remember { mutableFloatStateOf(0.9f) }

            AlertDialog(
                onDismissRequest = { showTextColorPicker = false; showHighlightColorPicker = false },
                title = { Text(if (isHighlight) "Highlight Color" else "Text Color") },
                text = {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(Color.hsv(hue, 1f, 1f)).pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); saturation = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f); value = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f) } }) {
                            Canvas(modifier = Modifier.fillMaxSize()) { drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent))); drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))); drawCircle(Color.White, 6.dp.toPx(), Offset(saturation * size.width, (1f - value) * size.height), style = Stroke(2.dp.toPx())) }
                        }
                        Spacer(Modifier.height(16.dp))
                        Canvas(modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(50)).pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); hue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f } }) {
                            drawRect(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))); drawCircle(Color.White, 12.dp.toPx(), Offset((hue / 360f) * size.width, size.height / 2), style = Stroke(3.dp.toPx()))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val palette = listOf(Color.Transparent) + recentTableColors
                            palette.take(6).forEach { color ->
                                Box(modifier = Modifier.size(32.dp).background(color, CircleShape).border(1.dp, Color.LightGray, CircleShape).clickable {
                                    if (color == Color.Transparent) { applyFormat(if (isHighlight) "HIGHLIGHT" else "COLOR", "") } else {
                                        val hex = String.format("#%06X", (0xFFFFFF and color.toArgb()))
                                        applyFormat(if (isHighlight) "HIGHLIGHT" else "COLOR", hex)
                                    }
                                    showTextColorPicker = false; showHighlightColorPicker = false
                                })
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            val pickedColor = Color.hsv(hue, saturation, value)
                            val hex = String.format("#%06X", (0xFFFFFF and pickedColor.toArgb()))
                            if (!recentTableColors.contains(pickedColor)) {
                                recentTableColors.add(0, pickedColor)
                                if (recentTableColors.size > 5) recentTableColors.removeAt(recentTableColors.size - 1)
                            }
                            applyFormat(if (isHighlight) "HIGHLIGHT" else "COLOR", hex)
                            showTextColorPicker = false; showHighlightColorPicker = false
                        }) { Text("Apply Color") }
                    }
                },
                confirmButton = { TextButton(onClick = { showTextColorPicker = false; showHighlightColorPicker = false }) { Text("Close") } }
            )
        }
    }
}

@Composable
fun SlashCommandMenu(expanded: Boolean, onDismiss: () -> Unit, commandQuery: String, onApplyType: (BlockType) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.width(240.dp).heightIn(max = 350.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)), properties = PopupProperties(focusable = false)) {
        val menuItems = listOf(
            "Basic Blocks" to null,
            BlockType.TEXT to ("Text" to "Aa"), BlockType.HEADING to ("Heading 1" to "H1"), BlockType.HEADING_2 to ("Heading 2" to "H2"), BlockType.HEADING_3 to ("Heading 3" to "H3"), BlockType.HEADING_4 to ("Heading 4" to "H4"), BlockType.TODO to ("To-do List" to "☑"), BlockType.BULLET to ("Bulleted List" to "•"), BlockType.NUMBER to ("Numbered List" to "1."), BlockType.TOGGLE to ("Toggle List" to "▶"), BlockType.QUOTE to ("Quote" to "”"), BlockType.DIVIDER to ("Divider" to "—"),
            "Advanced Blocks" to null,
            BlockType.CARD to ("Widget Card" to "🗂️"), BlockType.TOC to ("Table of Contents" to "📑"), BlockType.CHART to ("Chart" to "📈"), BlockType.CALLOUT to ("Callout" to "💡"), BlockType.TABLE to ("Table" to "⊞"), BlockType.CALENDAR to ("Calendar" to "📅"), BlockType.TIMELINE to ("Weekly Timeline" to "🗓️"), BlockType.KANBAN to ("Kanban Board" to "📊"), BlockType.SKETCH to ("Sketch Canvas" to "🎨"), BlockType.MATH to ("Math Equation" to "🧮"), BlockType.PROGRESS to ("Progress Tracker" to "⏳"), BlockType.CODE to ("Code Snippet" to "⌨️"), BlockType.TIMER to ("Pomodoro Timer" to "⏱️"), BlockType.COUNTDOWN to ("Countdown" to "⏳"), BlockType.GALLERY to ("Photo Gallery" to "📸"),
            "Media & Links" to null,
            BlockType.IMAGE to ("Image" to "🖼️"), BlockType.VIDEO to ("Video" to "▶️"), BlockType.AUDIO to ("Audio" to "🎵"), BlockType.FILE to ("File" to "📁"), BlockType.SPACE to ("Link Space" to "🚀"), BlockType.LINK to ("Web Link" to "🌐"), BlockType.BOOKMARK to ("Web Bookmark" to "🔗")
        )
        val filteredMenuItems = if (commandQuery.isEmpty()) menuItems else menuItems.filter { (type, data) -> if (data == null) false else data.first.contains(commandQuery, ignoreCase = true) || type.toString().contains(commandQuery, ignoreCase = true) }
        if (filteredMenuItems.isEmpty()) { DropdownMenuItem(text = { Text("No matching blocks found", color = Color.Gray, style = MaterialTheme.typography.bodySmall) }, onClick = {}) } else {
            filteredMenuItems.forEach { (type, data) ->
                if (data == null) { Text(type.toString(), style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)) } else {
                    val (label, iconStr) = data
                    DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Text(iconStr, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(32.dp), color = MaterialTheme.colorScheme.primary); Text(label, style = MaterialTheme.typography.bodyMedium) } }, onClick = { onApplyType(type as BlockType) }, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp))
                }
            }
        }
    }
}