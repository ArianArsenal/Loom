package com.example.loom.ui.sandbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandboxHubScreen(
    viewModel: SandboxViewModel,
    allSpaces: List<SandboxSpace>,
    onNavigateToSpace: (String) -> Unit
) {
    var editingTagsForSpace by remember { mutableStateOf<SandboxSpace?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Spaces", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = (-0.5).sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "New", tint = MaterialTheme.colorScheme.onPrimary) }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // GLOBAL SEARCH
            var searchQuery by remember { mutableStateOf("") }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search spaces & contents...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
            )

            // TAG FILTERS
            val allTags = remember(allSpaces) { allSpaces.flatMap { it.tags }.distinct().sorted() }
            var selectedTag by remember { mutableStateOf<String?>(null) }

            if (allTags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTag == null,
                            onClick = { selectedTag = null },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                    items(allTags) { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = if (selectedTag == tag) null else tag },
                            label = { Text(tag) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                }
            }

            // FILTERED & PINNED SORTING
            val filteredSpaces = remember(allSpaces, searchQuery, selectedTag) {
                allSpaces.filter { space ->
                    val matchesSearch = space.title.contains(searchQuery, ignoreCase = true) ||
                            space.blocks.any { it.content.contains(searchQuery, ignoreCase = true) }
                    val matchesTag = selectedTag == null || space.tags.contains(selectedTag)
                    matchesSearch && matchesTag
                }.sortedWith(compareByDescending<SandboxSpace> { it.isPinned }.thenByDescending { it.updatedAt })
            }

            if (filteredSpaces.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isNotEmpty()) "No matching spaces found." else "No spaces yet. Create your first workspace!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    items(filteredSpaces, key = { it.id }) { space ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteSpace(space)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else Color.Transparent
                                Box(modifier = Modifier.fillMaxSize().background(color, RoundedCornerShape(20.dp)).padding(end = 24.dp), contentAlignment = Alignment.CenterEnd) {
                                    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onError)
                                    }
                                }
                            }
                        ) {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    // DELEGATES SECURE NAVIGATION TO THE MAIN SCREEN
                                    val bumpedSpace = space.copy(updatedAt = System.currentTimeMillis())
                                    viewModel.saveSpace(bumpedSpace)
                                    onNavigateToSpace(bumpedSpace.id)
                                },
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) { Text(space.icon.ifEmpty { "📄" }, fontSize = 28.sp) }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (space.isPinned) {
                                                Icon(Icons.Default.PushPin, "Pinned", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            if (space.isPrivate) {
                                                Icon(Icons.Default.Lock, "Private", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(space.title.ifBlank { "Untitled Space" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        if (space.tags.isNotEmpty()) {
                                            Spacer(Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                                space.tags.forEach { tag ->
                                                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                                                        Text(tag, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    var showSpaceMenu by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = { showSpaceMenu = true }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray)
                                        }
                                        DropdownMenu(expanded = showSpaceMenu, onDismissRequest = { showSpaceMenu = false }) {
                                            DropdownMenuItem(
                                                text = { Text(if (space.isPinned) "Unpin Space" else "Pin Space") },
                                                onClick = {
                                                    viewModel.saveSpace(space.copy(isPinned = !space.isPinned, updatedAt = System.currentTimeMillis()))
                                                    showSpaceMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Edit Tags") },
                                                onClick = {
                                                    editingTagsForSpace = space
                                                    showSpaceMenu = false
                                                }
                                            )
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    viewModel.deleteSpace(space)
                                                    showSpaceMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        if (editingTagsForSpace != null) {
            EditSpaceTagsDialog(
                space = editingTagsForSpace!!,
                onDismiss = { editingTagsForSpace = null },
                onSave = { updatedTags ->
                    viewModel.saveSpace(editingTagsForSpace!!.copy(tags = updatedTags, updatedAt = System.currentTimeMillis()))
                    editingTagsForSpace = null
                }
            )
        }

        if (showCreateDialog) {
            val appTemplates = com.example.loom.utils.TemplateManager.appTemplates
            val userTemplates = remember { com.example.loom.utils.TemplateManager.getCustomTemplates(context) }
            val gson = remember { Gson() }

            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Choose a Template", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item { Text("App Layouts", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)) }
                        items(appTemplates) { template ->
                            ListItem(
                                headlineContent = { Text(template.name, fontWeight = FontWeight.SemiBold) },
                                leadingContent = { Text(template.icon, fontSize = 24.sp) },
                                modifier = Modifier.clickable {
                                    val type = object : TypeToken<List<SandboxBlock>>() {}.type
                                    val initialBlocks: List<SandboxBlock> = try {
                                        gson.fromJson(template.blocksJson, type) ?: listOf(SandboxBlock(content = "\u200B"))
                                    } catch(e: Exception) { listOf(SandboxBlock(content = "\u200B")) }

                                    val newSpace = SandboxSpace(
                                        title = if (template.id == "empty") "" else template.name,
                                        icon = if (template.id == "empty") "📄" else template.icon,
                                        blocks = initialBlocks
                                    )
                                    viewModel.saveSpace(newSpace)
                                    onNavigateToSpace(newSpace.id)
                                    showCreateDialog = false
                                }
                            )
                        }

                        if (userTemplates.isNotEmpty()) {
                            item { Text("Your Layouts", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)) }
                            items(userTemplates) { template ->
                                ListItem(
                                    headlineContent = { Text(template.name, fontWeight = FontWeight.SemiBold) },
                                    leadingContent = { Text(template.icon, fontSize = 24.sp) },
                                    modifier = Modifier.clickable {
                                        val type = object : TypeToken<List<SandboxBlock>>() {}.type
                                        val initialBlocks: List<SandboxBlock> = try {
                                            gson.fromJson(template.blocksJson, type) ?: listOf(SandboxBlock(content = "\u200B"))
                                        } catch(e: Exception) { listOf(SandboxBlock(content = "\u200B")) }

                                        val newSpace = SandboxSpace(
                                            title = template.name,
                                            icon = template.icon,
                                            blocks = initialBlocks
                                        )
                                        viewModel.saveSpace(newSpace)
                                        onNavigateToSpace(newSpace.id)
                                        showCreateDialog = false
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
            )
        }
    }
}