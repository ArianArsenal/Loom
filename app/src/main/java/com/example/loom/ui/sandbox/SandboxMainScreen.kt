package com.example.loom.ui.sandbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager // --- FIX: Clean qualifier ---
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import com.example.loom.util.SecurityManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds // --- FIX: Duration mapping ---

enum class SandboxRoute { HUB, CANVAS }

@Composable
fun SandboxMainScreen(
    viewModel: SandboxViewModel,
    onCanvasVisibilityChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var currentRoute by remember { mutableStateOf(SandboxRoute.HUB) }
    var spaceBackstack by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(currentRoute) {
        onCanvasVisibilityChanged(currentRoute == SandboxRoute.CANVAS)
    }

    val allSpaces by viewModel.allSpaces.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    val activeSpace = allSpaces.find { it.id == spaceBackstack.lastOrNull() }

    var spaceToUnlock by remember { mutableStateOf<SandboxSpace?>(null) }
    var showPinFallback by remember { mutableStateOf(false) }
    var showNoSecurityDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    var isNavigating by remember { mutableStateOf(false) }
    var pendingNavigation by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val savedPin = SecurityManager.getPin(context)
    val focusManager = LocalFocusManager.current // --- FIX: Replaced redundant qualifier ---
    val scope = rememberCoroutineScope()

    val attemptNavigateToSpace = { spaceId: String, isNested: Boolean ->
        if (!isNavigating) {
            isNavigating = true
            focusManager.clearFocus()

            val targetSpace = allSpaces.find { it.id == spaceId }

            val executeNavigation = {
                if (isNested) spaceBackstack = spaceBackstack + spaceId
                else {
                    spaceBackstack = listOf(spaceId)
                    currentRoute = SandboxRoute.CANVAS
                }
                isNavigating = false
            }

            if (targetSpace?.isPrivate == true) {
                val hasPin = !savedPin.isNullOrBlank()
                val hasBio = isBiometricEnabled && SecurityManager.hasBiometricHardware(context)

                if (!hasPin && !hasBio) {
                    showNoSecurityDialog = true
                } else if (hasBio) {
                    scope.launch {
                        delay(100.milliseconds) // --- FIX: Modern Duration API ---
                        SecurityManager.authenticate(
                            context = context,
                            title = "Unlock Private Space",
                            onSuccess = { executeNavigation() },
                            onFallback = {
                                if (hasPin) {
                                    spaceToUnlock = targetSpace
                                    pendingNavigation = Pair(spaceId, isNested)
                                    showPinFallback = true
                                } else {
                                    showNoSecurityDialog = true
                                }
                            }
                        )
                    }
                } else {
                    spaceToUnlock = targetSpace
                    pendingNavigation = Pair(spaceId, isNested)
                    showPinFallback = true
                }
            } else {
                executeNavigation()
            }
        }
    }

    if (showNoSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showNoSecurityDialog = false; isNavigating = false },
            title = { Text("Security Not Configured", fontWeight = FontWeight.Bold) },
            text = { Text("This space is private, but you haven't set up an App PIN or Biometric unlock. Please go to Settings to configure your security.") },
            confirmButton = {
                Button(onClick = { showNoSecurityDialog = false; isNavigating = false }) { Text("OK") }
            }
        )
    }

    if (showPinFallback && spaceToUnlock != null) {
        AlertDialog(
            onDismissRequest = {
                spaceToUnlock = null; passwordInput = ""; passwordError = false
                showPinFallback = false; isNavigating = false; pendingNavigation = null
            },
            title = { Text("Private Space") },
            text = {
                Column {
                    if (savedPin.isNullOrBlank()) {
                        Text("No PIN has been configured. Please go to Settings to set up an App PIN.", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Enter your custom PIN to unlock:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passwordInput, onValueChange = { passwordInput = it; passwordError = false },
                            isError = passwordError, visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        if (passwordError) Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                if (!savedPin.isNullOrBlank()) {
                    Button(onClick = {
                        if (passwordInput == savedPin) {
                            pendingNavigation?.let { (id, isNested) ->
                                if (isNested) spaceBackstack = spaceBackstack + id
                                else {
                                    spaceBackstack = listOf(id)
                                    currentRoute = SandboxRoute.CANVAS
                                }
                            }
                            spaceToUnlock = null
                            passwordInput = ""
                            passwordError = false
                            showPinFallback = false
                            isNavigating = false
                            pendingNavigation = null
                        } else passwordError = true
                    }) { Text("Unlock") }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    spaceToUnlock = null; passwordInput = ""; passwordError = false
                    showPinFallback = false; isNavigating = false; pendingNavigation = null
                }) { Text("Cancel") }
            }
        )
    }

    if (currentRoute == SandboxRoute.HUB) {
        SandboxHubScreen(
            viewModel = viewModel,
            allSpaces = allSpaces,
            onNavigateToSpace = { spaceId ->
                attemptNavigateToSpace(spaceId, false)
            }
        )
    } else {
        activeSpace?.let { space ->
            BackHandler {
                if (spaceBackstack.size > 1) {
                    spaceBackstack = spaceBackstack.dropLast(1)
                } else {
                    spaceBackstack = emptyList()
                    currentRoute = SandboxRoute.HUB
                }
            }

            NotionCanvasScreen(
                space = space,
                allSpaces = allSpaces,
                onBack = {
                    if (spaceBackstack.size > 1) {
                        spaceBackstack = spaceBackstack.dropLast(1)
                    } else {
                        spaceBackstack = emptyList()
                        currentRoute = SandboxRoute.HUB
                    }
                },
                onNavigateToSpace = { spaceId -> attemptNavigateToSpace(spaceId, true) },
                onCreateLinkedSpace = {
                    val newSpace = SandboxSpace(title = "", blocks = listOf(SandboxBlock(content = ZWSP)))
                    viewModel.saveSpace(newSpace)
                    newSpace.id
                },
                onSave = { updatedSpace -> viewModel.saveSpace(updatedSpace) }
            )
        } ?: run {
            currentRoute = SandboxRoute.HUB
            spaceBackstack = emptyList()
        }
    }
}