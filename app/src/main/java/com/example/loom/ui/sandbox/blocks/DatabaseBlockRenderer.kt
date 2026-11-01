package com.example.loom.ui.sandbox.blocks

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.loom.data.models.DatabaseProperty
import com.example.loom.data.models.PropertyType
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import com.example.loom.ui.sandbox.copyMediaToLocal
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Serializable
data class DatabaseSchema(
    val properties: List<DatabaseProperty> = listOf(
        DatabaseProperty(name = "Name", type = PropertyType.TEXT),
        DatabaseProperty(name = "Tags", type = PropertyType.MULTI_SELECT)
    ),
    val calculations: Map<Int, String> = emptyMap()
)

fun getSchema(block: SandboxBlock): DatabaseSchema {
    return try {
        if (block.secondaryContent.isNotBlank()) Json.decodeFromString(block.secondaryContent)
        else DatabaseSchema()
    } catch (_: Exception) { DatabaseSchema() }
}

@Composable
fun DatabaseBlockRenderer(
    block: SandboxBlock,
    allSpaces: List<SandboxSpace>,
    currentSpaceId: String,
    onNavigateToSpace: (String) -> Unit,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    val schema = getSchema(block)
    var showTableOptions by remember { mutableStateOf(false) }

    val rows = block.tableData.ifEmpty { listOf(List(schema.properties.size) { "" }) }
    val colWidths = if (block.colWidths.size == schema.properties.size) block.colWidths else List(schema.properties.size) { 150f }

    val totalWidthDp = (colWidths.sum() + 150f).dp
    val cellBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box {
                IconButton(onClick = { showTableOptions = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreHoriz, "Database Options", tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = showTableOptions,
                    onDismissRequest = { showTableOptions = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(text = { Text("Delete Database", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {

            Column(
                modifier = Modifier
                    .border(1.dp, cellBorderColor, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                // --- HEADER ROW ---
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    schema.properties.forEachIndexed { pIndex, prop ->
                        var showPropMenu by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .width(colWidths[pIndex].dp)
                                .fillMaxHeight()
                                .clickable { showPropMenu = true }
                                .drawBehind {
                                    drawLine(cellBorderColor, Offset(size.width, 0f), Offset(size.width, size.height), 1.dp.toPx())
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(getPropertyIcon(prop.type), contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(prop.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            if (showPropMenu) {
                                PropertyEditDialog(
                                    property = prop,
                                    canDelete = schema.properties.size > 1,
                                    onDismiss = { showPropMenu = false },
                                    onUpdateProp = { updatedProp ->
                                        val newProps = schema.properties.toMutableList()
                                        newProps[pIndex] = updatedProp
                                        onUpdate(block.copy(secondaryContent = Json.encodeToString(schema.copy(properties = newProps))))
                                    },
                                    onDeleteProp = {
                                        val newProps = schema.properties.toMutableList().apply { removeAt(pIndex) }
                                        val newWidths = colWidths.toMutableList().apply { removeAt(pIndex) }
                                        val newRows = rows.map { it.toMutableList().apply { removeAt(pIndex) } }
                                        onUpdate(block.copy(
                                            secondaryContent = Json.encodeToString(schema.copy(properties = newProps)),
                                            colWidths = newWidths,
                                            tableData = newRows
                                        ))
                                    }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .fillMaxHeight()
                            .clickable {
                                val newProps = schema.properties.toMutableList().apply { add(DatabaseProperty()) }
                                val newWidths = colWidths.toMutableList().apply { add(150f) }
                                val newRows = rows.map { it.toMutableList().apply { add("") } }
                                onUpdate(block.copy(
                                    secondaryContent = Json.encodeToString(schema.copy(properties = newProps)),
                                    colWidths = newWidths,
                                    tableData = newRows
                                ))
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Property", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .width(totalWidthDp)
                        .height(1.5.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                )

                // --- DATA ROWS ---
                rows.forEachIndexed { rIndex, rowData ->
                    Row(modifier = Modifier.height(IntrinsicSize.Max)) {
                        rowData.forEachIndexed { cIndex, cellValue ->
                            val prop = schema.properties.getOrNull(cIndex) ?: return@forEachIndexed

                            Box(
                                modifier = Modifier
                                    .width(colWidths[cIndex].dp)
                                    .fillMaxHeight()
                                    .drawBehind {
                                        val stroke = 1.dp.toPx()
                                        drawLine(cellBorderColor, Offset(size.width, 0f), Offset(size.width, size.height), stroke)
                                        drawLine(cellBorderColor, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                DatabaseCell(
                                    value = cellValue,
                                    property = prop,
                                    rowValues = rowData,
                                    allSpaces = allSpaces,
                                    onNavigateToSpace = onNavigateToSpace,
                                    onValueChange = { newVal ->
                                        val newRows = rows.map { it.toMutableList() }.toMutableList()
                                        newRows[rIndex][cIndex] = newVal
                                        onUpdate(block.copy(tableData = newRows))
                                    }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .fillMaxHeight()
                                .drawBehind {
                                    drawLine(cellBorderColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                                }
                        )
                    }
                }

                // --- CALCULATION FOOTER ---
                if (schema.calculations.isNotEmpty() || schema.properties.any { it.type in listOf(PropertyType.NUMBER, PropertyType.FORMULA) }) {
                    Row(modifier = Modifier.height(IntrinsicSize.Min).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.2f))) {
                        schema.properties.forEachIndexed { cIndex, prop ->
                            val calcType = schema.calculations[cIndex]

                            Box(
                                modifier = Modifier
                                    .width(colWidths[cIndex].dp)
                                    .fillMaxHeight()
                                    .drawBehind {
                                        drawLine(cellBorderColor, Offset(size.width, 0f), Offset(size.width, size.height), 1.dp.toPx())
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (calcType != null) {
                                    val numbers = rows.mapNotNull { it.getOrNull(cIndex)?.toFloatOrNull() }
                                    val result = when (calcType) {
                                        "SUM" -> numbers.sum()
                                        "AVG" -> if (numbers.isNotEmpty()) numbers.average().toFloat() else 0f
                                        "MIN" -> numbers.minOrNull() ?: 0f
                                        "MAX" -> numbers.maxOrNull() ?: 0f
                                        else -> 0f
                                    }
                                    val formattedRes = if (result % 1f == 0f) result.toInt().toString() else result.toString()

                                    var showCalcMenu by remember { mutableStateOf(false) }
                                    Box {
                                        Text("$calcType: $formattedRes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { showCalcMenu = true })
                                        DropdownMenu(
                                            expanded = showCalcMenu,
                                            onDismissRequest = { showCalcMenu = false },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            DropdownMenuItem(text = { Text("Remove Calculation", color = MaterialTheme.colorScheme.error) }, onClick = {
                                                val newCalcs = schema.calculations.toMutableMap().apply { remove(cIndex) }
                                                onUpdate(block.copy(secondaryContent = Json.encodeToString(schema.copy(calculations = newCalcs))))
                                                showCalcMenu = false
                                            })
                                        }
                                    }
                                } else {
                                    if (prop.type in listOf(PropertyType.NUMBER, PropertyType.FORMULA)) {
                                        var showCalcMenu by remember { mutableStateOf(false) }
                                        Box {
                                            Text("Calculate", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.clickable { showCalcMenu = true })
                                            DropdownMenu(
                                                expanded = showCalcMenu,
                                                onDismissRequest = { showCalcMenu = false },
                                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                            ) {
                                                listOf("SUM", "AVG", "MIN", "MAX").forEach { cType ->
                                                    DropdownMenuItem(text = { Text(cType) }, onClick = {
                                                        val newCalcs = schema.calculations.toMutableMap().apply { put(cIndex, cType) }
                                                        onUpdate(block.copy(secondaryContent = Json.encodeToString(schema.copy(calculations = newCalcs))))
                                                        showCalcMenu = false
                                                    })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Box(modifier = Modifier.width(150.dp).fillMaxHeight())
                    }
                }
            }
        }

        TextButton(
            onClick = {
                val newRows = rows.toMutableList().apply { add(List(schema.properties.size) { "" }) }
                onUpdate(block.copy(tableData = newRows))
            },
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("+ New Row", color = Color.Gray) }
    }
}

// ... PropertyEditDialog and getPropertyIcon remain identical ...
@Composable
fun PropertyEditDialog(property: DatabaseProperty, canDelete: Boolean, onDismiss: () -> Unit, onUpdateProp: (DatabaseProperty) -> Unit, onDeleteProp: () -> Unit) {
    var editName by remember { mutableStateOf(property.name) }
    var editType by remember { mutableStateOf(property.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Property") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(16.dp))
                Text("Type", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(Modifier.height(8.dp))

                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))) {
                    PropertyType.entries.forEach { t ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { editType = t }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(getPropertyIcon(t), null, tint = if (editType == t) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(t.name, modifier = Modifier.weight(1f), color = if (editType == t) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            if (editType == t) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (editType == PropertyType.SELECT || editType == PropertyType.MULTI_SELECT) {
                    Spacer(Modifier.height(16.dp))
                    Text("Options", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))

                    property.options.forEach { (optName, optColor) ->
                        val col = try { Color(optColor.toColorInt()) } catch(_: Exception){Color.Gray}
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Surface(color = col.copy(alpha=0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text(optName, color = col, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {
                                val newOpts = property.options.toMutableMap().apply { remove(optName) }
                                onUpdateProp(property.copy(options = newOpts))
                            }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
                        }
                    }

                    var newOptName by remember { mutableStateOf("") }
                    var hue by remember { mutableFloatStateOf(210f) }

                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        OutlinedTextField(
                            value = newOptName, onValueChange = { newOptName = it },
                            placeholder = { Text("New Tag Name...") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Canvas(modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(50))
                            .pointerInput(Unit) { detectTapGestures { offset -> hue = (offset.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f } }
                            .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); hue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f } }
                        ) {
                            drawRect(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                            drawCircle(Color.White, 8.dp.toPx(), Offset((hue / 360f) * size.width, size.height / 2), style = Stroke(2.dp.toPx()))
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            if (newOptName.isNotBlank() && !property.options.containsKey(newOptName)) {
                                val hex = String.format("#%06X", (0xFFFFFF and Color.hsv(hue, 0.9f, 0.9f).toArgb()))
                                val newOpts = property.options.toMutableMap().apply { put(newOptName, hex) }
                                onUpdateProp(property.copy(options = newOpts))
                                newOptName = ""
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Add Tag") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUpdateProp(property.copy(name = editName, type = editType))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            if (canDelete) {
                TextButton(onClick = {
                    onDeleteProp()
                    onDismiss()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

fun getPropertyIcon(type: PropertyType): androidx.compose.ui.graphics.vector.ImageVector {
    return when(type) {
        PropertyType.TEXT -> Icons.Default.FormatSize
        PropertyType.NUMBER -> Icons.Default.Numbers
        PropertyType.CHECKBOX -> Icons.Default.CheckBox
        PropertyType.DATE -> Icons.Default.CalendarToday
        PropertyType.SELECT -> Icons.Default.ArrowDropDownCircle
        PropertyType.MULTI_SELECT -> Icons.Default.Checklist
        PropertyType.STATUS -> Icons.Default.DonutLarge
        PropertyType.PROGRESS -> Icons.Default.LinearScale
        PropertyType.FORMULA -> Icons.Default.Functions
        PropertyType.URL -> Icons.Default.Link
        PropertyType.MEDIA -> Icons.Default.Image
        PropertyType.SPACE -> Icons.Default.Dashboard
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseCell(
    value: String,
    property: DatabaseProperty,
    rowValues: List<String>,
    allSpaces: List<SandboxSpace>,
    onNavigateToSpace: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    val context = LocalContext.current

    when (property.type) {
        PropertyType.TEXT -> {
            BasicTextField(
                value = value, onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
        }
        PropertyType.NUMBER -> {
            BasicTextField(
                value = value, onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
        }
        PropertyType.CHECKBOX -> {
            val isChecked = value == "true"
            Icon(
                imageVector = if (isChecked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isChecked) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(20.dp).clickable { onValueChange(if (isChecked) "false" else "true") }
            )
        }
        PropertyType.DATE -> {
            var showDatePicker by remember { mutableStateOf(false) }
            if (showDatePicker) {
                val dateState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            dateState.selectedDateMillis?.let {
                                val cal = Calendar.getInstance().apply { timeInMillis = it }
                                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                onValueChange(sdf.format(cal.time))
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
                ) { DatePicker(state = dateState) }
            }

            Text(
                text = value.ifEmpty { "Empty" },
                color = if (value.isEmpty()) Color.LightGray else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
            )
        }
        PropertyType.SELECT, PropertyType.MULTI_SELECT -> {
            var expanded by remember { mutableStateOf(false) }
            val selectedTags = value.split(",").filter { it.isNotBlank() }

            Box {
                Row(modifier = Modifier.fillMaxWidth().clickable { expanded = true }, verticalAlignment = Alignment.CenterVertically) {
                    if (selectedTags.isEmpty()) {
                        Text("Empty", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).weight(1f)) {
                            selectedTags.forEach { tag ->
                                val tagColorHex = property.options[tag] ?: "#808080"
                                val tagColor = try { Color(tagColorHex.toColorInt()) } catch (_: Exception) { Color.Gray }

                                Surface(color = tagColor.copy(alpha=0.2f), shape = RoundedCornerShape(4.dp)) {
                                    Text(tag, color = tagColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    if (property.options.isEmpty()) {
                        DropdownMenuItem(text = { Text("No tags in Schema. Click header to add.") }, onClick = {})
                    } else {
                        property.options.forEach { (optName, optColorHex) ->
                            val isSelected = selectedTags.contains(optName)
                            val optColor = try { Color(optColorHex.toColorInt()) } catch (_: Exception) { Color.Gray }

                            DropdownMenuItem(
                                text = {
                                    Surface(color = optColor.copy(alpha=0.2f), shape = RoundedCornerShape(4.dp)) {
                                        Text(optName, color = optColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                },
                                trailingIcon = { if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    if (property.type == PropertyType.SELECT) {
                                        onValueChange(if (isSelected) "" else optName)
                                    } else {
                                        val newTags = if (isSelected) selectedTags - optName else selectedTags + optName
                                        onValueChange(newTags.joinToString(","))
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        PropertyType.STATUS -> {
            var expanded by remember { mutableStateOf(false) }
            val statuses = listOf("Not Started" to Color.Gray, "In Progress" to Color(0xFF2196F3), "Done" to Color(0xFF4CAF50))
            val currentStatus = statuses.find { it.first == value } ?: statuses[0]

            Box {
                Surface(
                    color = currentStatus.second.copy(alpha=0.2f), shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.clickable { expanded = true }
                ) {
                    Text(currentStatus.first, color = currentStatus.second, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    statuses.forEach { (sName, sColor) ->
                        DropdownMenuItem(text = { Text(sName, color = sColor, fontWeight = FontWeight.Bold) }, onClick = { onValueChange(sName); expanded = false })
                    }
                }
            }
        }
        PropertyType.PROGRESS -> {
            val progVal = value.toFloatOrNull() ?: 0f
            Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxWidth(progVal.coerceIn(0f, 1f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
            }
        }
        PropertyType.FORMULA -> {
            val sum = rowValues.mapNotNull { it.toFloatOrNull() }.sum()
            val displaySum = if (sum % 1f == 0f) sum.toInt().toString() else sum.toString()
            Text("Σ $displaySum", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        PropertyType.URL -> {
            BasicTextField(
                value = value, onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF2196F3), textDecoration = TextDecoration.Underline),
                modifier = Modifier.fillMaxWidth(), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner -> Box { if (value.isEmpty()) Text("http://", color = Color.LightGray) else inner() } }
            )
            if (value.isNotEmpty() && !value.contains("http://")) {
                Spacer(Modifier.fillMaxWidth().height(24.dp).clickable {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, "https://$value".toUri())) } catch (_: Exception) {}
                })
            }
        }
        PropertyType.MEDIA -> {
            val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let { val localUrl = copyMediaToLocal(context, it); if (localUrl != null) onValueChange(localUrl) }
            }
            if (value.isEmpty()) {
                TextButton(onClick = { mediaLauncher.launch("image/*") }, contentPadding = PaddingValues(0.dp)) { Text("+ Image") }
            } else {
                AsyncImage(model = value, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).clickable { mediaLauncher.launch("image/*") })
            }
        }
        PropertyType.SPACE -> {
            var expanded by remember { mutableStateOf(false) }
            val linkedSpace = allSpaces.find { it.id == value }

            Box {
                Row(modifier = Modifier.fillMaxWidth().clickable { expanded = true }, verticalAlignment = Alignment.CenterVertically) {
                    if (linkedSpace != null) {
                        SpaceDynamicIcon(linkedSpace.icon, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(linkedSpace.title.ifEmpty { "Untitled" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f).clickable { onNavigateToSpace(linkedSpace.id) })
                    } else {
                        Text("Link a Space", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    allSpaces.forEach { s ->
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) { SpaceDynamicIcon(s.icon, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(s.title) } },
                            onClick = { onValueChange(s.id); expanded = false }
                        )
                    }
                }
            }
        }
    }
}