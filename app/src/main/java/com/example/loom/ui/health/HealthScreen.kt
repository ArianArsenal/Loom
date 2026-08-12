package com.example.loom.ui.health

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.loom.ui.home.PinnedCardWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(viewModel: HealthViewModel) {
    val hiddenCards by viewModel.hiddenHealthCards.collectAsState()

    val pinnedCards by viewModel.pinnedHealthCards.collectAsState()
    val allSpaces by viewModel.allSpaces.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val availableCards: List<Pair<String, @Composable () -> Unit>> = listOf(
        "Workout Tracker" to { HealthWorkout(viewModel) },
        "Nutrition Engine" to { HealthNutrition(viewModel) },
        "Supplements & Vitamins" to { SupplementsCard(viewModel) },
        "Skincare Routine" to { HealthSkincare(viewModel) },
        "Hydration Engine" to { HealthHydration(viewModel) },
        "Sleep & Recovery" to { SleepRecoveryCard(viewModel) },
        "Bio Engine" to { HealthBio(viewModel) },
        "Body Measurements" to { BodyMeasurementsCard(viewModel) },
        "Fasting Timer" to { FastingTimerCard(viewModel) }
    )

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(scrollState)
            ) {
                Text("Customize Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Select which modules appear on your Health tab.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(24.dp))

                // --- NATIVE CARDS ---
                availableCards.forEach { (cardName, _) ->
                    val isVisible = !hiddenCards.contains(cardName)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleCardVisibility(cardName, !isVisible) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cardName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        IconButton(onClick = { viewModel.toggleCardVisibility(cardName, !isVisible) }) {
                            Icon(
                                imageVector = if (isVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = "Toggle $cardName",
                                tint = if (isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // --- PINNED SPACE CARDS ---
                if (pinnedCards.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    Text("Pinned Space Cards", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    pinnedCards.forEach { (_, card) ->
                        val cardIdKey = "space_card_${card.id}"
                        val isVisible = !hiddenCards.contains(cardIdKey)
                        val cleanTitle = card.content.replace("\u200B", "").trim().ifEmpty { "Untitled" }
                        val displayTitle = "Space Card: $cleanTitle"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleCardVisibility(cardIdKey, !isVisible) }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            IconButton(onClick = { viewModel.toggleCardVisibility(cardIdKey, !isVisible) }) {
                                Icon(
                                    imageVector = if (isVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle $displayTitle",
                                    tint = if (isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Health & Fitness", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { showBottomSheet = true }
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Customize Dashboard", modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // --- RENDER PINNED CARDS WITH TOGGLE VISIBILITY ---
            if (pinnedCards.isNotEmpty()) {
                items(pinnedCards, key = { "pinned_${it.second.id}" }) { (space, card) ->
                    val cardIdKey = "space_card_${card.id}"
                    val isVisible = !hiddenCards.contains(cardIdKey)

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            PinnedCardWidget(
                                space = space,
                                cardBlock = card,
                                allSpaces = allSpaces,
                                tabName = "Health",
                                onUpdateSpace = { viewModel.updateSpaceFromWidget(it) },
                                onNavigateToSpace = { }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            // Empty State Check
            val allNativeHidden = availableCards.all { hiddenCards.contains(it.first) }
            val allPinnedHidden = pinnedCards.isEmpty() || pinnedCards.all { hiddenCards.contains("space_card_${it.second.id}") }

            if (allNativeHidden && allPinnedHidden) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your dashboard is empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the tune icon above to enable your health modules, or pin a custom Space card here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // --- RENDER NATIVE CARDS ---
            availableCards.forEach { (cardName, CardComposable) ->
                item(key = cardName) {
                    val isVisible = !hiddenCards.contains(cardName)

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            CardComposable()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}