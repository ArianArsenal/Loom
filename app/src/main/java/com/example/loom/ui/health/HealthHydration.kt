package com.example.loom.ui.health

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun HealthHydration(viewModel: HealthViewModel) {
    val todayRecord by viewModel.todayRecord.collectAsState()
    val defaultWaterIncrement by viewModel.defaultWaterIncrement.collectAsState()

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Hydration Quick-Add", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                val waterLiters = (todayRecord?.waterIntakeMl ?: 0) / 1000f
                val formattedLiters = String.format(Locale.US, "%.3f", waterLiters).trimEnd('0').trimEnd('.')
                Text("$formattedLiters L today", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val options = listOf(250 to "Glass", 600 to "Shaker", 1500 to "Bottle")
                options.forEach { (ml, label) ->
                    val isSelected = defaultWaterIncrement == ml
                    Surface(
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp).clickable { viewModel.saveDefaultWaterIncrement(ml) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${ml}ml", fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha=0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}