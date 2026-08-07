package com.example.loom.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

@Composable
fun ReorderableProductList(
    products: List<String>,
    onReorder: (List<String>) -> Unit,
    onRemove: ((String) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        products.forEachIndexed { index, product ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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
                Text(product, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)

                if (onRemove != null) {
                    IconButton(onClick = { onRemove(product) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}