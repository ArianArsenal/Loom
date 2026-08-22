package com.example.loom.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loom.data.local.entity.ConsumedFoodLog
import com.example.loom.data.local.entity.DailyRecord
import com.example.loom.ui.home.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun getTimelineDateFormatter() = SimpleDateFormat("h:mm a", Locale.getDefault())

// --- PERFECT PRECISION FIX: Max 3 decimals, strip trailing zeros ---
private fun Float.formatWeight(): String {
    val formatted = String.format(Locale.US, "%.3f", this)
    return formatted.trimEnd('0').trimEnd('.')
}

// --- RTL MASTER FIX ---
@Composable
fun LoggedServingDisplay(servings: Float, servingUnit: String, textStyle: androidx.compose.ui.text.TextStyle, color: Color) {
    val mResult = "\\d+(\\.\\d+)?".toRegex().find(servingUnit)
    val numStr = mResult?.value ?: ""
    val textStr = if (mResult != null) servingUnit.replaceFirst(numStr, "").trim() else servingUnit.trim()
    val isPersian = Regex("[\\u0600-\\u06FF]").containsMatchIn(textStr)

    val finalMultiplierStr = servings.formatWeight()

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPersian) {
            if (textStr.isNotBlank()) Text(textStr, style = textStyle.copy(textDirection = TextDirection.Rtl), color = color)
            if (numStr.isNotBlank()) Text(numStr, style = textStyle, fontWeight = FontWeight.Bold, color = color)
            Text("×", style = textStyle, color = color)
            Text(finalMultiplierStr, style = textStyle, fontWeight = FontWeight.Bold, color = color)
        } else {
            Text(finalMultiplierStr, style = textStyle, fontWeight = FontWeight.Bold, color = color)
            Text("×", style = textStyle, color = color)
            if (numStr.isNotBlank()) Text(numStr, style = textStyle, fontWeight = FontWeight.Bold, color = color)
            if (textStr.isNotBlank()) Text(textStr, style = textStyle.copy(textDirection = TextDirection.Ltr), color = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    var logDetailsToShow by remember { mutableStateOf<ConsumedFoodLog?>(null) }

    if (logDetailsToShow != null) {
        val log = logDetailsToShow!!
        val timeStr = getTimelineDateFormatter().format(Date(log.timestamp))
        AlertDialog(
            onDismissRequest = { logDetailsToShow = null },
            title = { Text("Meal Details") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(log.emoji, fontSize = 40.sp) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val isNamePersian = Regex("[\\u0600-\\u06FF]").containsMatchIn(log.foodName)
                    Text(
                        text = log.foodName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            textDirection = if (isNamePersian) TextDirection.Rtl else TextDirection.Ltr
                        ),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LoggedServingDisplay(log.servings, log.servingUnit.ifBlank { "serving" }, MaterialTheme.typography.bodyMedium, MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("•  $timeStr", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${log.calories}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                            Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${log.protein}g", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), style = MaterialTheme.typography.titleMedium)
                            Text("Pro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${log.carbs}g", fontWeight = FontWeight.Bold, color = Color(0xFFFFA726), style = MaterialTheme.typography.titleMedium)
                            Text("Carbs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${log.fats}g", fontWeight = FontWeight.Bold, color = Color(0xFFE53935), style = MaterialTheme.typography.titleMedium)
                            Text("Fats", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { logDetailsToShow = null }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.removeConsumedFoodLog(log)
                    logDetailsToShow = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete Log")
                }
            }
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Nutrition Log", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onLogMealClick, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("+ Log Meal")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

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
                            val timeStr = getTimelineDateFormatter().format(Date(log.timestamp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .combinedClickable(
                                        onClick = { logDetailsToShow = log },
                                        onLongClick = { logDetailsToShow = log }
                                    )
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(log.emoji, fontSize = 24.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                LoggedServingDisplay(
                                                    servings = log.servings,
                                                    servingUnit = log.servingUnit.ifBlank { "serving" },
                                                    textStyle = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        val isNamePersian = Regex("[\\u0600-\\u06FF]").containsMatchIn(log.foodName)
                                        Text(
                                            text = log.foodName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDirection = if (isNamePersian) TextDirection.Rtl else TextDirection.Ltr
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
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