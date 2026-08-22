package com.example.loom.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val tags = listOf("UI / UX", "Logic / Functionality", "Performance", "Crash", "Feature Request", "Other")
    var expanded by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf(tags[0]) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report a Bug") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (title.isBlank() || description.isBlank()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    } else {
                        sendBugReportEmail(context, selectedTag, title, description)
                        onNavigateBack()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Report")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Help us improve Loom! Provide a brief title and a detailed description of the issue you encountered.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedTag,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    tags.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag) },
                            onClick = {
                                selectedTag = tag
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Issue Title") },
                placeholder = { Text("e.g., App crashes when saving a new task") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description & Steps to Reproduce") },
                placeholder = { Text("1. Go to Home Screen\n2. Click 'Add Task'\n3. Observe error...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

// Intent Logic to Launch Gmail / Email Client
private fun sendBugReportEmail(context: Context, tag: String, title: String, description: String) {
    val deviceModel = Build.MODEL
    val androidVersion = Build.VERSION.RELEASE
    val sdkInt = Build.VERSION.SDK_INT

    // Automatically fetch the real app version from your build.gradle
    val appVersion = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    } catch (_: Exception) {
        "1.0.0"
    }

    // Using trimMargin() with the '|' character ensures a perfectly flush left-alignment
    val emailBody = """
        |**Bug Description:**
        |$description
        |
        |-----------------------
        |**Diagnostic Info (Do not edit):**
        |Tag: $tag
        |Device: $deviceModel
        |Android Version: $androidVersion (API $sdkInt)
        |App Version: $appVersion
    """.trimMargin()

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf("arianmohseni1382@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, "Loom Bug Report: [$tag] $title")
        putExtra(Intent.EXTRA_TEXT, emailBody)
    }

    try {
        context.startActivity(Intent.createChooser(intent, "Send Bug Report via..."))
    } catch (_: Exception) {
        Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
    }
}