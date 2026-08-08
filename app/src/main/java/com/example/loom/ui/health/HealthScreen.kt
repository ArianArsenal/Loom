package com.example.loom.ui.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HealthScreen(viewModel: HealthViewModel) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Health & Fitness", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item { HealthWorkout(viewModel) }
            item { HealthNutrition(viewModel) }
            item { HealthSkincare(viewModel) }
            item { HealthHydration(viewModel) }
            item { SleepRecoveryCard(viewModel) }
            item { HealthBio(viewModel) }
            item { BodyMeasurementsCard(viewModel) }
            item { FastingTimerCard(viewModel) }

        }
    }
}