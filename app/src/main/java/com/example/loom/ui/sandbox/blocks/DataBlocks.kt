package com.example.loom.ui.sandbox.blocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart // --- FIX: Using AutoMirrored ---
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.loom.data.models.SandboxBlock
import com.example.loom.ui.sandbox.ZWSP
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json // --- FIX: Removed redundant inline qualifiers below ---

@Serializable
data class ProgressMeta(
    val current: Float = 0f,
    val max: Float = 100f,
    val unit: String = "%"
)

@Composable
fun ChartBlockRenderer(
    block: SandboxBlock,
    textState: TextFieldValue,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    var showBlockMenu by remember { mutableStateOf(false) }
    var chartType by remember { mutableStateOf(block.secondaryContent.ifEmpty { "BAR" }) }

    val rawText = textState.text.replace(ZWSP, "")
    val chartData = rawText.lines().mapNotNull { line ->
        val parts = line.split(":", ",").map { it.trim() }
        if (parts.size >= 2) {
            val value = parts[1].toFloatOrNull()
            if (value != null) parts[0] to value else null
        } else null
    }

    Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) {

                val chartIcons = listOf(
                    "BAR" to Icons.Default.BarChart,
                    "LINE" to Icons.AutoMirrored.Filled.ShowChart, // --- FIX: Replaced with AutoMirrored ---
                    "PIE" to Icons.Default.PieChart
                )

                chartIcons.forEach { (type, iconVector) ->
                    val isSelected = chartType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable {
                                chartType = type
                                onUpdate(block.copy(secondaryContent = type))
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = type,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
        }

        DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
            DropdownMenuItem(text = { Text("Delete Chart", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        val primaryColor = MaterialTheme.colorScheme.primary
        val chartColors = listOf(primaryColor, Color(0xFF8BC34A), Color(0xFFFF9800), Color(0xFF03A9F4), Color(0xFFE91E63), Color(0xFF9C27B0))
        val textColor = MaterialTheme.colorScheme.onBackground.toArgb()

        Box(modifier = Modifier.fillMaxWidth().height(240.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.2f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            if (chartData.isEmpty()) {
                Text("Add data below to see the chart", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            } else {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val maxValue = chartData.maxOf { it.second }.coerceAtLeast(1f)
                    val labelPaint = android.graphics.Paint().apply {
                        textSize = 32f
                        textAlign = android.graphics.Paint.Align.CENTER
                        color = textColor
                    }

                    when (chartType) {
                        "BAR" -> {
                            val barWidth = (size.width / (chartData.size * 2f)).coerceAtMost(120f)
                            chartData.forEachIndexed { index, dataPoint ->
                                val barHeight = (dataPoint.second / maxValue) * (size.height - 80f)
                                val xOffset = (index * (size.width / chartData.size)) + ((size.width / chartData.size) / 2f)
                                drawRoundRect(
                                    color = chartColors[index % chartColors.size],
                                    topLeft = Offset(xOffset - (barWidth / 2f), size.height - 40f - barHeight),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                )
                                drawContext.canvas.nativeCanvas.drawText(dataPoint.first, xOffset, size.height - 10f, labelPaint)
                                drawContext.canvas.nativeCanvas.drawText(if (dataPoint.second % 1 == 0f) dataPoint.second.toInt().toString() else dataPoint.second.toString(), xOffset, size.height - 50f - barHeight, labelPaint)
                            }
                        }
                        "LINE" -> {
                            val xStep = size.width / (chartData.size - 1).coerceAtLeast(1)
                            val path = Path()
                            chartData.forEachIndexed { index, dataPoint ->
                                val x = if (chartData.size == 1) size.width / 2f else index * xStep
                                val y = size.height - 40f - ((dataPoint.second / maxValue) * (size.height - 80f))
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                drawCircle(color = chartColors[index % chartColors.size], radius = 8.dp.toPx(), center = Offset(x, y))
                                drawContext.canvas.nativeCanvas.drawText(dataPoint.first, x, size.height - 10f, labelPaint)
                                drawContext.canvas.nativeCanvas.drawText(if (dataPoint.second % 1 == 0f) dataPoint.second.toInt().toString() else dataPoint.second.toString(), x, y - 20f, labelPaint)
                            }
                            if (chartData.size > 1) {
                                drawPath(path, color = primaryColor, style = Stroke(width = 3.dp.toPx()))
                            }
                        }
                        "PIE" -> {
                            val total = chartData.sumOf { it.second.toDouble() }.toFloat()
                            var startAngle = -90f
                            chartData.forEachIndexed { index, dataPoint ->
                                val sweepAngle = (dataPoint.second / total) * 360f
                                drawArc(color = chartColors[index % chartColors.size], startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true)
                                startAngle += sweepAngle
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Data Points", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(Modifier.height(8.dp))

            chartData.forEachIndexed { index, data ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(chartColors[index % chartColors.size], CircleShape))
                    Spacer(Modifier.width(8.dp))

                    var nameState by remember { mutableStateOf(TextFieldValue(data.first)) }
                    var isNameFocused by remember { mutableStateOf(false) }

                    LaunchedEffect(data.first) {
                        if (!isNameFocused && nameState.text != data.first) {
                            nameState = nameState.copy(text = data.first)
                        }
                    }

                    BasicTextField(
                        value = nameState,
                        onValueChange = { newNameState ->
                            nameState = newNameState
                            val newLines = chartData.toMutableList()
                            newLines[index] = newNameState.text to data.second
                            val newText = newLines.joinToString("\n") { "${it.first}: ${if (it.second % 1 == 0f) it.second.toInt() else it.second}" }
                            onUpdate(block.copy(content = ZWSP + newText))
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(4.dp))
                            .onFocusChanged { isNameFocused = it.isFocused }
                            .padding(8.dp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(8.dp))

                    val expectedValStr = if (data.second % 1 == 0f) data.second.toInt().toString() else data.second.toString()
                    var valState by remember { mutableStateOf(TextFieldValue(expectedValStr)) }
                    var isValFocused by remember { mutableStateOf(false) }

                    LaunchedEffect(expectedValStr) {
                        if (!isValFocused && valState.text != expectedValStr) {
                            valState = valState.copy(text = expectedValStr)
                        }
                    }

                    BasicTextField(
                        value = valState,
                        onValueChange = { newValState ->
                            valState = newValState
                            val parsed = newValState.text.toFloatOrNull() ?: 0f
                            val newLines = chartData.toMutableList()
                            newLines[index] = data.first to parsed
                            val newText = newLines.joinToString("\n") { "${it.first}: ${if (it.second % 1 == 0f) it.second.toInt() else it.second}" }
                            onUpdate(block.copy(content = ZWSP + newText))
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center),
                        modifier = Modifier
                            .width(60.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(4.dp))
                            .onFocusChanged { isValFocused = it.isFocused }
                            .padding(8.dp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )

                    IconButton(onClick = {
                        val newLines = chartData.toMutableList().apply { removeAt(index) }
                        val newText = newLines.joinToString("\n") { "${it.first}: ${if (it.second % 1 == 0f) it.second.toInt() else it.second}" }
                        onUpdate(block.copy(content = ZWSP + newText))
                    }, modifier = Modifier.size(24.dp).padding(start = 8.dp)) {
                        Icon(Icons.Default.Close, "Remove", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            TextButton(onClick = {
                val currentLines = if (rawText.isBlank()) mutableListOf() else chartData.toMutableList()
                currentLines.add("Item ${currentLines.size + 1}" to 10f)
                val newText = currentLines.joinToString("\n") { "${it.first}: ${if (it.second % 1 == 0f) it.second.toInt() else it.second}" }
                onUpdate(block.copy(content = ZWSP + newText))
            }, modifier = Modifier.padding(top = 4.dp)) {
                Text("+ Add Data Point", color = Color.Gray)
            }
        }
    }
}

@Composable
fun ProgressBlockRenderer(
    block: SandboxBlock,
    textState: TextFieldValue,
    handleTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier, // --- FIX: Renamed modifier ---
    placeholderUI: @Composable (@Composable () -> Unit) -> Unit,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    var showBlockMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val meta = try {
        if (block.secondaryContent.isNotBlank()) {
            Json.decodeFromString<ProgressMeta>(block.secondaryContent)
        } else {
            ProgressMeta(current = block.progress * 100f, max = 100f, unit = "%")
        }
    } catch (_: Exception) { // --- FIX: Unused e ---
        ProgressMeta(current = block.progress * 100f, max = 100f, unit = "%")
    }

    val formatNum = { value: Float -> if (value % 1f == 0f) value.toInt().toString() else value.toString() }

    val displayString = if (meta.unit == "%" && meta.max == 100f) {
        "${formatNum(meta.current)}%"
    } else {
        "${formatNum(meta.current)} / ${formatNum(meta.max)} ${meta.unit}".trim()
    }

    val ratio = if (meta.max > 0f) (meta.current / meta.max).coerceIn(0f, 1f) else 0f

    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = ratio,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "progress_anim"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = textState, onValueChange = handleTextChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold),
                modifier = modifier.weight(1f), // --- FIX: Applied renamed modifier ---
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = placeholderUI
            )

            Spacer(Modifier.width(8.dp))

            Row(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newVal = (meta.current - 1f).coerceAtLeast(0f)
                    val newMeta = meta.copy(current = newVal)
                    onUpdate(block.copy(progress = newVal / meta.max, secondaryContent = Json.encodeToString(newMeta)))
                }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Remove, "Decrease", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text(
                    text = displayString,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.clickable { showEditDialog = true }.padding(horizontal = 4.dp)
                )

                IconButton(onClick = {
                    val newVal = (meta.current + 1f).coerceAtMost(meta.max)
                    val newMeta = meta.copy(current = newVal)
                    onUpdate(block.copy(progress = newVal / meta.max, secondaryContent = Json.encodeToString(newMeta)))
                }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, "Increase", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray)
            }
            DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                DropdownMenuItem(text = { Text("Edit Target & Unit") }, onClick = { showEditDialog = true; showBlockMenu = false })
                DropdownMenuItem(text = { Text("Delete Tracker", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val newRatio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val newVal = newRatio * meta.max
                            val finalVal = if (meta.max % 1f == 0f) kotlin.math.round(newVal) else newVal
                            val newMeta = meta.copy(current = finalVal)
                            onUpdate(block.copy(progress = finalVal / meta.max, secondaryContent = Json.encodeToString(newMeta)))
                        }
                    )
                }
                .pointerInput(Unit) {
                    // --- FIX: Standard extension call relying on Kotlin's type inference ---
                    detectDragGestures { change, _ ->
                        change.consume()
                        val newRatio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val newVal = newRatio * meta.max
                        val finalVal = if (meta.max % 1f == 0f) kotlin.math.round(newVal) else newVal
                        val newMeta = meta.copy(current = finalVal)
                        onUpdate(block.copy(progress = finalVal / meta.max, secondaryContent = Json.encodeToString(newMeta)))
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            )
        }
    }

    if (showEditDialog) {
        var editCurrent by remember { mutableStateOf(formatNum(meta.current)) }
        var editMax by remember { mutableStateOf(formatNum(meta.max)) }
        var editUnit by remember { mutableStateOf(meta.unit) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Setup Goal Tracker") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editCurrent, onValueChange = { editCurrent = it },
                            label = { Text("Current") }, modifier = Modifier.weight(1f), singleLine = true
                        )
                        OutlinedTextField(
                            value = editMax, onValueChange = { editMax = it },
                            label = { Text("Target") }, modifier = Modifier.weight(1f), singleLine = true
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editUnit, onValueChange = { editUnit = it },
                        label = { Text("Unit Label (e.g., Pages, kg, $, %)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val c = editCurrent.toFloatOrNull() ?: meta.current
                    val m = editMax.toFloatOrNull() ?: meta.max
                    val safeMax = if (m <= 0f) 1f else m
                    val safeCurrent = c.coerceIn(0f, safeMax)

                    val newMeta = ProgressMeta(current = safeCurrent, max = safeMax, unit = editUnit.trim())
                    onUpdate(block.copy(progress = safeCurrent / safeMax, secondaryContent = Json.encodeToString(newMeta)))
                    showEditDialog = false
                }) { Text("Save Goal") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
}