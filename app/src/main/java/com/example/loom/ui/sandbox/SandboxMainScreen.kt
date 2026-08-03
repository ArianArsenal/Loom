package com.example.loom.ui.sandbox

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace
import com.example.loom.utils.SecurityManager // Make sure this import matches your project!

enum class SandboxRoute { HUB, CANVAS }

@Composable
fun SandboxMainScreen(viewModel: SandboxViewModel) {
    val context = LocalContext.current
    var currentRoute by remember { mutableStateOf(SandboxRoute.HUB) }
    var spaceBackstack by remember { mutableStateOf(emptyList<String>()) }
    val allSpaces by viewModel.allSpaces.collectAsState()

    val activeSpace = allSpaces.find { it.id == spaceBackstack.lastOrNull() }

    // Security Dialog States
    var spaceToUnlock by remember { mutableStateOf<SandboxSpace?>(null) }
    var showPinFallback by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    // Reusable Navigation Logic with Security Interception
    val attemptNavigateToSpace = { spaceId: String, isNested: Boolean ->
        val targetSpace = allSpaces.find { it.id == spaceId }

        if (targetSpace?.isPrivate == true) {
            spaceToUnlock = targetSpace
            SecurityManager.authenticate(
                context = context,
                title = "Unlock Private Space",
                onSuccess = {
                    if (isNested) spaceBackstack = spaceBackstack + spaceId
                    else {
                        spaceBackstack = listOf(spaceId)
                        currentRoute = SandboxRoute.CANVAS
                    }
                    spaceToUnlock = null
                    showPinFallback = false
                },
                onFallback = { showPinFallback = true }
            )
        } else {
            if (isNested) spaceBackstack = spaceBackstack + spaceId
            else {
                spaceBackstack = listOf(spaceId)
                currentRoute = SandboxRoute.CANVAS
            }
        }
    }

    // PIN Fallback Dialog
    if (showPinFallback && spaceToUnlock != null) {
        val savedPin = SecurityManager.getPin(context)

        AlertDialog(
            onDismissRequest = { spaceToUnlock = null; passwordInput = ""; passwordError = false; showPinFallback = false },
            title = { Text("Private Space") },
            text = {
                Column {
                    if (savedPin == null) {
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
                if (savedPin != null) {
                    Button(onClick = {
                        if (passwordInput == savedPin) {
                            if (currentRoute == SandboxRoute.HUB) {
                                spaceBackstack = listOf(spaceToUnlock!!.id)
                                currentRoute = SandboxRoute.CANVAS
                            } else {
                                spaceBackstack = spaceBackstack + spaceToUnlock!!.id
                            }
                            spaceToUnlock = null
                            passwordInput = ""
                            passwordError = false
                            showPinFallback = false
                        } else passwordError = true
                    }) { Text("Unlock") }
                }
            },
            dismissButton = { TextButton(onClick = { spaceToUnlock = null; passwordInput = ""; passwordError = false; showPinFallback = false }) { Text("Cancel") } }
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
                onNavigateToSpace = { spaceId ->
                    attemptNavigateToSpace(spaceId, true)
                },
                onCreateLinkedSpace = {
                    val newSpace = SandboxSpace(title = "", blocks = listOf(SandboxBlock(content = ZWSP)))
                    viewModel.saveSpace(newSpace)
                    newSpace.id
                },
                onSave = { updatedSpace ->
                    viewModel.saveSpace(updatedSpace)
                }
            )
        } ?: run {
            currentRoute = SandboxRoute.HUB
            spaceBackstack = emptyList()
        }
    }
}