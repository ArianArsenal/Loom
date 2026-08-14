package com.example.loom.ui.sandbox.blocks

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.models.PropertyType
import com.example.loom.data.models.DatabaseProperty
import com.example.loom.data.models.SandboxBlock
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
    )
)

// Helper to quickly encode/decode the schema
fun getSchema(block: SandboxBlock): DatabaseSchema {
    return try {
        if (block.secondaryContent.isNotBlank()) Json.decodeFromString(block.secondaryContent)
        else DatabaseSchema()
    } catch (e: Exception) { DatabaseSchema() }
}

@Composable
fun DatabaseBlockRenderer(
    block: SandboxBlock,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    val schema = getSchema(block)
    var showTableOptions by remember { mutableStateOf(false) }

    // If table data is empty, initialize it to match the schema length
    val rows = if (block.tableData.isNotEmpty()) block.tableData else listOf(List(schema.properties.size) { "" })

    // Ensure table widths match schema length
    val colWidths = if (block.colWidths.size == schema.properties.size) block.colWidths else List(schema.properties.size) { 150f }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box {
                IconButton(onClick = { showTableOptions = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreHoriz, "Database Options", tint = Color.Gray)
                }
                DropdownMenu(expanded = showTableOptions, onDismissRequest = { showTableOptions = false }) {
                    DropdownMenuItem(text = { Text("Delete Database", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Column {
                // --- HEADER ROW (The Schema Definition) ---
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    schema.properties.forEachIndexed { pIndex, prop ->
                        var showPropMenu by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .width(colWidths[pIndex].dp)
                                .fillMaxHeight()
                                .clickable { showPropMenu = true }
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val typeIcon = when(prop.type) {
                                    PropertyType.TEXT -> "Aa"
                                    PropertyType.NUMBER -> "123"
                                    PropertyType.CHECKBOX -> "☑"
                                    PropertyType.DATE -> "📅"
                                    PropertyType.SELECT -> "▼"
                                    PropertyType.MULTI_SELECT -> "☰"
                                }
                                Text(typeIcon, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                                Text(prop.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            // Property Edit Menu
                            DropdownMenu(expanded = showPropMenu, onDismissRequest = { showPropMenu = false }) {
                                var editName by remember { mutableStateOf(prop.name) }

                                Box(modifier = Modifier.padding(16.dp)) {
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = {
                                            editName = it
                                            val newProps = schema.properties.toMutableList()
                                            newProps[pIndex] = prop.copy(name = it)
                                            onUpdate(block.copy(secondaryContent = Json.encodeToString(schema.copy(properties = newProps))))
                                        },
                                        label = { Text("Property Name") },
                                        singleLine = true
                                    )
                                }
                                HorizontalDivider()
                                PropertyType.values().forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.name) },
                                        trailingIcon = { if (prop.type == t) Icon(Icons.Default.Check, null) },
                                        onClick = {
                                            val newProps = schema.properties.toMutableList()
                                            newProps[pIndex] = prop.copy(type = t)
                                            onUpdate(block.copy(secondaryContent = Json.encodeToString(schema.copy(properties = newProps))))
                                            showPropMenu = false
                                        }
                                    )
                                }
                                if (schema.properties.size > 1) {
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Delete Property", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            val newProps = schema.properties.toMutableList().apply { removeAt(pIndex) }
                                            val newWidths = colWidths.toMutableList().apply { removeAt(pIndex) }
                                            val newRows = rows.map { it.toMutableList().apply { removeAt(pIndex) } }
                                            onUpdate(block.copy(
                                                secondaryContent = Json.encodeToString(schema.copy(properties = newProps)),
                                                colWidths = newWidths,
                                                tableData = newRows
                                            ))
                                            showPropMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // --- "+ Add Property" Column ---
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
                            .padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Property", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // --- DATA ROWS ---
                rows.forEachIndexed { rIndex, rowData ->
                    Row(modifier = Modifier.height(IntrinsicSize.Max).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f))) {
                        rowData.forEachIndexed { cIndex, cellValue ->
                            val prop = schema.properties.getOrNull(cIndex) ?: return@forEachIndexed

                            Box(modifier = Modifier
                                .width(colWidths[cIndex].dp)
                                .fillMaxHeight()
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f))
                                .padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                DatabaseCell(
                                    value = cellValue,
                                    property = prop,
                                    onValueChange = { newVal ->
                                        val newRows = rows.map { it.toMutableList() }.toMutableList()
                                        newRows[rIndex][cIndex] = newVal
                                        onUpdate(block.copy(tableData = newRows))
                                    },
                                    onUpdateSchema = { updatedProp ->
                                        val newProps = schema.properties.toMutableList()
                                        newProps[cIndex] = updatedProp
                                        onUpdate(block.copy(secondaryContent = Json.encodeToString(schema.copy(properties = newProps))))
                                    }
                                )
                            }
                        }

                        // Empty space under the "+ Add Property" column
                        Box(modifier = Modifier.width(150.dp).fillMaxHeight().border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f)))
                    }
                }

                // Add Row Button
                TextButton(
                    onClick = {
                        val newRows = rows.toMutableList().apply { add(List(schema.properties.size) { "" }) }
                        onUpdate(block.copy(tableData = newRows))
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) { Text("+ Add Row", color = Color.Gray) }
            }
        }
    }
}

@Composable
fun DatabaseCell(
    value: String,
    property: DatabaseProperty,
    onValueChange: (String) -> Unit,
    onUpdateSchema: (DatabaseProperty) -> Unit
) {
    val context = LocalContext.current

    when (property.type) {
        PropertyType.TEXT -> {
            BasicTextField(
                value = value, onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
        }
        PropertyType.NUMBER -> {
            BasicTextField(
                value = value, onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
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
            Text(
                text = value.ifEmpty { "Empty" },
                color = if (value.isEmpty()) Color.LightGray else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().clickable {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(context, { _, y, m, d ->
                        cal.set(y, m, d)
                        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        onValueChange(sdf.format(cal.time))
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                            selectedTags.forEach { tag ->
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                                    Text(tag, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    var newOption by remember { mutableStateOf("") }

                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = newOption, onValueChange = { newOption = it },
                            placeholder = { Text("Create new option") }, singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (newOption.isNotBlank() && !property.options.contains(newOption)) {
                                        onUpdateSchema(property.copy(options = property.options + newOption))

                                        val newTags = if (property.type == PropertyType.SELECT) listOf(newOption) else selectedTags + newOption
                                        onValueChange(newTags.joinToString(","))
                                        newOption = ""
                                        expanded = false
                                    }
                                }) { Icon(Icons.Default.Add, "Add") }
                            }
                        )
                    }
                    HorizontalDivider()

                    if (property.options.isEmpty()) {
                        DropdownMenuItem(text = { Text("No options yet", color = Color.Gray) }, onClick = {})
                    } else {
                        property.options.forEach { opt ->
                            val isSelected = selectedTags.contains(opt)
                            DropdownMenuItem(
                                text = { Text(opt) },
                                trailingIcon = { if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    if (property.type == PropertyType.SELECT) {
                                        onValueChange(if (isSelected) "" else opt)
                                    } else {
                                        val newTags = if (isSelected) selectedTags - opt else selectedTags + opt
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
    }
}