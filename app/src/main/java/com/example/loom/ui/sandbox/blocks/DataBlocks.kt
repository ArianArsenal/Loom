package com.example.loom.ui.sandbox.blocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.models.SandboxBlock
import com.example.loom.ui.sandbox.ZWSP

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
                listOf("BAR" to "📊", "LINE" to "📈", "PIE" to "🥧").forEach { (type, icon) ->
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
                        Text(icon, fontSize = 16.sp)
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

                    BasicTextField(
                        value = data.first,
                        onValueChange = { newName ->
                            val newLines = chartData.toMutableList()
                            newLines[index] = newName to data.second
                            val newText = newLines.joinToString("\n") { "${it.first}: ${if (it.second % 1 == 0f) it.second.toInt() else it.second}" }
                            onUpdate(block.copy(content = ZWSP + newText))
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(4.dp)).padding(8.dp)
                    )
                    Spacer(Modifier.width(8.dp))

                    BasicTextField(
                        value = if (data.second % 1 == 0f) data.second.toInt().toString() else data.second.toString(),
                        onValueChange = { newVal ->
                            val parsed = newVal.toFloatOrNull() ?: 0f
                            val newLines = chartData.toMutableList()
                            newLines[index] = data.first to parsed
                            val newText = newLines.joinToString("\n") { "${it.first}: ${if (it.second % 1 == 0f) it.second.toInt() else it.second}" }
                            onUpdate(block.copy(content = ZWSP + newText))
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center),
                        modifier = Modifier.width(60.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(4.dp)).padding(8.dp)
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
    commonModifier: Modifier,
    placeholderUI: @Composable (@Composable () -> Unit) -> Unit,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    var showBlockMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = textState, onValueChange = handleTextChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold),
                modifier = commonModifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = placeholderUI
            )
            Spacer(Modifier.width(8.dp))
            Text("${(block.progress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
            DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                DropdownMenuItem(text = { Text("Delete Tracker", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
            }
        }
        Slider(
            value = block.progress, onValueChange = { onUpdate(block.copy(progress = it)) },
            modifier = Modifier.fillMaxWidth().height(24.dp), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}