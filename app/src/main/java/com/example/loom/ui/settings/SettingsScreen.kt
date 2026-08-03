package com.example.loom.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.unit.dp
import com.example.loom.ui.home.HomeViewModel
import com.example.loom.utils.SecurityManager // Ensure this import matches where you put the file!

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isBiometricEnabled by remember { mutableStateOf(true) }
    var currentPin by remember { mutableStateOf(SecurityManager.getPin(context)) }

    // Dialog States
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showAuthPinDialog by remember { mutableStateOf(false) }

    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Dialog for Setting a NEW Pin
    if (showSetPinDialog) {
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false; pinInput = ""; pinError = false },
            title = { Text(if (currentPin == null) "Set App PIN" else "Set New PIN") },
            text = {
                Column {
                    Text("Enter a 4-digit PIN to secure your private notes and spaces.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pinInput.length == 4) {
                        SecurityManager.savePin(context, pinInput)
                        currentPin = pinInput
                        showSetPinDialog = false
                        pinInput = ""
                    }
                }) { Text("Save PIN") }
            },
            dismissButton = { TextButton(onClick = { showSetPinDialog = false; pinInput = "" }) { Text("Cancel") } }
        )
    }

    // Dialog for Authenticating before changing an existing PIN
    if (showAuthPinDialog) {
        AlertDialog(
            onDismissRequest = { showAuthPinDialog = false; pinInput = ""; pinError = false },
            title = { Text("Verify Identity") },
            text = {
                Column {
                    Text("Enter your current PIN to make changes.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it; pinError = false },
                        isError = pinError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pinInput == currentPin) {
                        showAuthPinDialog = false
                        pinInput = ""
                        pinError = false
                        showSetPinDialog = true // Move to the Set PIN dialog!
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
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(20.dp)
        ) {
            Text("App Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))

            // App PIN Card
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = "PIN", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (currentPin == null) "Set App PIN" else "Change App PIN", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Fallback security if biometrics fail.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Button(onClick = {
                        if (currentPin == null) {
                            showSetPinDialog = true
                        } else {
                            // Require Biometrics or Old PIN to change the PIN
                            SecurityManager.authenticate(
                                context = context,
                                title = "Verify Identity",
                                subtitle = "Authenticate to change your PIN",
                                onSuccess = { showSetPinDialog = true },
                                onFallback = { showAuthPinDialog = true }
                            )
                        }
                    }) {
                        Text(if (currentPin == null) "Setup" else "Change")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Biometric Toggle Card
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Security", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Biometric Unlock", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Use fingerprint or face ID to open locked items.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Switch(checked = isBiometricEnabled, onCheckedChange = { isBiometricEnabled = it })
                }
            }
        }
    }
}