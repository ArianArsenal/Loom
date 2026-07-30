package com.example.loom.ui.sandbox

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.loom.data.local.dao.SandboxDao
import com.example.loom.data.local.entity.NotionSpaceEntity
import com.example.loom.data.models.BlockType
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

private const val ZWSP = "\u200B"

// ==========================================
// 1. VIEWMODEL & FACTORY
// ==========================================

class SandboxViewModel(private val sandboxDao: SandboxDao) : ViewModel() {
    val allSpaces: StateFlow<List<SandboxSpace>> = sandboxDao.getAllSpaces()
        .map { entities -> entities.map { it.toDomainModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveSpace(space: SandboxSpace) {
        viewModelScope.launch {
            sandboxDao.saveSpace(NotionSpaceEntity.fromDomainModel(space))
        }
    }

    fun deleteSpace(space: SandboxSpace) {
        viewModelScope.launch {
            sandboxDao.deleteSpace(NotionSpaceEntity.fromDomainModel(space))
        }
    }
}

class SandboxViewModelFactory(private val sandboxDao: SandboxDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SandboxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SandboxViewModel(sandboxDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ==========================================
// 2. MAIN ROUTER & HUB UI
// ==========================================

enum class SandboxRoute { HUB, CANVAS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandboxMainScreen(viewModel: SandboxViewModel) {
    var currentRoute by remember { mutableStateOf(SandboxRoute.HUB) }
    var activeSpace by remember { mutableStateOf<SandboxSpace?>(null) }
    val allSpaces by viewModel.allSpaces.collectAsState()

    if (currentRoute == SandboxRoute.HUB) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("My Canvas Spaces", fontWeight = FontWeight.Bold) }) },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    val newSpace = SandboxSpace(title = "Untitled", blocks = listOf(SandboxBlock(content = ZWSP)))
                    viewModel.saveSpace(newSpace)
                    activeSpace = newSpace
                    currentRoute = SandboxRoute.CANVAS
                }) { Icon(Icons.Default.Add, "New") }
            }
        ) { paddingValues ->
            LazyColumn(modifier = Modifier.padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allSpaces) { space ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            activeSpace = space
                            currentRoute = SandboxRoute.CANVAS
                        }
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${space.icon} ${space.title.ifBlank { "Untitled" }}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.deleteSpace(space) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    } else {
        activeSpace?.let { space ->
            NotionCanvasScreen(
                space = space,
                onBack = { currentRoute = SandboxRoute.HUB },
                onSave = { updatedSpace ->
                    activeSpace = updatedSpace
                    viewModel.saveSpace(updatedSpace)
                }
            )
        }
    }
}

// ==========================================
// 3. NOTION CANVAS ENGINE (Mirrors DiaryScreen)
// ==========================================

fun prepareBlocksForUI(blocks: List<SandboxBlock>): List<SandboxBlock> {
    if (blocks.isEmpty()) return listOf(SandboxBlock(content = ZWSP))
    return blocks.map {
        val needsZWSP = it.type in listOf(BlockType.TEXT, BlockType.HEADING, BlockType.BULLET, BlockType.NUMBER, BlockType.TODO, BlockType.QUOTE, BlockType.CALLOUT, BlockType.TOGGLE)
        val newContent = if (needsZWSP && !it.content.startsWith(ZWSP)) ZWSP + it.content else it.content
        it.copy(content = newContent, children = prepareBlocksForUI(it.children).takeIf { c -> c.isNotEmpty() } ?: emptyList())
    }
}

fun cleanBlocksForDB(blocks: List<SandboxBlock>): List<SandboxBlock> {
    return blocks.map {
        it.copy(content = it.content.replace(ZWSP, ""), children = cleanBlocksForDB(it.children))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotionCanvasScreen(space: SandboxSpace, onBack: () -> Unit, onSave: (SandboxSpace) -> Unit) {
    var title by remember { mutableStateOf(space.title) }

    // Using mutableStateListOf identically to your DiaryScreen
    val blocks = remember { mutableStateListOf<SandboxBlock>().apply { addAll(prepareBlocksForUI(space.blocks)) } }

    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    val focusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(focusedBlockId) {
        focusedBlockId?.let { id ->
            try { focusRequesters[id]?.requestFocus() } catch (e: Exception) {}
        }
    }

    LaunchedEffect(title, blocks.toList()) {
        onSave(space.copy(title = title, blocks = cleanBlocksForDB(blocks.toList())))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        // Changed to Column + verticalScroll EXACTLY like your NotesScreen to fix jumping
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 8.dp),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) Text("Untitled", style = MaterialTheme.typography.headlineLarge.copy(color = Color.Gray))
                        innerTextField()
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )

                blocks.forEachIndexed { index, block ->
                    val currentNumberedIndex = if (block.type == BlockType.NUMBER) {
                        var count = 1
                        for (i in 0 until index) { if (blocks[i].type == BlockType.NUMBER) count++ }
                        count
                    } else 1

                    BlockRenderer(
                        block = block,
                        numberIndex = currentNumberedIndex,
                        focusRequesters = focusRequesters,
                        focusTargetId = focusedBlockId,
                        onFocusRequest = { focusedBlockId = it },
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
                        onDeleteBlock = { remainder ->
                            val safeIndex = blocks.indexOfFirst { it.id == block.id }
                            if (safeIndex > 0) {
                                val prevBlock = blocks[safeIndex - 1]
                                blocks[safeIndex - 1] = prevBlock.copy(content = prevBlock.content + remainder)
                                blocks.removeAt(safeIndex)
                                focusedBlockId = prevBlock.id
                            } else if (safeIndex == 0 && blocks.size == 1) {
                                blocks[0] = blocks[0].copy(type = BlockType.TEXT, content = ZWSP + remainder)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Strictly limits the "click to add" area to the bottom empty space
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .pointerInput(Unit) {
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
}

@Composable
fun BlockRenderer(
    block: SandboxBlock,
    numberIndex: Int,
    focusRequesters: MutableMap<String, FocusRequester>,
    focusTargetId: String?,
    onFocusRequest: (String) -> Unit,
    onUpdate: (SandboxBlock) -> Unit,
    onSplitBlock: (String, BlockType, String) -> Unit,
    onDeleteBlock: (String) -> Unit
) {
    val focusRequester = focusRequesters.getOrPut(block.id) { FocusRequester() }
    var textState by remember { mutableStateOf(TextFieldValue(block.content, TextRange(block.content.length))) }
    var showMenu by remember { mutableStateOf(false) }

    val isFocused = focusTargetId == block.id

    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUpdate(block.copy(url = it.toString())) }
    }

    if (textState.text != block.content) {
        textState = textState.copy(text = block.content, selection = TextRange(block.content.length))
    }

    val handleTextChange: (TextFieldValue) -> Unit = { newTextState ->
        val newText = newTextState.text
        val needsZWSP = block.type in listOf(BlockType.TEXT, BlockType.HEADING, BlockType.BULLET, BlockType.NUMBER, BlockType.TODO, BlockType.QUOTE, BlockType.CALLOUT, BlockType.TOGGLE)

        if (!needsZWSP) {
            textState = newTextState
            onUpdate(block.copy(content = newText))
        } else if (newText != block.content) {
            if (!newText.startsWith(ZWSP) || newText.isEmpty()) {
                val remainder = newText.replace(ZWSP, "")
                onDeleteBlock(remainder)
            }
            else if (newText.contains('\n')) {
                val parts = newText.substring(1).split('\n', limit = 2)
                val cleanCurrent = parts[0]
                val nextContent = ZWSP + (parts.getOrNull(1) ?: "")

                if (cleanCurrent.isEmpty() && block.type != BlockType.TEXT && block.type != BlockType.TOGGLE) {
                    textState = newTextState.copy(text = ZWSP, selection = TextRange(1))
                    onUpdate(block.copy(type = BlockType.TEXT, content = ZWSP))
                } else {
                    val nextType = if (block.type in listOf(BlockType.TODO, BlockType.BULLET, BlockType.NUMBER)) block.type else BlockType.TEXT
                    onSplitBlock(ZWSP + cleanCurrent, nextType, nextContent)
                }
            } else {
                textState = newTextState
                onUpdate(block.copy(content = newText))
                showMenu = newText.endsWith("/")
            }
        } else {
            textState = newTextState
        }
    }

    val commonModifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 24.dp)
        .focusRequester(focusRequester)
        .onFocusChanged { state -> if (state.isFocused) onFocusRequest(block.id) }

    val placeholderUI: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { inner ->
        if (textState.text.replace(ZWSP, "").isEmpty() && isFocused) Text("Type '/' for commands", color = Color.LightGray) else inner()
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        when (block.type) {
            BlockType.TEXT -> {
                BasicTextField(
                    value = textState, onValueChange = handleTextChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    modifier = commonModifier, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = placeholderUI
                )
            }
            BlockType.CODE -> {
                Surface(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = textState, onValueChange = handleTextChange,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFD4D4D4), fontFamily = FontFamily.Monospace),
                        modifier = commonModifier.padding(12.dp), cursorBrush = SolidColor(Color.White),
                        decorationBox = { inner -> if (textState.text.isEmpty()) Text("// write code here", color = Color.Gray, fontFamily = FontFamily.Monospace) else inner() }
                    )
                }
            }
            BlockType.IMAGE, BlockType.AUDIO, BlockType.FILE -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            val iconStr = when (block.type) { BlockType.IMAGE -> "🖼️"; BlockType.AUDIO -> "🎵"; else -> "📁" }
                            Text(iconStr, fontSize = 24.sp, modifier = Modifier.padding(end = 16.dp))

                            if (block.url.isEmpty()) {
                                TextButton(onClick = {
                                    val mimeType = when (block.type) { BlockType.IMAGE -> "image/*"; BlockType.AUDIO -> "audio/*"; else -> "*/*" }
                                    mediaLauncher.launch(mimeType)
                                }) { Text("Select ${block.type.name.lowercase().capitalize()}") }
                            } else {
                                Text("File Attached", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onUpdate(block.copy(url = "")) }) { Icon(Icons.Default.Close, "Clear") }
                            }
                        }
                    }
                    BasicTextField(
                        value = textState, onValueChange = handleTextChange,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                        modifier = commonModifier.padding(top = 4.dp, start = 4.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner -> if (textState.text.isEmpty()) Text("Write a caption...", color = Color.LightGray, fontSize = 12.sp) else inner() }
                    )
                }
            }
            BlockType.HEADING -> {
                BasicTextField(
                    value = textState, onValueChange = handleTextChange,
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground),
                    modifier = commonModifier, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner -> if (textState.text.replace(ZWSP,"").isEmpty()) Text("Heading", color = Color.LightGray, style = MaterialTheme.typography.titleLarge) else inner() }
                )
            }
            BlockType.BULLET -> {
                Row(verticalAlignment = Alignment.Top) {
                    Text("•", modifier = Modifier.padding(end = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    BasicTextField(value = textState, onValueChange = handleTextChange, textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground), modifier = commonModifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary))
                }
            }
            BlockType.NUMBER -> {
                Row(verticalAlignment = Alignment.Top) {
                    Text("$numberIndex.", modifier = Modifier.padding(end = 8.dp), fontWeight = FontWeight.Bold)
                    BasicTextField(value = textState, onValueChange = handleTextChange, textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground), modifier = commonModifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary))
                }
            }
            BlockType.TODO -> {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = if (block.isChecked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp).padding(end = 8.dp).clickable { onUpdate(block.copy(isChecked = !block.isChecked)) }
                    )
                    BasicTextField(
                        value = textState, onValueChange = handleTextChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (block.isChecked) Color.Gray else MaterialTheme.colorScheme.onBackground, textDecoration = if (block.isChecked) TextDecoration.LineThrough else null),
                        modifier = commonModifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            BlockType.QUOTE -> {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Box(modifier = Modifier.width(4.dp).height(IntrinsicSize.Min).background(MaterialTheme.colorScheme.primary))
                    BasicTextField(
                        value = textState, onValueChange = handleTextChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onBackground),
                        modifier = commonModifier.weight(1f).padding(start = 12.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            BlockType.CALLOUT -> {
                Surface(
                    color = Color(0xFFF2FBF5),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFF4CAF50))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = block.icon,
                            onValueChange = { if (it.length <= 2) onUpdate(block.copy(icon = it)) },
                            textStyle = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(32.dp).padding(end = 8.dp)
                        )
                        BasicTextField(
                            value = textState, onValueChange = handleTextChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF1B5E20)),
                            modifier = commonModifier.weight(1f), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner -> if (textState.text.replace(ZWSP,"").isEmpty()) Text("Type callout...", color = Color.Gray) else inner() }
                        )
                    }
                }
            }
            BlockType.DIVIDER -> {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray)
            }
            BlockType.CALENDAR -> {
                Surface(modifier = Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(contentAlignment = Alignment.Center) { Text("📅 Calendar View (Coming Soon)") }
                }
            }
            BlockType.TABLE -> {
                Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(4.dp)) {
                    block.tableData.forEachIndexed { rIndex, row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEachIndexed { cIndex, cell ->
                                BasicTextField(
                                    value = cell,
                                    onValueChange = { newCell ->
                                        val newData = block.tableData.map { it.toMutableList() }.toMutableList()
                                        newData[rIndex][cIndex] = newCell
                                        onUpdate(block.copy(tableData = newData))
                                    },
                                    modifier = Modifier.weight(1f).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant).padding(8.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground)
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = {
                            val colCount = block.tableData.firstOrNull()?.size ?: 3
                            val newData = block.tableData.toMutableList().apply { add(List(colCount) { "" }) }
                            onUpdate(block.copy(tableData = newData))
                        }) { Text("+ Add Row") }
                        TextButton(onClick = {
                            val newData = block.tableData.map { row -> row.toMutableList().apply { add("") } }
                            onUpdate(block.copy(tableData = newData))
                        }) { Text("+ Add Column") }
                    }
                }
            }
            BlockType.TOGGLE -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = if (block.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).clickable { onUpdate(block.copy(isExpanded = !block.isExpanded)) }
                        )
                        BasicTextField(
                            value = textState, onValueChange = handleTextChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground),
                            modifier = commonModifier.weight(1f).padding(start = 4.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (block.isExpanded) {
                        Column(modifier = Modifier.padding(start = 28.dp, top = 4.dp)) {
                            block.children.forEachIndexed { childIndex, childBlock ->
                                BlockRenderer(
                                    block = childBlock,
                                    numberIndex = childIndex + 1,
                                    focusRequesters = focusRequesters,
                                    focusTargetId = focusTargetId,
                                    onFocusRequest = onFocusRequest,
                                    onUpdate = { updatedChild ->
                                        val newChildren = block.children.toMutableList()
                                        val safeIndex = newChildren.indexOfFirst { it.id == updatedChild.id }
                                        if (safeIndex != -1) {
                                            newChildren[safeIndex] = updatedChild
                                            onUpdate(block.copy(children = newChildren))
                                        }
                                    },
                                    onSplitBlock = { currentContent, nextType, nextContent ->
                                        val newChildren = block.children.toMutableList()
                                        val safeIndex = newChildren.indexOfFirst { it.id == childBlock.id }
                                        if (safeIndex != -1) {
                                            newChildren[safeIndex] = newChildren[safeIndex].copy(content = currentContent)
                                            val newBlockId = UUID.randomUUID().toString()
                                            val newChild = SandboxBlock(id = newBlockId, type = nextType, content = nextContent)
                                            newChildren.add(safeIndex + 1, newChild)
                                            onUpdate(block.copy(children = newChildren))
                                            onFocusRequest(newBlockId)
                                        }
                                    },
                                    onDeleteBlock = { remainder ->
                                        val newChildren = block.children.toMutableList()
                                        val safeIndex = newChildren.indexOfFirst { it.id == childBlock.id }
                                        if (safeIndex > 0) {
                                            val prevChild = newChildren[safeIndex - 1]
                                            newChildren[safeIndex - 1] = prevChild.copy(content = prevChild.content + remainder)
                                            newChildren.removeAt(safeIndex)
                                            onUpdate(block.copy(children = newChildren))
                                            onFocusRequest(prevChild.id)
                                        } else if (safeIndex == 0) {
                                            onUpdate(block.copy(content = block.content + remainder, children = newChildren.drop(1)))
                                            onFocusRequest(block.id)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (block.children.isEmpty()) {
                                TextButton(onClick = { onUpdate(block.copy(children = listOf(SandboxBlock(content = ZWSP)))) }) { Text("+ Add inside toggle", color = Color.Gray) }
                            }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.width(220.dp).heightIn(max = 280.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
        ) {
            val applyType: (BlockType) -> Unit = { type ->
                val cleanText = textState.text.removeSuffix("/")
                textState = textState.copy(text = cleanText, selection = TextRange(cleanText.length))
                val updated = block.copy(type = type, content = cleanText)
                if (type == BlockType.TABLE) updated.tableData = List(3) { List(3) { "" } }
                onUpdate(updated)
                showMenu = false
            }

            val menuItems = listOf(
                BlockType.TEXT to ("Text" to "Aa"),
                BlockType.HEADING to ("Heading" to "H"),
                BlockType.TODO to ("To-do List" to "☑"),
                BlockType.BULLET to ("Bulleted List" to "•"),
                BlockType.NUMBER to ("Numbered List" to "1."),
                BlockType.TOGGLE to ("Toggle List" to "▶"),
                BlockType.CALLOUT to ("Callout" to "💡"),
                BlockType.QUOTE to ("Quote" to "”"),
                BlockType.DIVIDER to ("Divider" to "—"),
                BlockType.TABLE to ("Table" to "⊞"),
                BlockType.CODE to ("Code" to "⌨️"),
                BlockType.IMAGE to ("Image" to "🖼️"),
                BlockType.FILE to ("File" to "📁"),
                BlockType.AUDIO to ("Audio" to "🎵"),
                BlockType.CALENDAR to ("Calendar" to "📅")
            )

            menuItems.forEach { (type, data) ->
                val (label, iconStr) = data
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(iconStr, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(32.dp), color = MaterialTheme.colorScheme.primary)
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    onClick = { applyType(type) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}