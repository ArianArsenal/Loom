package com.example.loom.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.local.entity.ConsumedFoodLog
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.ui.home.HomeViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionLogCard(
    todayRecord: DailyRecord?,
    targetCaloriesInt: Int,
    targetProteinInt: Int,
    targetCarbsInt: Int,
    targetFatsInt: Int,
    consumedList: List<ConsumedFoodLog>,
    viewModel: HomeViewModel,
    onLogMealClick: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Nutrition Log", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onLogMealClick, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) { Text("+ Log Meal") }
            }
            Spacer(modifier = Modifier.height(12.dp))

            val currentCals = todayRecord?.calories ?: 0
            val calProg = if (targetCaloriesInt > 0) (currentCals.toFloat() / targetCaloriesInt).coerceIn(0f, 1f) else 0f
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Calories", style = MaterialTheme.typography.bodyMedium)
                Text("$currentCals / $targetCaloriesInt kcal", fontWeight = FontWeight.Bold)
            }

            GaplessProgressBar(
                progress = calProg,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val currentPro = todayRecord?.protein ?: 0
                val proProg = if (targetProteinInt > 0) (currentPro.toFloat() / targetProteinInt).coerceIn(0f, 1f) else 0f
                Column(modifier = Modifier.weight(1f)) {
                    Text("Protein", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${currentPro}g / ${targetProteinInt}g", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    GaplessProgressBar(progress = proProg, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp))
                }
                val currentCarbs = todayRecord?.carbs ?: 0
                val carbProg = if (targetCarbsInt > 0) (currentCarbs.toFloat() / targetCarbsInt).coerceIn(0f, 1f) else 0f
                Column(modifier = Modifier.weight(1f)) {
                    Text("Carbs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${currentCarbs}g / ${targetCarbsInt}g", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    GaplessProgressBar(progress = carbProg, color = Color(0xFFFFA726), modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp))
                }
                val currentFats = todayRecord?.fats ?: 0
                val fatProg = if (targetFatsInt > 0) (currentFats.toFloat() / targetFatsInt).coerceIn(0f, 1f) else 0f
                Column(modifier = Modifier.weight(1f)) {
                    Text("Fats", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${currentFats}g / ${targetFatsInt}g", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    GaplessProgressBar(progress = fatProg, color = Color(0xFFE53935), modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Consumed Today", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

            if (consumedList.isEmpty()) {
                Text("No meals logged yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                consumedList.sortedByDescending { it.timestamp }.forEach { log ->
                    key(log.id) {
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.removeConsumedFoodLog(log)
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else Color.Transparent
                                Box(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp).background(color, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterEnd) {
                                    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onError)
                                }
                            }
                        ) {
                            val timeStr = java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(java.util.Date(log.timestamp))
                            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(log.emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val servingsDisplay = if (log.servings % 1f == 0f) log.servings.toInt().toString() else log.servings.toString()
                                    Text("${log.foodName} ($servingsDisplay)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("${log.calories} kcal • P:${log.protein} C:${log.carbs} F:${log.fats}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}