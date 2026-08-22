package com.example.loom.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.loom.ui.home.HomeViewModel
import com.example.loom.util.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBugReport: () -> Unit // --- NEW CALLBACK ---
) {
    val context = LocalContext.current

    val userName by viewModel.userName.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var currentPin by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pin = SecurityManager.getPin(context)
            withContext(Dispatchers.Main) {
                currentPin = pin
            }
        }
    }

    // Dialog States
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showAuthPinDialog by remember { mutableStateOf(false) }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var pendingDisableBiometric by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                viewModel.exportDatabase(context, it,
                    onSuccess = { Toast.makeText(context, "Export Successful!", Toast.LENGTH_SHORT).show() },
                    onError = { err -> Toast.makeText(context, "Export Failed: $err", Toast.LENGTH_LONG).show() }
                )
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                viewModel.importDatabase(context, it,
                    onSuccess = { Toast.makeText(context, "Import Successful!", Toast.LENGTH_SHORT).show() },
                    onError = { err -> Toast.makeText(context, "Import Failed: $err", Toast.LENGTH_LONG).show() }
                )
            }
        }
    )

    // --- NEW: Edit Name Dialog ---
    if (showEditNameDialog) {
        var nameInput by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Profile") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (nameInput.isNotBlank()) {
                        viewModel.saveUserName(nameInput.trim())
                    }
                    showEditNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (isLoading) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {},
            title = { Text("Processing Data") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(24.dp))
                    Text("Please wait...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        )
    }

    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Erase All Data?") },
            text = { Text("This will permanently delete all your tasks, logs, spaces, and routines. This action cannot be undone. Are you absolutely sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.wipeAllData(
                            onSuccess = {
                                showWipeConfirmDialog = false
                                Toast.makeText(context, "All data wiped successfully", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                showWipeConfirmDialog = false
                                Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Erase Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSetPinDialog) {
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false; pinInput = ""; pinError = false },
            title = { Text(if (currentPin == null) "Set App PIN" else "Set New PIN") },
            text = {
                Column {
                    Text("Enter a 4-digit PIN to secure your private notes and spaces.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("----", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            SecurityManager.savePin(context, pinInput)
                            currentPin = pinInput
                            showSetPinDialog = false
                            pinInput = ""
                        }
                    },
                    enabled = pinInput.length == 4
                ) { Text("Save PIN") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    if (!currentPin.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                SecurityManager.savePin(context, "")
                                currentPin = ""
                                showSetPinDialog = false
                                pinInput = ""
                                Toast.makeText(context, "PIN Removed", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Remove PIN") }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = { showSetPinDialog = false; pinInput = "" }) { Text("Cancel") }
                }
            }
        )
    }

    if (showAuthPinDialog) {
        AlertDialog(
            onDismissRequest = { showAuthPinDialog = false; pinInput = ""; pinError = false },
            title = { Text("Verify Identity") },
            text = {
                Column {
                    Text("Enter your current PIN to make changes.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it; pinError = false },
                        isError = pinError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Text("Incorrect PIN. Please try again.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pinInput == currentPin) {
                        showAuthPinDialog = false
                        pinInput = ""
                        pinError = false
                        if (pendingDisableBiometric) {
                            viewModel.setBiometricEnabled(false)
                            pendingDisableBiometric = false
                        } else {
                            showSetPinDialog = true
                        }
                    } else {
                        pinError = true
                    }
                }) { Text("Verify") }
            },
            dismissButton = {
                TextButton(onClick = { showAuthPinDialog = false; pinInput = ""; pinError = false; pendingDisableBiometric = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- NEW: PROFILE SECTION ---
            Text("Profile", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable { showEditNameDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(userName.ifBlank { "User" }, fontWeight = FontWeight.Bold)
                        Text("Tap to edit your name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECURITY SECTION ---
            Text("Security", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = "PIN", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (currentPin.isNullOrBlank()) "Set App PIN" else "Change App PIN", fontWeight = FontWeight.Bold)
                            Text("Fallback security for private spaces", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                if (currentPin.isNullOrBlank()) {
                                    showSetPinDialog = true
                                } else {
                                    if (isBiometricEnabled) {
                                        SecurityManager.authenticate(
                                            context = context,
                                            title = "Verify Identity",
                                            subtitle = "Authenticate to change your PIN",
                                            onSuccess = { showSetPinDialog = true },
                                            onFallback = { showAuthPinDialog = true }
                                        )
                                    } else {
                                        showAuthPinDialog = true
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text(if (currentPin.isNullOrBlank()) "Setup" else "Manage")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))

                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = "Security", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric Unlock", fontWeight = FontWeight.Bold)
                            Text("Use fingerprint or face ID to open locked items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { isEnabling ->
                                if (isEnabling) {
                                    viewModel.setBiometricEnabled(true)
                                } else {
                                    if (currentPin.isNullOrBlank()) {
                                        viewModel.setBiometricEnabled(false)
                                    } else {
                                        SecurityManager.authenticate(
                                            context = context,
                                            title = "Verify Identity",
                                            subtitle = "Authenticate to disable Security",
                                            onSuccess = { viewModel.setBiometricEnabled(false) },
                                            onFallback = {
                                                pendingDisableBiometric = true
                                                showAuthPinDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- GENERAL SECTION ---
            Text("General", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            var themeDropdownExpanded by remember { mutableStateOf(false) }

            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { themeDropdownExpanded = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ColorLens, contentDescription = "Theme", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App Theme", fontWeight = FontWeight.Bold)
                            Text(appTheme, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    DropdownMenu(
                        expanded = themeDropdownExpanded,
                        onDismissRequest = { themeDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        listOf("System", "Light", "Dark").forEach { themeOption ->
                            DropdownMenuItem(
                                text = { Text(themeOption) },
                                onClick = {
                                    viewModel.setAppTheme(themeOption)
                                    themeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- DATA MANAGEMENT SECTION ---
            Text("Data Backup & Restore", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { exportLauncher.launch("Loom_Backup.json") },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export Data", fontWeight = FontWeight.Bold)
                            Text("Save all your logs, spaces, and routines to a file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))

                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { importLauncher.launch(arrayOf("application/json")) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Import", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Import Data", fontWeight = FontWeight.Bold)
                            Text("Restore everything from a previous Loom backup", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- NEW: SUPPORT & FEEDBACK SECTION ---
            Text("Support & Feedback", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { onNavigateToBugReport() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = "Bug Report", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Report a Bug", fontWeight = FontWeight.Bold)
                        Text("Found an issue? Let us know so we can fix it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- DANGER ZONE ---
            Text("Danger Zone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { showWipeConfirmDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete All", tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Erase All Data", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Permanently wipe database and preferences", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}