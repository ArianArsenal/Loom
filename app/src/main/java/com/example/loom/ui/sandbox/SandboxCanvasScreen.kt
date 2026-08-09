package com.example.loom.ui.sandbox

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.window.Dialog
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.platform.LocalContext
import java.io.InputStreamReader
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotionCanvasScreen(
    space: SandboxSpace,
    allSpaces: List<SandboxSpace>,
    onBack: () -> Unit,
    onNavigateToSpace: (String) -> Unit,
    onCreateLinkedSpace: () -> String,
    onSave: (SandboxSpace) -> Unit
) {
    var title by remember { mutableStateOf(space.title) }
    var spaceIcon by remember { mutableStateOf(space.icon) }
    var bannerUrl by remember { mutableStateOf(space.bannerUrl) }
    var isPrivate by remember { mutableStateOf(space.isPrivate) }

    var currentSpaceId by remember { mutableStateOf(space.id) }
    val blocks = remember { mutableStateListOf<SandboxBlock>() }

    if (currentSpaceId != space.id) {
        currentSpaceId = space.id
        title = space.title
        spaceIcon = space.icon
        bannerUrl = space.bannerUrl
        isPrivate = space.isPrivate
        blocks.clear()
        blocks.addAll(prepareBlocksForUI(space.blocks))
    } else if (blocks.isEmpty()) {
        blocks.addAll(prepareBlocksForUI(space.blocks))
    }

    val recentTableColors = remember { mutableStateListOf<Color>() }
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    val focusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val focusManager = LocalFocusManager.current
    val blockPositions = remember { mutableStateMapOf<String, Float>() }

    val canvasScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var showBannerPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val context = LocalContext.current

    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var templateNameInput by remember { mutableStateOf("") }

    val currentBlocks = blocks.toList()
    LaunchedEffect(title, spaceIcon, bannerUrl, isPrivate, currentBlocks) {
        if (title != space.title || spaceIcon != space.icon || bannerUrl != space.bannerUrl || isPrivate != space.isPrivate || cleanBlocksForDB(currentBlocks) != space.blocks) {
            onSave(space.copy(title = title, icon = spaceIcon, bannerUrl = bannerUrl, isPrivate = isPrivate, updatedAt = System.currentTimeMillis(), blocks = cleanBlocksForDB(currentBlocks)))
        }
    }

    // Dynamic Icon Parsing
    val iconParts = spaceIcon.split("|")
    val isCustomIcon = iconParts.size == 2
    val iconName = if (isCustomIcon) iconParts[0] else ""
    val iconColorHex = if (isCustomIcon) iconParts[1] else ""
    val iconTint = try { Color(android.graphics.Color.parseColor(iconColorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.onSurface }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(canvasScrollState)) {
            Box(modifier = Modifier.fillMaxWidth()) {

                // SMOOTH PARALLAX COVER IMAGE
                if (bannerUrl.isNotEmpty()) {
                    val bannerColor = try { Color(android.graphics.Color.parseColor(bannerUrl)) } catch(e:Exception){ Color.LightGray }
                    val scrollOffset = canvasScrollState.value.toFloat()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .graphicsLayer {
                                translationY = scrollOffset * 0.5f // Creates the depth/parallax effect
                                alpha = (1f - (scrollOffset / 500f)).coerceIn(0f, 1f) // Fades out as you scroll down
                            }
                            .background(bannerColor)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (bannerUrl.isNotEmpty()) 180.dp else 72.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    // NEW ICON BUTTON (Replaces Keyboard TextField)
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(24.dp))
                            .border(4.dp, MaterialTheme.colorScheme.background, RoundedCornerShape(24.dp))
                            .shadow(if (bannerUrl.isNotEmpty()) 8.dp else 0.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.1f))
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { showIconPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(targetState = isCustomIcon, label = "IconTransition") { custom ->
                            if (custom) {
                                val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                                if (resId != 0) {
                                    Icon(painterResource(resId), contentDescription = "Space Icon", tint = iconTint, modifier = Modifier.size(48.dp))
                                } else {
                                    Text("❓", fontSize = 42.sp) // Fallback if drawable isn't found
                                }
                            } else {
                                Text(spaceIcon.ifEmpty { "📄" }, fontSize = 42.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (bannerUrl.isEmpty()) {
                        TextButton(
                            onClick = { showBannerPicker = true },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp).padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Default.Palette, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Cover", color = Color.Gray, fontSize = 14.sp)
                        }
                    }

                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground, fontSize = 42.sp, letterSpacing = (-1).sp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (title.isEmpty()) Text("Untitled", style = MaterialTheme.typography.headlineLarge.copy(color = Color.LightGray, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp))
                                innerTextField()
                            }
                        },
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // ... [YOUR EXISTING BLOCKS FOREACH LOOP REMAINS UNCHANGED] ...
                    blocks.forEachIndexed { index, block ->
                        val currentNumberedIndex = if (block.type == BlockType.NUMBER) {
                            var count = 1
                            for (i in index - 1 downTo 0) {
                                if (blocks[i].type == BlockType.NUMBER) count++ else break
                            }
                            count
                        } else 1

                        key(block.id) {
                            Box(modifier = Modifier.onGloballyPositioned { coords ->
                                blockPositions[block.id] = coords.positionInParent().y
                            }) {
                                BlockRenderer(
                                    block = block,
                                    numberIndex = currentNumberedIndex,
                                    allSpaces = allSpaces,
                                    currentSpaceId = currentSpaceId,
                                    recentTableColors = recentTableColors,
                                    focusRequesters = focusRequesters,
                                    focusTargetId = focusedBlockId,
                                    allBlocks = blocks,
                                    onScrollToBlock = { targetId ->
                                        scope.launch {
                                            blockPositions[targetId]?.let { yPos ->
                                                canvasScrollState.animateScrollTo(yPos.toInt())
                                            }
                                        }
                                    },
                                    onMove = { offset ->
                                        val currentIndex = blocks.indexOfFirst { it.id == block.id }
                                        if (currentIndex != -1) {
                                            val newIndex = currentIndex + offset
                                            if (newIndex in 0 until blocks.size) {
                                                val temp = blocks[currentIndex]
                                                blocks[currentIndex] = blocks[newIndex]
                                                blocks[newIndex] = temp
                                            }
                                        }
                                    },
                                    onFocusRequest = { focusedBlockId = it },
                                    onScrollToTop = { scope.launch { canvasScrollState.animateScrollTo(0) } },
                                    onNavigateToSpace = onNavigateToSpace,
                                    onCreateLinkedSpace = onCreateLinkedSpace,
                                    onUpdate = { updatedBlock ->
                                        val safeIndex = blocks.indexOfFirst { it.id == updatedBlock.id }
                                        if (safeIndex != -1) blocks[safeIndex] = updatedBlock
                                    },
                                    onSplitBlock = { currentContent, nextType, nextContent ->
                                        val safeIndex = blocks.indexOfFirst { it.id == block.id }
                                        if (safeIndex != -1) {
                                            blocks[safeIndex] = blocks[safeIndex].copy(content = currentContent)
                                            val newBlockId = UUID.randomUUID().toString()
                                            blocks.add(safeIndex + 1, SandboxBlock(id = newBlockId, type = nextType, content = nextContent))
                                            focusedBlockId = newBlockId
                                        }
                                    },
                                    onDeleteBlock = { remainder, deleteEntirely ->
                                        val safeIndex = blocks.indexOfFirst { it.id == block.id }
                                        if (safeIndex != -1 && safeIndex < blocks.size) {
                                            if (deleteEntirely) {
                                                val prevId = if (safeIndex > 0) blocks[safeIndex - 1].id else null
                                                blocks.removeAt(safeIndex)
                                                if (blocks.isEmpty()) {
                                                    val newId = UUID.randomUUID().toString()
                                                    blocks.add(SandboxBlock(id = newId, type = BlockType.TEXT, content = "\u200B"))
                                                    focusedBlockId = newId
                                                } else {
                                                    prevId?.let {
                                                        focusedBlockId = it
                                                        try { focusRequesters[it]?.requestFocus() } catch(e:Exception){}
                                                    }
                                                }
                                            } else if (safeIndex > 0) {
                                                val prevBlock = blocks[safeIndex - 1]
                                                val isPrevTextBased = prevBlock.type in listOf(BlockType.TEXT, BlockType.HEADING, BlockType.HEADING_2, BlockType.HEADING_3, BlockType.HEADING_4, BlockType.BULLET, BlockType.NUMBER, BlockType.TODO, BlockType.QUOTE, BlockType.CALLOUT, BlockType.TOGGLE)
                                                if (isPrevTextBased) {
                                                    val prevId = prevBlock.id
                                                    blocks[safeIndex - 1] = prevBlock.copy(content = prevBlock.content + remainder)
                                                    blocks.removeAt(safeIndex)
                                                    focusedBlockId = prevId
                                                    try { focusRequesters[prevId]?.requestFocus() } catch(e:Exception){}
                                                } else {
                                                    blocks.removeAt(safeIndex - 1)
                                                }
                                            } else if (safeIndex == 0 && blocks.size == 1) {
                                                blocks[0] = blocks[0].copy(type = BlockType.TEXT, content = "\u200B" + remainder)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            Spacer(
                modifier = Modifier.fillMaxWidth().height(300.dp + imeBottomPadding).pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        val lastBlock = blocks.lastOrNull()
                        if (lastBlock != null && lastBlock.type == BlockType.TEXT && lastBlock.content == "\u200B") {
                            focusedBlockId = lastBlock.id
                        } else {
                            val newBlockId = UUID.randomUUID().toString()
                            blocks.add(SandboxBlock(id = newBlockId, type = BlockType.TEXT, content = "\u200B"))
                            focusedBlockId = newBlockId
                        }
                    })
                }
            )
        }

        // Floating Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 16.dp, start = 16.dp).background(if (bannerUrl.isNotEmpty()) Color.Black.copy(alpha=0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), CircleShape).size(40.dp)
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = if (bannerUrl.isNotEmpty()) Color.White else MaterialTheme.colorScheme.onSurface)
        }

        // Floating Options Menu & Cover Button
        var showSpaceMenu by remember { mutableStateOf(false) }
        var showPasteMarkdownDialog by remember { mutableStateOf(false) }
        var pastedMarkdownText by remember { mutableStateOf("") }

        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                try {
                    val stream = context.contentResolver.openInputStream(it)
                    val text = InputStreamReader(stream!!).readText()
                    val importedBlocks = parseMarkdownToBlocks(text)
                    blocks.clear()
                    blocks.addAll(importedBlocks)
                } catch(e: Exception){}
            }
        }

        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bannerUrl.isNotEmpty()) {
                IconButton(
                    onClick = { showBannerPicker = true },
                    modifier = Modifier.background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(8.dp)).height(32.dp).width(100.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Change", color = Color.White, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
            }

            Box {
                IconButton(
                    onClick = { showSpaceMenu = true },
                    modifier = Modifier.background(if (bannerUrl.isNotEmpty()) Color.Black.copy(alpha=0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), CircleShape).size(40.dp)
                ) {
                    Icon(Icons.Default.MoreVert, "Options", tint = if (bannerUrl.isNotEmpty()) Color.White else MaterialTheme.colorScheme.onSurface)
                }

                DropdownMenu(expanded = showSpaceMenu, onDismissRequest = { showSpaceMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (isPrivate) "Unlock Space" else "Lock Space") },
                        leadingIcon = { Icon(if (isPrivate) Icons.Default.LockOpen else Icons.Default.Lock, null) },
                        onClick = {
                            isPrivate = !isPrivate
                            showSpaceMenu = false
                        }
                    )
                    DropdownMenuItem(text = { Text("Export to Markdown") }, onClick = {
                        val mdStr = exportSpaceToMarkdown(title, blocks.toList())
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, mdStr)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Export Space"))
                        showSpaceMenu = false
                    })
                    DropdownMenuItem(text = { Text("Import .md File") }, onClick = {
                        importLauncher.launch("*/*")
                        showSpaceMenu = false
                    })
                    DropdownMenuItem(text = { Text("Paste Markdown") }, onClick = {
                        showPasteMarkdownDialog = true
                        showSpaceMenu = false
                    })
                    DropdownMenuItem(
                        text = { Text("Save Space Layout") },
                        leadingIcon = { Icon(Icons.Default.Save, null) },
                        onClick = {
                            showSaveTemplateDialog = true
                            showSpaceMenu = false
                        }
                    )
                }
            }
        }

        // Paste Markdown Dialog
        if (showPasteMarkdownDialog) {
            AlertDialog(
                onDismissRequest = { showPasteMarkdownDialog = false },
                title = { Text("Paste Markdown") },
                text = {
                    OutlinedTextField(
                        value = pastedMarkdownText,
                        onValueChange = { pastedMarkdownText = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp),
                        placeholder = { Text("Paste your markdown here...") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (pastedMarkdownText.isNotBlank()) {
                            val importedBlocks = parseMarkdownToBlocks(pastedMarkdownText)
                            blocks.clear()
                            blocks.addAll(importedBlocks)
                        }
                        pastedMarkdownText = ""
                        showPasteMarkdownDialog = false
                    }) { Text("Import") }
                },
                dismissButton = {
                    TextButton(onClick = { pastedMarkdownText = ""; showPasteMarkdownDialog = false }) { Text("Cancel") }
                }
            )
        }
    }

    if (showSaveTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false; templateNameInput = "" },
            title = { Text("Save Space Layout") },
            text = {
                Column {
                    Text("Save this space's current layout to use as a template later.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = templateNameInput,
                        onValueChange = { templateNameInput = it },
                        placeholder = { Text("e.g., Weekly Workout Plan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (templateNameInput.isNotBlank()) {
                        com.example.loom.utils.TemplateManager.saveCustomTemplate(context, templateNameInput, spaceIcon, blocks)
                        showSaveTemplateDialog = false
                        templateNameInput = ""
                    }
                }) { Text("Save Template") }
            },
            dismissButton = { TextButton(onClick = { showSaveTemplateDialog = false; templateNameInput = "" }) { Text("Cancel") } }
        )
    }

    // ORIGINAL BANNER PICKER
    if (showBannerPicker) {
        var hue by remember { mutableFloatStateOf(210f) }
        var saturation by remember { mutableFloatStateOf(0.9f) }
        var value by remember { mutableFloatStateOf(0.9f) }

        AlertDialog(
            onDismissRequest = { showBannerPicker = false },
            title = { Text("Select Cover Color") },
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
                        val pickedColor = Color.hsv(hue, saturation, value)
                        val hex = String.format("#%06X", (0xFFFFFF and pickedColor.toArgb()))
                        bannerUrl = hex
                        showBannerPicker = false
                    }) { Text("Apply Color") }

                    TextButton(modifier = Modifier.fillMaxWidth(), onClick = { bannerUrl = ""; showBannerPicker = false }) { Text("Remove Cover", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = { showBannerPicker = false }) { Text("Close") } }
        )
    }

    // NEW ICON PICKER DIALOG
    if (showIconPicker) {
        SpaceIconPicker(
            initialIcon = spaceIcon,
            onIconSelected = { newIcon ->
                spaceIcon = newIcon
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceIconPicker(
    initialIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val initialColorHex = if (initialIcon.contains("|")) initialIcon.split("|")[1] else "#607D8B"
    val initialColor = try { Color(android.graphics.Color.parseColor(initialColorHex)) } catch (e: Exception) { Color(0xFF607D8B) }
    val hsv = remember { FloatArray(3).apply { android.graphics.Color.colorToHSV(initialColor.toArgb(), this) } }

    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    val selectedColor = Color.hsv(hue, saturation, value)

    var selectedCategory by remember { mutableStateOf("All") }

    // EXACT MAP OF YOUR DRAWABLES TO PREVENT MISSING DOTS
    val iconLibrary = remember {
        mapOf(
            "Arrows" to listOf("arrow", "arrow_circle_down", "arrow_circle_left", "arrow_circle_right", "arrow_circle_up", "arrow_down", "arrow_left", "arrow_right", "arrow_single_down", "arrow_single_left", "arrow_single_right", "arrow_single_up", "arrow_square_down", "arrow_square_left", "arrow_square_right", "arrow_square_up", "backward", "chevrons", "chevrons_down", "chevrons_left", "chevrons_right", "forward"),
            "Currency" to listOf("dollar", "euro", "franc", "pound", "ruble", "rupee"),
            "E-Commerce" to listOf("bag", "bag_2", "basket", "box", "box_2", "card", "card_2", "card_3", "cart", "cart_delete", "cart_remove", "conveyor_belt", "factory", "hand_truck", "location", "offer", "qr", "sale", "shop", "shopping_cart", "shopping_cart_2", "shopping_cart_3", "tag", "tag_2", "truck"),
            "Emojis" to listOf("confused_emoji", "cool_emoji", "crying_emoji", "grinning_squinting_emoji", "happy_emoji", "heart_eyes_emoji", "laugh_emoji", "sad_emoji", "shocked_emoji", "smiling_emoji", "smiling_with_eyes_emoji", "surprised_emoji"),
            "Files" to listOf("doc", "doc_remove", "file", "file_attachment", "file_audio", "file_code", "file_contract", "file_figma", "file_form", "file_image", "file_image_2", "file_invoice", "file_list", "file_mp4", "file_notes", "file_spreadsheet", "file_text", "file_vector", "folder", "folder_delete", "folder_empty", "folder_remove", "png"),
            "Finance" to listOf("bank", "bill", "cash", "coin", "coin_2", "diamond", "money_plant", "piggy_bank", "safe", "saving", "trend", "trend_down", "trend_down_square"),
            "Food" to listOf("burger", "cake", "candy", "cutlery", "dish", "drink", "egg", "fork", "ice_cream", "pizza", "pizza_2", "popsicle", "spoon"),
            "Gender" to listOf("bisexual", "femaile", "gay", "genderless", "hetro", "intersex", "lesbian", "male", "non_binary_transgender", "other_gender", "transgender", "transgender_2"),
            "Gestures" to listOf("clap", "folded_hands", "free_drag", "hand", "horizontal_scroll", "multi_touch", "ok", "page_move", "point", "point_down", "point_left", "point_right", "rotate", "scan", "scan_fingerprint", "screen_rotate", "scroll", "scroll_down", "scroll_left", "scroll_left_1", "swipe_left", "swipe_right", "tap", "tap_1", "thumbs", "thumbs_down", "touch_hold", "transform", "vibrate", "_2", "_2_double_tap", "_2_scroll_left", "_2_scroll_right", "_2_scroll_up", "_2_scroll_up_1", "_2_tap_1", "_2_touch_hold", "_3", "_3_scroll_down", "_3_scroll_up", "_3_tap_scroll"),
            "Health" to listOf("blood", "bottle", "dropper", "first", "firstaid_2", "flask", "heart_beat", "injection", "lungs", "medical_logo", "microscope", "pills", "stethoscope", "tablet", "test_tube", "tooth"),
            "Interface" to listOf("analytics", "at_the_rate", "bell", "bell_2", "bluetooth", "bookmark", "bot", "bug", "bulb", "calculator", "calendar", "call", "camera", "caution", "center_align", "center_align_2", "checklist", "chip", "clock", "cloud", "cloud_down", "coffee_cup_1", "coffee_cup_2", "cookie", "copy", "crop", "cross", "cut", "dashboard", "dashboard_2", "dashboard_3", "dashboard_4", "delete", "download", "drawer", "eject", "eraser", "filter", "flag", "flag_2", "flip", "floppy", "gift", "globe", "grid", "grid_2", "headphone", "heart", "hide", "home", "home_1", "info", "key", "layer", "left_align", "left_align_2", "link", "list", "lock", "login", "logout", "magic_wand", "mail", "mail_gift", "mail_open", "mail_surprise", "map", "maximize", "megaphone", "menu", "menu_2", "message", "message_2", "mic", "mic_2", "minimize", "move", "movie_clapper", "music", "music_2", "music_3", "mute", "navigation", "navigation_2", "note", "paint_brush", "paint_brush_2", "paint_bucket", "paint_roller", "paper_clip", "paper_clip_2", "paste", "pause", "pen", "pencil", "pencil_2", "pencil_3", "pencil_ruler", "pen_tool", "phone", "phone_setting", "photo", "pie", "pin", "play", "puzzle", "puzzle_2", "qr", "question", "question_2", "record", "rectangle", "right_align", "right_align_2", "rss", "ruler", "search", "send", "send_2", "send_3", "server", "setting", "setting_2", "setting_3", "shape", "shield", "shield_2", "shuffle", "signal", "speaker", "square", "star", "stopwatch", "suitcase", "switch_1", "sync", "target", "target_2", "tick", "tick_2", "unhide", "upload", "user", "user_caution", "user_delete", "user_female", "user_male", "user_remove", "video_camera", "zap", "zoom_in"),
            "Logos" to listOf("apple", "behance", "codepen", "dribbble", "dropbox", "facebook", "facebook_2", "fb_messenger", "google", "instagram", "linkedin", "paypal", "pinterest", "product_hunt", "skype", "snapchat", "spotify", "tik_tok", "tumblr", "twitch", "uber", "webflow"),
            "Misc" to listOf("automation", "bus", "car", "fire", "hot_air_balloon", "plane", "rocket", "satellite", "ship", "trophy"),
            "Objects" to listOf("anchor", "balloon", "balloon_2", "crown", "flashlight", "frame", "guitar", "sofa", "tv"),
            "Weather" to listOf("night", "rain_heavy", "rain_light", "snow", "snowflake", "snowman", "sun", "sun_2", "sun_3", "sun_4", "sunny", "thunderstorm", "tornado")
        )
    }

    val categoriesList = listOf("All") + iconLibrary.keys.toList()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Select Icon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(selectedColor).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha=0.2f), CircleShape))
                    }
                    TextButton(onClick = { onIconSelected("") }, contentPadding = PaddingValues(0.dp)) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // HSV PICKER - SATURATION & VALUE
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.hsv(hue, 1f, 1f))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                saturation = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                value = 1f - (down.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                do {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        saturation = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        value = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                        change.consume()
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        drawCircle(Color.White, 6.dp.toPx(), Offset(saturation * size.width, (1f - value) * size.height), style = Stroke(2.dp.toPx()))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // HSV PICKER - HUE SLIDER
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(50))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                hue = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                                do {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        hue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                                        change.consume()
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    drawRect(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                    drawCircle(Color.White, 10.dp.toPx(), Offset((hue / 360f) * size.width, size.height / 2), style = Stroke(3.dp.toPx()))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // MODERNIZED CATEGORY FILTERS (Sleek monochrome look, no purple)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoriesList) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                                selectedLabelColor = MaterialTheme.colorScheme.surface,
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = null // Removes the default border for a much cleaner look
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                val iconsToShow = if (selectedCategory == "All") {
                    iconLibrary.values.flatten()
                } else {
                    iconLibrary[selectedCategory] ?: emptyList()
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 44.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(iconsToShow) { iconName ->
                        val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)

                        if (resId != 0) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val hexColor = String.format("#%06X", (0xFFFFFF and selectedColor.toArgb()))
                                        onIconSelected("$iconName|$hexColor")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = resId),
                                    contentDescription = null,
                                    tint = selectedColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}