package com.example.loom.ui.sandbox.blocks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SketchPoint
import com.example.loom.data.models.SketchStroke

@Composable
fun SketchBlockRenderer(
    block: SandboxBlock,
    textState: TextFieldValue,
    handleTextChange: (TextFieldValue) -> Unit,
    commonModifier: Modifier,
    placeholderUI: @Composable (@Composable () -> Unit) -> Unit,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    var showBlockMenu by remember { mutableStateOf(false) }
    var localStrokes by remember { mutableStateOf(block.sketchStrokes) }
    var recentColors by remember { mutableStateOf(listOf("#000000")) }
    var currentColor by remember { mutableStateOf("#000000") }
    var currentWidth by remember { mutableFloatStateOf(8f) }
    var showColorWheel by remember { mutableStateOf(false) }

    LaunchedEffect(block.sketchStrokes) {
        if (block.sketchStrokes.isEmpty() && localStrokes.isNotEmpty()) {
            localStrokes = emptyList()
        }
    }

    // --- NEW: Faint outer border enclosing the entire Sketch Block ---
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {

            // --- TOP TOOLBAR ---
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = textState, onValueChange = handleTextChange,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    modifier = commonModifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = placeholderUI
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    recentColors.take(4).forEach { hex ->
                        Box(modifier = Modifier.size(24.dp).padding(2.dp).background(Color(android.graphics.Color.parseColor(hex)), CircleShape).border(if (currentColor == hex) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape).clickable { currentColor = hex })
                    }
                    IconButton(onClick = { showColorWheel = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Palette, "Custom Color", tint = Color.Gray, modifier = Modifier.size(20.dp)) }

                    // --- NEW: Real Eraser Icon ---
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { currentColor = "#FAFAFA" }
                            .border(if (currentColor == "#FAFAFA") 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color.Gray)
                        ) {}
                    }

                    TextButton(onClick = {
                        localStrokes = emptyList()
                        onUpdate(block.copy(sketchStrokes = emptyList()))
                    }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                    IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                    DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                        DropdownMenuItem(text = { Text("Delete Canvas", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                    }
                }
            }

            // --- MODERN SLIDER ---
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Brush Size", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                val displayColor = try { Color(android.graphics.Color.parseColor(currentColor)) } catch (e: Exception) { Color.Black }
                val safeDisplayColor = if (currentColor == "#FAFAFA") Color.LightGray else displayColor

                Slider(
                    value = currentWidth,
                    onValueChange = { currentWidth = it },
                    valueRange = 2f..40f,
                    modifier = Modifier.padding(horizontal = 12.dp).weight(1f).height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = safeDisplayColor,
                        activeTrackColor = safeDisplayColor.copy(alpha = 0.7f),
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            var currentStroke by remember { mutableStateOf<SketchStroke?>(null) }

            // --- CANVAS AREA ---
            Surface(modifier = Modifier.fillMaxWidth().height(300.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), color = Color(0xFFFAFAFA)) {
                Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStroke = SketchStroke(color = currentColor, width = currentWidth, points = listOf(SketchPoint(offset.x, offset.y)))
                        },
                        onDrag = { change, _ ->
                            currentStroke = currentStroke?.let { it.copy(points = it.points + SketchPoint(change.position.x, change.position.y)) }
                        },
                        onDragEnd = {
                            currentStroke?.let { stroke ->
                                val newStrokes = localStrokes + stroke
                                localStrokes = newStrokes
                                onUpdate(block.copy(sketchStrokes = newStrokes))
                                currentStroke = null
                            }
                        }
                    )
                }) {
                    val drawStroke = { stroke: SketchStroke ->
                        if (stroke.points.size > 1) {
                            val path = Path().apply {
                                moveTo(stroke.points.first().x, stroke.points.first().y)
                                stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(path, color = try { Color(android.graphics.Color.parseColor(stroke.color)) } catch(e:Exception){Color.Black}, style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                    localStrokes.forEach { drawStroke(it) }
                    currentStroke?.let { drawStroke(it) }
                }
            }
        }
    }

    // --- HSV COLOR WHEEL DIALOG ---
    if (showColorWheel) {
        var hue by remember { mutableFloatStateOf(210f) }
        var saturation by remember { mutableFloatStateOf(0.9f) }
        var value by remember { mutableFloatStateOf(0.9f) }

        AlertDialog(
            onDismissRequest = { showColorWheel = false },
            title = { Text("Custom Color") },
            text = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(Color.hsv(hue, 1f, 1f))
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
                    Spacer(Modifier.height(16.dp))
                    Canvas(modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(50))
                        .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); hue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f } }
                    ) {
                        drawRect(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                        drawCircle(Color.White, 12.dp.toPx(), Offset((hue / 360f) * size.width, size.height / 2), style = Stroke(3.dp.toPx()))
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        val hex = String.format("#%06X", (0xFFFFFF and Color.hsv(hue, saturation, value).toArgb()))
                        recentColors = (listOf(hex) + recentColors).distinct().take(4)
                        currentColor = hex
                        showColorWheel = false
                    }) { Text("Use Color") }
                }
            },
            confirmButton = { TextButton(onClick = { showColorWheel = false }) { Text("Cancel") } }
        )
    }
}