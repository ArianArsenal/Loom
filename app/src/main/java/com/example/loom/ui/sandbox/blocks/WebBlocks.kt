package com.example.loom.ui.sandbox.blocks

import com.example.loom.ui.sandbox.LinkMetadata
import com.example.loom.ui.sandbox.fetchUrlMetadata
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

import com.example.loom.data.models.SandboxBlock

import kotlinx.coroutines.withContext
import org.jsoup.Jsoup


@Composable
fun LinkBlockRenderer(
    block: SandboxBlock,
    textState: TextFieldValue,
    handleTextChange: (TextFieldValue) -> Unit,
    commonModifier: Modifier,
    placeholderUI: @Composable (@Composable () -> Unit) -> Unit,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var showBlockMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (block.url.isEmpty()) {
            OutlinedTextField(
                value = block.secondaryContent,
                onValueChange = { onUpdate(block.copy(secondaryContent = it)) },
                placeholder = { Text("Paste web link here (e.g. https://google.com)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        val finalUrl = if (!block.secondaryContent.startsWith("http")) "https://${block.secondaryContent}" else block.secondaryContent
                        onUpdate(block.copy(url = finalUrl))
                    }) { Icon(Icons.Default.Check, "Save") }
                }
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(block.url))) } catch (e: Exception) {}
                },
                shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(block.url.removePrefix("https://").removePrefix("http://").substringBefore("/"), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(block.url, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { showBlockMenu = true }) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                    DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                        DropdownMenuItem(text = { Text("Delete Link", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                        DropdownMenuItem(text = { Text("Edit Link") }, onClick = { onUpdate(block.copy(url = "", secondaryContent = block.url)); showBlockMenu = false })
                    }
                }
            }
        }

        BasicTextField(
            value = textState, onValueChange = handleTextChange,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
            modifier = commonModifier.padding(top = 4.dp, start = 4.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = placeholderUI
        )
    }
}

@Composable
fun BookmarkBlockRenderer(
    block: SandboxBlock,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var showBlockMenu by remember { mutableStateOf(false) }
    var ogTitle by remember { mutableStateOf("") }
    var ogDesc by remember { mutableStateOf("") }
    var ogImage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(block.url) {
        if (block.url.isNotBlank() && block.url.contains(".")) {
            isLoading = true
            val fetchUrl = if (!block.url.startsWith("http")) "https://${block.url}" else block.url

            withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val doc = Jsoup.connect(fetchUrl)
                        .userAgent("Mozilla/5.0")
                        .timeout(4000)
                        .get()
                    ogTitle = doc.select("meta[property=og:title]").attr("content").ifEmpty { doc.title() }
                    ogDesc = doc.select("meta[property=og:description]").attr("content").ifEmpty { doc.select("meta[name=description]").attr("content") }
                    ogImage = doc.select("meta[property=og:image]").attr("content").ifEmpty { doc.select("meta[name=twitter:image]").attr("content") }
                } catch (e: Exception) {
                    ogTitle = "Preview unavailable"
                    ogDesc = "Could not load metadata for this link."
                    ogImage = ""
                }
            }
            isLoading = false
        } else {
            ogTitle = ""; ogDesc = ""; ogImage = ""
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Web Bookmark", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
        }

        DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
            DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
        }

        OutlinedTextField(
            value = block.url,
            onValueChange = { onUpdate(block.copy(url = it)) },
            placeholder = { Text("e.g., github.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (block.url.isEmpty()) MaterialTheme.colorScheme.outline else Color.Transparent,
                focusedBorderColor = if (block.url.isEmpty()) MaterialTheme.colorScheme.primary else Color.Transparent
            )
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
        } else if (ogTitle.isNotEmpty()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
                    try {
                        val clickUrl = if (!block.url.startsWith("http")) "https://${block.url}" else block.url
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clickUrl)))
                    } catch (e: Exception) {}
                },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text(ogTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        if (ogDesc.isNotEmpty()) {
                            Text(ogDesc, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(8.dp))
                        val displayUrl = block.url.replace("https://", "").replace("http://", "").replace("www.", "").substringBefore("/")
                        Text(displayUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (ogImage.isNotEmpty()) {
                        AsyncImage(
                            model = ogImage,
                            contentDescription = "Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.width(120.dp).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}