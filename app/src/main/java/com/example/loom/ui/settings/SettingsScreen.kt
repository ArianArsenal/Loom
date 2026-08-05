package com.example.loom.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
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
import com.example.loom.utils.SecurityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val appTheme by viewModel.appTheme.collectAsState()
    // Connected permanently to Datastore!
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    var currentPin by remember { mutableStateOf(SecurityManager.getPin(context)) }

    // Dialog States
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showAuthPinDialog by remember { mutableStateOf(false) }

    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Dialog: Set NEW Pin
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
            dismissButton = { TextButton(onClick = { showSetPinDialog = false; pinInput = "" }) { Text("Cancel") } }
        )
    }

    // Dialog: Authenticate Existing PIN
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
                        showSetPinDialog = true
                    } else {
                        pinError = true
                    }
                }) { Text("Verify") }
            },
            dismissButton = { TextButton(onClick = { showAuthPinDialog = false; pinInput = ""; pinError = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- SECURITY SECTION ---
            Text("Security", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column {
                    // App PIN
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = "PIN", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (currentPin == null) "Set App PIN" else "Change App PIN", fontWeight = FontWeight.Bold)
                            Text("Fallback security for private spaces", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                if (currentPin == null) {
                                    showSetPinDialog = true
                                } else {
                                    // CHECK THE TOGGLE HERE
                                    if (isBiometricEnabled) {
                                        SecurityManager.authenticate(
                                            context = context,
                                            title = "Verify Identity",
                                            subtitle = "Authenticate to change your PIN",
                                            onSuccess = { showSetPinDialog = true },
                                            onFallback = { showAuthPinDialog = true }
                                        )
                                    } else {
                                        // Skip biometrics and go straight to PIN entry
                                        showAuthPinDialog = true
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text(if (currentPin == null) "Setup" else "Change")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))

                    // Biometrics
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = "Security", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric Unlock", fontWeight = FontWeight.Bold)
                            Text("Use fingerprint or face ID to open locked items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) } // Properly routed to ViewModel
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
        }
    }
}