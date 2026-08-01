package com.example.loom.ui.sandbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.loom.data.models.SandboxBlock
import com.example.loom.data.models.SandboxSpace

enum class SandboxRoute { HUB, CANVAS }

@Composable
fun SandboxMainScreen(viewModel: SandboxViewModel) {
    var currentRoute by remember { mutableStateOf(SandboxRoute.HUB) }
    var spaceBackstack by remember { mutableStateOf(emptyList<String>()) }
    val allSpaces by viewModel.allSpaces.collectAsState()

    val activeSpace = allSpaces.find { it.id == spaceBackstack.lastOrNull() }

    if (currentRoute == SandboxRoute.HUB) {
        SandboxHubScreen(
            viewModel = viewModel,
            allSpaces = allSpaces,
            onNavigateToSpace = { spaceId ->
                spaceBackstack = listOf(spaceId)
                currentRoute = SandboxRoute.CANVAS
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
                    spaceBackstack = spaceBackstack + spaceId
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
            // Fallback in case the active space was somehow deleted or not found
            currentRoute = SandboxRoute.HUB
            spaceBackstack = emptyList()
        }
    }
}