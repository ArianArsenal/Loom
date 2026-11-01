package com.example.loom.ui.sandbox.blocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.TextFormatSpan
import com.example.loom.ui.sandbox.TableCellData
import com.example.loom.ui.sandbox.TextFormatToolbar
import com.example.loom.ui.sandbox.adjustFormats
import com.example.loom.ui.sandbox.buildAnnotated
import com.example.loom.ui.sandbox.encodeCell
import com.example.loom.ui.sandbox.parseCell

@Composable
fun TableBlockRenderer(
    block: SandboxBlock,
    recentTableColors: MutableList<Color>,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    var showTableOptions by remember { mutableStateOf(false) }
    var cellOptionsTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var focusedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val colCount = block.tableData.firstOrNull()?.size ?: 3
    val rowCount = block.tableData.size

    val colWidths = if (block.colWidths.size == colCount) block.colWidths else List(colCount) { 120f }
    val rowHeights = if (block.rowHeights.size == rowCount) block.rowHeights else List(rowCount) { 48f }

    var activeTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            // --- FIX: Added the surface background modifier to remove the purple tint ---
            Box {
                IconButton(onClick = { showTableOptions = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreHoriz, "Table Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = showTableOptions,
                    onDismissRequest = { showTableOptions = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(text = { Text("Delete Table", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Column {
                block.tableData.forEachIndexed { rIndex, row ->
                    Row(modifier = Modifier.height(IntrinsicSize.Max)) {
                        row.forEachIndexed { cIndex, rawCell ->
                            val cell = parseCell(rawCell)
                            val isFocused = focusedCell == (rIndex to cIndex)

                            Box(modifier = Modifier.width(colWidths[cIndex].dp).height(rowHeights[rIndex].dp)) {
                                if (cell.isDeleted) {
                                    Box(modifier = Modifier.fillMaxSize().padding(4.dp))
                                } else {
                                    val bgColor = cell.colorHex?.let { try { Color(it.toColorInt()) } catch(_: Exception){Color.Transparent} } ?: Color.Transparent
                                    Surface(
                                        modifier = Modifier.fillMaxSize().border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                        color = bgColor
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {

                                            val displayValue = if (isFocused) {
                                                activeTextFieldValue
                                            } else {
                                                TextFieldValue(buildAnnotated(cell.text, cell.formats), TextRange(cell.text.length))
                                            }

                                            BasicTextField(
                                                value = displayValue,
                                                onValueChange = { newTextState ->
                                                    if (isFocused) {
                                                        val oldText = activeTextFieldValue.text
                                                        val newText = newTextState.text
                                                        var newFormats = cell.formats

                                                        if (newText != oldText) {
                                                            newFormats = adjustFormats(oldText, newText, cell.formats, newTextState.selection.start)
                                                        }

                                                        val updatedValue = newTextState.copy(annotatedString = buildAnnotated(newText, newFormats))
                                                        activeTextFieldValue = updatedValue

                                                        val newData = block.tableData.map { it.toMutableList() }.toMutableList()
                                                        newData[rIndex][cIndex] = encodeCell(cell.copy(text = newText, formats = newFormats))
                                                        onUpdate(block.copy(tableData = newData))
                                                    }
                                                },
                                                modifier = Modifier.fillMaxSize()
                                                    .verticalScroll(rememberScrollState())
                                                    .onFocusChanged {
                                                        if (it.isFocused) {
                                                            focusedCell = rIndex to cIndex
                                                            activeTextFieldValue = TextFieldValue(buildAnnotated(cell.text, cell.formats), TextRange(cell.text.length))
                                                        }
                                                    }
                                                    .padding(8.dp),
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                                            )

                                            if (isFocused) {
                                                IconButton(onClick = { cellOptionsTarget = rIndex to cIndex }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp)) {
                                                    Icon(Icons.Default.MoreVert, "Options", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        focusedCell?.let { (r, c) ->
            if (r in block.tableData.indices && c in block.tableData[r].indices) {
                val cell = parseCell(block.tableData[r][c])
                val fakeBlock = SandboxBlock(id = "table_cell", content = cell.text, textFormats = cell.formats)

                TextFormatToolbar(
                    isFocused = true,
                    textState = activeTextFieldValue,
                    currentBlock = fakeBlock,
                    recentTableColors = recentTableColors,
                    onUpdate = { updatedBlock ->
                        val newData = block.tableData.map { it.toMutableList() }.toMutableList()
                        newData[r][c] = encodeCell(cell.copy(formats = updatedBlock.textFormats))
                        onUpdate(block.copy(tableData = newData))

                        activeTextFieldValue = activeTextFieldValue.copy(annotatedString = buildAnnotated(activeTextFieldValue.text, updatedBlock.textFormats))
                    },
                    applyFormat = { type, value ->
                        val selStart = activeTextFieldValue.selection.min
                        val selEnd = activeTextFieldValue.selection.max
                        val overlapping = cell.formats.filter { it.type == type && maxOf(it.start, selStart) < minOf(it.end, selEnd) }
                        val cleared = cell.formats - overlapping.toSet()

                        val newFormats = if (value.isNotEmpty()) {
                            cleared + TextFormatSpan(selStart, selEnd, type, value)
                        } else if (type in listOf("BOLD", "ITALIC", "STRIKE", "UNDERLINE")) {
                            if (overlapping.isNotEmpty()) cleared else cleared + TextFormatSpan(selStart, selEnd, type, "")
                        } else {
                            cleared
                        }

                        val newData = block.tableData.map { it.toMutableList() }.toMutableList()
                        newData[r][c] = encodeCell(cell.copy(formats = newFormats))
                        onUpdate(block.copy(tableData = newData))

                        activeTextFieldValue = activeTextFieldValue.copy(annotatedString = buildAnnotated(activeTextFieldValue.text, newFormats))
                    }
                )
            } else {
                focusedCell = null
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = {
                val newData = block.tableData.toMutableList().apply { add(List(colCount) { encodeCell(TableCellData()) }) }
                val newHeights = rowHeights.toMutableList().apply { add(48f) }
                onUpdate(block.copy(tableData = newData, rowHeights = newHeights))
            }) { Text("+ Add Row") }
            TextButton(onClick = {
                val newData = block.tableData.map { row -> row.toMutableList().apply { add(encodeCell(TableCellData())) } }
                val newWidths = colWidths.toMutableList().apply { add(120f) }
                onUpdate(block.copy(tableData = newData, colWidths = newWidths))
            }) { Text("+ Add Column") }
        }
    }

    if (cellOptionsTarget != null) {
        val (r, c) = cellOptionsTarget!!
        val currentCell = parseCell(block.tableData[r][c])
        var showColorWheel by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { cellOptionsTarget = null },
            title = { Text("Cell Settings") },
            text = {
                if (showColorWheel) {
                    var hue by remember { mutableFloatStateOf(210f) }
                    var saturation by remember { mutableFloatStateOf(0.9f) }
                    var value by remember { mutableFloatStateOf(0.9f) }

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
                            val pickedColor = Color.hsv(hue, saturation, value)
                            if (!recentTableColors.contains(pickedColor)) {
                                recentTableColors.add(0, pickedColor)
                                if (recentTableColors.size > 5) recentTableColors.removeAt(recentTableColors.size - 1)
                            }
                            val hex = String.format("#%08X", pickedColor.toArgb())
                            val newData = block.tableData.map { it.toMutableList() }.toMutableList()
                            newData[r][c] = encodeCell(currentCell.copy(colorHex = hex))
                            onUpdate(block.copy(tableData = newData))
                            cellOptionsTarget = null
                        }) { Text("Apply Color") }
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Dimensions", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Width: ${colWidths[c].toInt()}")
                            Row {
                                IconButton(onClick = { val n = colWidths.toMutableList(); n[c] = (n[c]-10f).coerceAtLeast(60f); onUpdate(block.copy(colWidths=n)) }) { Icon(Icons.Default.Remove, "") }
                                IconButton(onClick = { val n = colWidths.toMutableList(); n[c] += 10f; onUpdate(block.copy(colWidths=n)) }) { Icon(Icons.Default.Add, "") }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Height: ${rowHeights[r].toInt()}")
                            Row {
                                IconButton(onClick = { val n = rowHeights.toMutableList(); n[r] = (n[r]-10f).coerceAtLeast(36f); onUpdate(block.copy(rowHeights=n)) }) { Icon(Icons.Default.Remove, "") }
                                IconButton(onClick = { val n = rowHeights.toMutableList(); n[r] += 10f; onUpdate(block.copy(rowHeights=n)) }) { Icon(Icons.Default.Add, "") }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        TextButton(onClick = {
                            val newData = block.tableData.map { it.toMutableList() }.toMutableList()
                            newData[r][c] = encodeCell(currentCell.copy(isDeleted = true))
                            onUpdate(block.copy(tableData = newData))
                            cellOptionsTarget = null
                        }) { Text("Delete Cell (Leave Blank)") }

                        TextButton(onClick = {
                            if (block.tableData.size <= 1) {
                                onDeleteBlock("", true)
                            } else {
                                val newData = block.tableData.toMutableList().apply { removeAt(r) }
                                val newHeights = rowHeights.toMutableList().apply { removeAt(r) }
                                focusedCell = null // Fix State Leak
                                onUpdate(block.copy(tableData = newData, rowHeights = newHeights))
                            }
                            cellOptionsTarget = null
                        }) { Text("Delete Entire Row", color = MaterialTheme.colorScheme.error) }

                        TextButton(onClick = {
                            if ((block.tableData.firstOrNull()?.size ?: 0) <= 1) {
                                onDeleteBlock("", true)
                            } else {
                                val newData = block.tableData.map { it.toMutableList().apply { removeAt(c) } }
                                val newWidths = colWidths.toMutableList().apply { removeAt(c) }
                                focusedCell = null // Fix State Leak
                                onUpdate(block.copy(tableData = newData, colWidths = newWidths))
                            }
                            cellOptionsTarget = null
                        }) { Text("Delete Entire Column", color = MaterialTheme.colorScheme.error) }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cell Color", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val palette = listOf(Color.Transparent) + recentTableColors
                            palette.take(6).forEach { color ->
                                Box(
                                    modifier = Modifier.size(32.dp).background(color, CircleShape).border(1.dp, Color.LightGray, CircleShape)
                                        .clickable {
                                            val hex = if (color == Color.Transparent) null else String.format("#%08X", color.toArgb())
                                            val newData = block.tableData.map { it.toMutableList() }.toMutableList()
                                            newData[r][c] = encodeCell(currentCell.copy(colorHex = hex))
                                            onUpdate(block.copy(tableData = newData))
                                            cellOptionsTarget = null
                                        }
                                )
                            }
                            Box(
                                modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).clickable { showColorWheel = true },
                                contentAlignment = Alignment.Center
                            ) { Text("+") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { cellOptionsTarget = null }) { Text("Close") } }
        )
    }
}