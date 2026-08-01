package com.example.loom.ui.sandbox

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
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

    var currentSpaceId by remember { mutableStateOf(space.id) }
    val blocks = remember { mutableStateListOf<SandboxBlock>() }

    if (currentSpaceId != space.id) {
        currentSpaceId = space.id
        title = space.title
        spaceIcon = space.icon
        bannerUrl = space.bannerUrl
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
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(title, spaceIcon, bannerUrl, blocks.toList()) {
        if (title != space.title || spaceIcon != space.icon || bannerUrl != space.bannerUrl || cleanBlocksForDB(blocks.toList()) != space.blocks) {
            onSave(space.copy(title = title, icon = spaceIcon, bannerUrl = bannerUrl, updatedAt = System.currentTimeMillis(), blocks = cleanBlocksForDB(blocks.toList())))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = if (bannerUrl.isNotEmpty()) Modifier.padding(start = 8.dp).background(Color.Black.copy(alpha=0.3f), CircleShape).size(36.dp) else Modifier
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = if (bannerUrl.isNotEmpty()) Color.White else MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(canvasScrollState)) {

                Box(modifier = Modifier.fillMaxWidth()) {
                    if (bannerUrl.isNotEmpty()) {
                        val bannerColor = try { Color(android.graphics.Color.parseColor(bannerUrl)) } catch(e:Exception){ Color.LightGray }
                        Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(bannerColor)) {
                            IconButton(
                                onClick = { showBannerPicker = true },
                                modifier = Modifier.align(Alignment.TopEnd).padding(top = padding.calculateTopPadding() + 8.dp, end = 16.dp).background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(8.dp)).height(32.dp).width(100.dp)
                            ) { Row(verticalAlignment = Alignment.CenterVertically){ Icon(Icons.Default.Palette, null, tint=Color.White, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Change", color=Color.White, fontSize=12.sp) } }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (bannerUrl.isNotEmpty()) 180.dp else padding.calculateTopPadding() + 16.dp)
                            .padding(horizontal = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(24.dp))
                                .border(4.dp, MaterialTheme.colorScheme.background, RoundedCornerShape(24.dp))
                                .shadow(if (bannerUrl.isNotEmpty()) 8.dp else 0.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = spaceIcon,
                                onValueChange = { text ->
                                    val clean = text.replace("\n", "").trim()
                                    if (clean.isEmpty()) {
                                        spaceIcon = ""
                                    } else if (clean.length > spaceIcon.length) {
                                        spaceIcon = clean.removePrefix(spaceIcon).trim().take(4)
                                    } else {
                                        spaceIcon = clean.take(4)
                                    }
                                },
                                textStyle = TextStyle(fontSize = 48.sp, textAlign = TextAlign.Center),
                                modifier = Modifier.fillMaxWidth(),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (spaceIcon.isEmpty()) Text("📄", fontSize = 48.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                        inner()
                                    }
                                }
                            )
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

                        blocks.forEachIndexed { index, block ->
                            val currentNumberedIndex = if (block.type == BlockType.NUMBER) {
                                var count = 1
                                for (i in index - 1 downTo 0) {
                                    if (blocks[i].type == BlockType.NUMBER) count++ else break
                                }
                                count
                            } else 1

                            // NEW: Wrap in a Box to track the block's Y position for TOC scrolling
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
                                    allBlocks = blocks, // NEW: Pass all blocks for TOC
                                    onScrollToBlock = { targetId -> // NEW: Scroll logic for TOC
                                        scope.launch {
                                            blockPositions[targetId]?.let { yPos ->
                                                canvasScrollState.animateScrollTo(yPos.toInt())
                                            }
                                        }
                                    },
                                    onMove = { offset -> // NEW: Drag & Drop swap logic
                                        val newIndex = index + offset
                                        if (newIndex in 0 until blocks.size) {
                                            val temp = blocks[index]
                                            blocks[index] = blocks[newIndex]
                                            blocks[newIndex] = temp
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
                                                    blocks.add(SandboxBlock(id = newId, type = BlockType.TEXT, content = ZWSP))
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
                                                blocks[0] = blocks[0].copy(type = BlockType.TEXT, content = ZWSP + remainder)
                                            }
                                        }
                                    }
                                )
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
                            if (lastBlock != null && lastBlock.type == BlockType.TEXT && lastBlock.content == ZWSP) {
                                focusedBlockId = lastBlock.id
                            } else {
                                val newBlockId = UUID.randomUUID().toString()
                                blocks.add(SandboxBlock(id = newBlockId, type = BlockType.TEXT, content = ZWSP))
                                focusedBlockId = newBlockId
                            }
                        })
                    }
                )
            }
        }
    }

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
}