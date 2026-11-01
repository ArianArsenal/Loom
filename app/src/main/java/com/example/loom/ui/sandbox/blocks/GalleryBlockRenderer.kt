package com.example.loom.ui.sandbox.blocks

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import com.example.loom.data.models.GalleryItem
import com.example.loom.data.models.SandboxBlock
import com.example.loom.ui.sandbox.copyMediaToLocal
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class GallerySettings(
    val showDate: Boolean = true,
    val showTitle: Boolean = true,
    val showComment: Boolean = true,
    val showNumbers: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GalleryBlockRenderer(
    block: SandboxBlock,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var showLightbox by remember { mutableStateOf(false) }
    var lightboxInitialPage by remember { mutableIntStateOf(0) }
    var showDatePickerFor by remember { mutableStateOf<GalleryItem?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val settings = try {
        if (block.secondaryContent.isNotBlank()) Json.decodeFromString<GallerySettings>(block.secondaryContent)
        else GallerySettings(showNumbers = block.isChecked)
    } catch (_: Exception) { GallerySettings(showNumbers = block.isChecked) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newItems = uris.mapNotNull { uri ->
                val localUrl = copyMediaToLocal(context, uri)
                if (localUrl != null) GalleryItem(url = localUrl) else null
            }
            onUpdate(block.copy(galleryItems = block.galleryItems + newItems))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val cleanTitle = block.content.replace("\u200B", "")
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = cleanTitle,
                    onValueChange = { onUpdate(block.copy(content = it)) },
                    readOnly = block.isLocked,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    decorationBox = { inner ->
                        if (cleanTitle.isEmpty()) {
                            Text("Gallery Title...", color = Color.Gray, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        inner()
                    }
                )
                if (block.isLocked) {
                    Icon(Icons.Default.Lock, "Locked", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(16.dp).padding(start = 4.dp))
                }
            }

            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray)
                }
                MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surfaceTint = Color.Transparent, surface = MaterialTheme.colorScheme.background)) {
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (block.isLocked) "Unlock Gallery" else "Lock Gallery") },
                            leadingIcon = { Icon(if (block.isLocked) Icons.Outlined.LockOpen else Icons.Default.Lock, null) },
                            onClick = { onUpdate(block.copy(isLocked = !block.isLocked)); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Gallery Settings") },
                            onClick = { showSettingsDialog = true; showMenu = false }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Entire Gallery", color = MaterialTheme.colorScheme.error) },
                            onClick = { onDeleteBlock("", true); showMenu = false }
                        )
                    }
                }
            }
        }

        val gridItems = block.galleryItems + null
        val chunks = gridItems.chunked(2)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chunks.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (item in rowItems) {
                        if (item == null) {
                            if (!block.isLocked) {
                                var showAddMenu by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { showAddMenu = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add", tint = Color.Gray, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Add Media", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                    DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                        DropdownMenuItem(text = { Text("Add Photos") }, onClick = { galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)); showAddMenu = false })
                                        DropdownMenuItem(text = { Text("Add Solid Color") }, onClick = { showColorPicker = true; showAddMenu = false })
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        } else {
                            val itemIndex = block.galleryItems.indexOf(item)
                            val isSolidColor = item.url.startsWith("#")

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                    .clickable {
                                        lightboxInitialPage = itemIndex
                                        showLightbox = true
                                    }
                            ) {
                                if (isSolidColor) {
                                    val solidCol = try { Color(item.url.toColorInt()) } catch (_: Exception) { Color.Gray }
                                    Box(modifier = Modifier.fillMaxSize().background(solidCol))
                                } else {
                                    AsyncImage(
                                        model = item.url,
                                        contentDescription = "Gallery Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                if (settings.showDate) {
                                    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.timestamp))
                                    Box(
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .align(Alignment.TopStart)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clickable(enabled = !block.isLocked) { showDatePickerFor = item }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(dateStr, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (!block.isLocked) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clickable {
                                                val updatedList = block.galleryItems.toMutableList().apply { remove(item) }
                                                onUpdate(block.copy(galleryItems = updatedList))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }

                                if (settings.showTitle || settings.showComment || settings.showNumbers) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                                            .padding(horizontal = 10.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            if (settings.showTitle) {
                                                BasicTextField(
                                                    value = item.title,
                                                    onValueChange = { newTitle ->
                                                        val updatedList = block.galleryItems.toMutableList()
                                                        updatedList[itemIndex] = item.copy(title = newTitle)
                                                        onUpdate(block.copy(galleryItems = updatedList))
                                                    },
                                                    readOnly = block.isLocked,
                                                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                    cursorBrush = SolidColor(Color.White),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    decorationBox = { inner ->
                                                        if (item.title.isEmpty() && !block.isLocked) Text("Title...", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                        inner()
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }
                                            if (settings.showComment) {
                                                BasicTextField(
                                                    value = item.comment,
                                                    onValueChange = { newComment ->
                                                        val updatedList = block.galleryItems.toMutableList()
                                                        updatedList[itemIndex] = item.copy(comment = newComment)
                                                        onUpdate(block.copy(galleryItems = updatedList))
                                                    },
                                                    readOnly = block.isLocked,
                                                    textStyle = TextStyle(color = Color.LightGray, fontSize = 12.sp),
                                                    cursorBrush = SolidColor(Color.LightGray),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    decorationBox = { inner ->
                                                        if (item.comment.isEmpty() && !block.isLocked) Text("Add comment...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                                                        inner()
                                                    }
                                                )
                                            }
                                        }

                                        if (settings.showNumbers) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("${itemIndex + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Gallery Visibility Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Show Date Badge")
                        Switch(checked = settings.showDate, onCheckedChange = { onUpdate(block.copy(secondaryContent = Json.encodeToString(settings.copy(showDate = it)))) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Show Title")
                        Switch(checked = settings.showTitle, onCheckedChange = { onUpdate(block.copy(secondaryContent = Json.encodeToString(settings.copy(showTitle = it)))) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Show Comment")
                        Switch(checked = settings.showComment, onCheckedChange = { onUpdate(block.copy(secondaryContent = Json.encodeToString(settings.copy(showComment = it)))) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Show Position Numbers")
                        Switch(checked = settings.showNumbers, onCheckedChange = { onUpdate(block.copy(secondaryContent = Json.encodeToString(settings.copy(showNumbers = it)))) })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("Done") } }
        )
    }

    if (showColorPicker) {
        var hue by remember { mutableFloatStateOf(210f) }
        var saturation by remember { mutableFloatStateOf(0.9f) }
        var value by remember { mutableFloatStateOf(0.9f) }

        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Add Solid Color") },
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
                }
            },
            confirmButton = {
                Button(onClick = {
                    val hex = String.format("#%06X", (0xFFFFFF and Color.hsv(hue, saturation, value).toArgb()))
                    val newItem = GalleryItem(url = hex)
                    onUpdate(block.copy(galleryItems = block.galleryItems + newItem))
                    showColorPicker = false
                }) { Text("Add Color") }
            },
            dismissButton = { TextButton(onClick = { showColorPicker = false }) { Text("Cancel") } }
        )
    }

    if (showDatePickerFor != null) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = showDatePickerFor!!.timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val updatedList = block.galleryItems.toMutableList()
                        val idx = updatedList.indexOfFirst { it.id == showDatePickerFor!!.id }
                        if (idx != -1) {
                            updatedList[idx] = updatedList[idx].copy(timestamp = millis)
                            onUpdate(block.copy(galleryItems = updatedList))
                        }
                    }
                    showDatePickerFor = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerFor = null }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showLightbox && block.galleryItems.isNotEmpty()) {
        val pagerState = rememberPagerState(initialPage = lightboxInitialPage, pageCount = { block.galleryItems.size })

        Dialog(
            onDismissRequest = { showLightbox = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val currentItem = block.galleryItems[page]
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                        if (currentItem.url.startsWith("#")) {
                            val solidCol = try { Color(currentItem.url.toColorInt()) } catch (_: Exception) { Color.Gray }
                            Box(modifier = Modifier.fillMaxSize().background(solidCol))
                        } else {
                            AsyncImage(
                                model = currentItem.url,
                                contentDescription = "Full Screen Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if ((settings.showTitle && currentItem.title.isNotBlank()) || (settings.showComment && currentItem.comment.isNotBlank())) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                                    .padding(24.dp)
                                    .padding(bottom = 32.dp)
                            ) {
                                if (settings.showTitle && currentItem.title.isNotBlank()) {
                                    Text(currentItem.title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                if (settings.showComment && currentItem.comment.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(currentItem.comment, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showLightbox = false },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }

                    if (settings.showNumbers) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${block.galleryItems.size}",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}