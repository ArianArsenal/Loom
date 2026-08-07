package com.example.loom.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.local.entity.SkincareProduct
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Locale

val timelineDateFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

fun Float.formatWeight(): String {
    return if (this % 1f == 0f) this.toInt().toString() else this.toString()
}

@Composable
fun GaplessProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = color.copy(alpha = 0.2f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(color)
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ReorderableProductList(
    products: List<SkincareProduct>,
    onReorder: (List<SkincareProduct>) -> Unit,
    onRemove: ((SkincareProduct) -> Unit)? = null,
    onLongClick: ((SkincareProduct) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        products.forEachIndexed { index, product ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { onLongClick?.invoke(product) }
                    )
            ) {
                Column {
                    IconButton(
                        onClick = {
                            if (index > 0) {
                                val newList = products.toMutableList()
                                Collections.swap(newList, index, index - 1)
                                onReorder(newList)
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up") }
                    IconButton(
                        onClick = {
                            if (index < products.size - 1) {
                                val newList = products.toMutableList()
                                Collections.swap(newList, index, index + 1)
                                onReorder(newList)
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down") }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))

                Text("${product.timeOfUseEmoji} ", fontSize = 14.sp)
                Text(product.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)

                if (onRemove != null) {
                    IconButton(onClick = { onRemove(product) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductProfileDialog(
    initialProduct: SkincareProduct,
    onDismiss: () -> Unit,
    onSave: (SkincareProduct) -> Unit,
    onDelete: (SkincareProduct) -> Unit
) {
    var editName by remember { mutableStateOf(initialProduct.name) }
    var editBrand by remember { mutableStateOf(initialProduct.brand) }
    var editDetails by remember { mutableStateOf(initialProduct.details) }
    var selectedEmoji by remember { mutableStateOf(initialProduct.timeOfUseEmoji) }
    var selectedFrequency by remember { mutableStateOf(initialProduct.frequency) }

    // THE FIX: Added descriptive text mapping for the emojis
    val emojis = listOf(
        "☀️" to "Morning",
        "🌙" to "Night",
        "☀️/🌙" to "Both",
        "💧" to "As Needed"
    )
    val frequencies = listOf("Daily", "2x Week", "3x Week", "As Needed")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Product Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = editName, onValueChange = { editName = it },
                    label = { Text("Product Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editBrand, onValueChange = { editBrand = it },
                    label = { Text("Brand (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Time of Use:", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    emojis.forEach { (emoji, label) ->
                        val isSelected = selectedEmoji == emoji
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedEmoji = emoji }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(label, fontSize = 10.sp, color = if(isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text("Frequency:", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    frequencies.forEach { freq ->
                        val isSelected = selectedFrequency == freq
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedFrequency = freq }
                        ) {
                            Text(
                                text = freq,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editDetails, onValueChange = { editDetails = it },
                    label = { Text("Active Ingredients / Notes") },
                    minLines = 2, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // TOP ROW: Primary Save Action (Full width)
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onSave(initialProduct.copy(
                                name = editName, brand = editBrand, details = editDetails,
                                timeOfUseEmoji = selectedEmoji, frequency = selectedFrequency
                            ))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Profile") }

                // BOTTOM ROW: Secondary Actions (Delete & Cancel)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onDelete(initialProduct) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete Product") }

                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
    )
}