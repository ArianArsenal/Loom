package com.example.loom.ui.sandbox

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import com.example.loom.util.SpaceTemplate
import com.example.loom.util.TemplateManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val hubDateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat {
        return SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    }
}

private val trashDateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat {
        return SimpleDateFormat("MMM d", Locale.getDefault())
    }
}

private val globalGson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DiscouragedApi", "KotlinConstantConditions") // --- FIX: Suppressed Compose State false-positive ---
@Composable
fun SandboxHubScreen(
    viewModel: SandboxViewModel,
    allSpaces: List<SandboxSpace>,
    onNavigateToSpace: (String) -> Unit
) {
    var editingTagsForSpace by remember { mutableStateOf<SandboxSpace?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    var templateToDelete by remember { mutableStateOf<SpaceTemplate?>(null) }

    var showingTrash by remember { mutableStateOf(false) }
    var templateTrashTrigger by remember { mutableIntStateOf(0) }

    val trashedSpaces by viewModel.trashedSpaces.collectAsState(initial = emptyList())

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val trashedAppTemplates = remember(showingTrash, templateTrashTrigger) { TemplateManager.getTrashedAppTemplates(context) }
    val trashedCustomTemplates = remember(showingTrash, templateTrashTrigger) { TemplateManager.getTrashedCustomTemplates(context) }
    val allTrashedTemplates = trashedAppTemplates + trashedCustomTemplates

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (showingTrash) "Trash" else "My Spaces", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = (-0.5).sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    if (showingTrash && (trashedSpaces.isNotEmpty() || allTrashedTemplates.isNotEmpty())) {
                        TextButton(onClick = {
                            viewModel.emptyTrash()
                            TemplateManager.emptyTemplateTrash(context)
                            templateTrashTrigger++
                        }) {
                            Text("Empty", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = { showingTrash = !showingTrash }) {
                        Icon(if (showingTrash) Icons.Default.Close else Icons.Default.DeleteSweep, "Trash")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showingTrash) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.offset(y = 28.dp)
                ) { Icon(Icons.Default.Add, "New", tint = MaterialTheme.colorScheme.onPrimary) }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            if (!showingTrash) {
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
                        items(allTags, key = { it }) { tag ->
                            val parts = tag.split("|")
                            val text = parts[0]
                            val hex = parts.getOrNull(1) ?: "#4CAF50"
                            val tagColor = try { Color(hex.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.primary }

                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { selectedTag = if (selectedTag == tag) null else tag },
                                label = { Text(text) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tagColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

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
                                        viewModel.moveToTrash(space)
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
                                            Icon(Icons.Default.Delete, "Trash", tint = MaterialTheme.colorScheme.onError)
                                        }
                                    }
                                }
                            ) {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth().clickable {
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
                                            val iconParts = space.icon.split("|")
                                            val isCustomIcon = iconParts.size == 2
                                            Box(contentAlignment = Alignment.Center) {
                                                if (isCustomIcon) {
                                                    val iconName = iconParts[0]
                                                    val iconColorHex = iconParts[1]
                                                    val iconTint = try { Color(iconColorHex.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.onSurface }
                                                    val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                                                    if (resId != 0) {
                                                        Icon(painterResource(resId), contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
                                                    } else {
                                                        Text("❓", fontSize = 24.sp)
                                                    }
                                                } else {
                                                    Text(space.icon.ifEmpty { "📄" }, fontSize = 28.sp)
                                                }
                                            }
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

                                            val dateStr = hubDateFormatter.get()?.format(Date(space.updatedAt)) ?: ""
                                            Text("Last edited: $dateStr", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))

                                            if (space.tags.isNotEmpty()) {
                                                Spacer(Modifier.height(4.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                                    space.tags.forEach { tag ->
                                                        val parts = tag.split("|")
                                                        val text = parts[0]
                                                        val hex = parts.getOrNull(1) ?: "#4CAF50"
                                                        val tagColor = try { Color(hex.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.primary }

                                                        Surface(
                                                            color = tagColor.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(4.dp),
                                                            border = BorderStroke(1.dp, tagColor)
                                                        ) {
                                                            Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = tagColor, fontWeight = FontWeight.Bold)
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
                                                    text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                                    onClick = {
                                                        viewModel.moveToTrash(space)
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
            } else {
                if (trashedSpaces.isEmpty() && allTrashedTemplates.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Trash is empty.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        // 1. SHOW TRASHED SPACES
                        items(trashedSpaces, key = { it.id }) { space ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.permanentlyDeleteSpace(space)
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
                                            Icon(Icons.Default.DeleteForever, "Destroy", tint = MaterialTheme.colorScheme.onError)
                                        }
                                    }
                                }
                            ) {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth().clickable { },
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            val iconParts = space.icon.split("|")
                                            val isCustomIcon = iconParts.size == 2
                                            Box(contentAlignment = Alignment.Center) {
                                                if (isCustomIcon) {
                                                    val iconName = iconParts[0]
                                                    val iconColorHex = iconParts[1]
                                                    val iconTint = try { Color(iconColorHex.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.onSurface }
                                                    val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                                                    if (resId != 0) {
                                                        Icon(painterResource(resId), contentDescription = null, tint = iconTint.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                                                    } else {
                                                        Text("❓", fontSize = 24.sp)
                                                    }
                                                } else {
                                                    Text(space.icon.ifEmpty { "📄" }, fontSize = 28.sp)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(space.title.ifBlank { "Untitled Space" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            val dateStr = trashDateFormatter.get()?.format(Date(space.updatedAt)) ?: ""
                                            Text("Deleted Space: $dateStr", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                                        }

                                        var showTrashMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { showTrashMenu = true }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray)
                                            }
                                            DropdownMenu(expanded = showTrashMenu, onDismissRequest = { showTrashMenu = false }) {
                                                DropdownMenuItem(
                                                    text = { Text("Restore Space") },
                                                    leadingIcon = { Icon(Icons.Default.Restore, null) },
                                                    onClick = {
                                                        viewModel.restoreSpace(space)
                                                        showTrashMenu = false
                                                    }
                                                )
                                                HorizontalDivider()
                                                DropdownMenuItem(
                                                    text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                                    onClick = {
                                                        viewModel.permanentlyDeleteSpace(space)
                                                        showTrashMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. SHOW TRASHED TEMPLATES
                        if (allTrashedTemplates.isNotEmpty()) {
                            item {
                                if (trashedSpaces.isNotEmpty()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                                }
                                Text("Trashed Layouts", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            }

                            items(allTrashedTemplates, key = { "trash_tpl_" + it.id }) { template ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        if (it == SwipeToDismissBoxValue.EndToStart) {
                                            if (template.isAppDefault) {
                                                TemplateManager.permanentlyDeleteAppTemplate(context, template.id)
                                            } else {
                                                TemplateManager.permanentlyDeleteCustomTemplate(context, template.name)
                                            }
                                            templateTrashTrigger++
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
                                                Icon(Icons.Default.DeleteForever, "Destroy", tint = MaterialTheme.colorScheme.onError)
                                            }
                                        }
                                    }
                                ) {
                                    ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(56.dp)
                                            ) {
                                                val iconParts = template.icon.split("|")
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (iconParts.size == 2) {
                                                        val iconTint = try { Color(iconParts[1].toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.onSurface }
                                                        val resId = context.resources.getIdentifier(iconParts[0], "drawable", context.packageName)
                                                        if (resId != 0) Icon(painterResource(resId), null, tint = iconTint.copy(alpha=0.5f), modifier = Modifier.size(28.dp))
                                                    } else {
                                                        Text(template.icon.ifEmpty { "📄" }, fontSize = 28.sp)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(template.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("Deleted Layout", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                                            }

                                            var showTrashMenu by remember { mutableStateOf(false) }
                                            Box {
                                                IconButton(onClick = { showTrashMenu = true }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray)
                                                }
                                                DropdownMenu(expanded = showTrashMenu, onDismissRequest = { showTrashMenu = false }) {
                                                    DropdownMenuItem(
                                                        text = { Text("Restore Layout") },
                                                        leadingIcon = { Icon(Icons.Default.Restore, null) },
                                                        onClick = {
                                                            if (template.isAppDefault) {
                                                                TemplateManager.restoreAppTemplate(context, template.id)
                                                            } else {
                                                                TemplateManager.restoreCustomTemplate(context, template.name)
                                                            }
                                                            templateTrashTrigger++
                                                            showTrashMenu = false
                                                        }
                                                    )
                                                    HorizontalDivider()
                                                    DropdownMenuItem(
                                                        text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                                        onClick = {
                                                            if (template.isAppDefault) {
                                                                TemplateManager.permanentlyDeleteAppTemplate(context, template.id)
                                                            } else {
                                                                TemplateManager.permanentlyDeleteCustomTemplate(context, template.name)
                                                            }
                                                            templateTrashTrigger++
                                                            showTrashMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingTagsForSpace != null) {
        val globalTags = allSpaces.flatMap { it.tags }.distinct().sorted()

        EditSpaceTagsDialog(
            space = editingTagsForSpace!!,
            allExistingTags = globalTags,
            onDismiss = { editingTagsForSpace = null },
            onSave = { updatedTags ->
                viewModel.saveSpace(editingTagsForSpace!!.copy(tags = updatedTags, updatedAt = System.currentTimeMillis()))
                editingTagsForSpace = null
            },
            onDeleteGlobalTag = { tagToDelete ->
                allSpaces.forEach { sp ->
                    if (sp.tags.contains(tagToDelete)) {
                        viewModel.saveSpace(sp.copy(tags = sp.tags.filter { it != tagToDelete }))
                    }
                }
            }
        )
    }

    if (showCreateDialog) {
        val appTemplates = remember(showCreateDialog, templateTrashTrigger) { TemplateManager.getActiveAppTemplates(context) }
        val userTemplates = remember(showCreateDialog, templateTrashTrigger) { TemplateManager.getCustomTemplates(context) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Choose a Template", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {

                    if (appTemplates.isNotEmpty()) {
                        item { Text("App Layouts", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)) }

                        items(appTemplates, key = { it.id }) { template ->
                            ListItem(
                                headlineContent = { Text(template.name, fontWeight = FontWeight.SemiBold) },
                                leadingContent = {
                                    val iconParts = template.icon.split("|")
                                    if (iconParts.size == 2) {
                                        val iconName = iconParts[0]
                                        val iconColorHex = iconParts[1]
                                        val iconTint = try { Color(iconColorHex.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.onSurface }
                                        val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                                        if (resId != 0) {
                                            Icon(painterResource(resId), contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                                        } else {
                                            Text("❓", fontSize = 24.sp)
                                        }
                                    } else {
                                        Text(template.icon.ifEmpty { "📄" }, fontSize = 24.sp)
                                    }
                                },
                                trailingContent = {
                                    if (template.id != "empty") {
                                        IconButton(
                                            onClick = {
                                                showCreateDialog = false
                                                templateToDelete = template
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, "Delete Template", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                },
                                modifier = Modifier.clickable {
                                    // --- FIX: R8-Proof Type Token ---
                                    val type = TypeToken.getParameterized(List::class.java, SandboxBlock::class.java).type
                                    val initialBlocks: List<SandboxBlock> = try {
                                        globalGson.fromJson(template.blocksJson, type) ?: listOf(SandboxBlock(content = "\u200B"))
                                    } catch(_: Exception) { listOf(SandboxBlock(content = "\u200B")) }

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
                    }

                    if (userTemplates.isNotEmpty()) {
                        item { Text("Your Layouts", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)) }

                        items(userTemplates, key = { it.id }) { template ->
                            ListItem(
                                headlineContent = { Text(template.name, fontWeight = FontWeight.SemiBold) },
                                leadingContent = {
                                    val iconParts = template.icon.split("|")
                                    if (iconParts.size == 2) {
                                        val iconName = iconParts[0]
                                        val iconColorHex = iconParts[1]
                                        val iconTint = try { Color(iconColorHex.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.onSurface }
                                        val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                                        if (resId != 0) {
                                            Icon(painterResource(resId), contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                                        } else {
                                            Text("❓", fontSize = 24.sp)
                                        }
                                    } else {
                                        Text(template.icon.ifEmpty { "📄" }, fontSize = 24.sp)
                                    }
                                },
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            showCreateDialog = false
                                            templateToDelete = template
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete Template", tint = MaterialTheme.colorScheme.error)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    // --- FIX: R8-Proof Type Token ---
                                    val type = TypeToken.getParameterized(List::class.java, SandboxBlock::class.java).type
                                    val initialBlocks: List<SandboxBlock> = try {
                                        globalGson.fromJson(template.blocksJson, type) ?: listOf(SandboxBlock(content = "\u200B"))
                                    } catch(_: Exception) { listOf(SandboxBlock(content = "\u200B")) }

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

    if (templateToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                templateToDelete = null
                showCreateDialog = true
            },
            title = { Text("Move Layout to Trash?") },
            text = { Text("Move the '${templateToDelete!!.name}' layout to the trash?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        if (templateToDelete!!.isAppDefault) {
                            TemplateManager.deleteAppTemplate(context, templateToDelete!!.id)
                        } else {
                            TemplateManager.deleteCustomTemplate(context, templateToDelete!!.name)
                        }
                        templateTrashTrigger++
                        templateToDelete = null
                        showCreateDialog = true
                    }
                ) { Text("Trash", color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = {
                TextButton(onClick = {
                    templateToDelete = null
                    showCreateDialog = true
                }) { Text("Cancel") }
            }
        )
    }
}