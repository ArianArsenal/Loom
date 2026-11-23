package com.example.loom.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.loom.MainActivity
import com.example.loom.data.local.dao.TrackerDao
import com.example.loom.data.local.entity.Task
import com.example.loom.data.local.preferences.UserPreferences
import com.example.loom.data.repository.TrackerRepository
import com.example.loom.util.DateUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun trackerRepository(): TrackerRepository
    fun trackerDao(): TrackerDao
    fun userPreferences(): UserPreferences
}

private fun deps(context: Context) = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)

/** Snapshot of today's numbers shown by the widgets. */
private data class WidgetData(
    val waterMl: Int, val waterGoal: Int, val waterIncrement: Int,
    val calories: Int, val calorieTarget: Int, val protein: Int, val proteinTarget: Int,
    val fastingText: String?, val nextTask: Task?
) {
    companion object {
        suspend fun load(context: Context): WidgetData {
            val d = deps(context)
            val prefs = d.userPreferences()
            val record = d.trackerDao().getDailyRecordSync(DateUtils.getTodayDateId())
            val fastingText = if (prefs.isFastingFlow.first()) {
                val start = prefs.fastingStartTimeFlow.first()
                val goal = prefs.fastingGoalHoursFlow.first()
                val fmt = DateTimeFormatter.ofPattern("HH:mm")
                fun t(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(fmt)
                "Fasting since ${t(start)} · ${goal}h goal ends ${t(start + goal * 3_600_000L)}"
            } else null
            return WidgetData(
                waterMl = record?.waterIntakeMl ?: 0,
                waterGoal = prefs.waterDailyGoalFlow.first(),
                waterIncrement = prefs.defaultWaterIncrementFlow.first(),
                calories = record?.calories ?: 0,
                calorieTarget = prefs.targetCaloriesFlow.first(),
                protein = record?.protein ?: 0,
                proteinTarget = prefs.targetProteinFlow.first(),
                fastingText = fastingText,
                nextTask = d.trackerDao().getNextOpenTask()
            )
        }
    }
}

private fun progress(value: Int, goal: Int) = if (goal > 0) (value.toFloat() / goal).coerceIn(0f, 1f) else 0f

class WaterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.load(context)
        provideContent { GlanceTheme { WaterContent(data) } }
    }
}

@Composable
private fun WaterContent(data: WidgetData) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground).cornerRadius(20.dp).padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Water", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
        Text("%.1f / %.1f L".format(data.waterMl / 1000f, data.waterGoal / 1000f),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(6.dp))
        LinearProgressIndicator(progress = progress(data.waterMl, data.waterGoal), modifier = GlanceModifier.fillMaxWidth().height(6.dp))
        Spacer(GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth().background(GlanceTheme.colors.primary).cornerRadius(14.dp).padding(8.dp)
                .clickable(actionRunCallback<AddWaterAction>()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("+${data.waterIncrement} ml", style = TextStyle(color = GlanceTheme.colors.onPrimary, fontWeight = FontWeight.Bold))
        }
    }
}

/** "+250 ml" on the widget: logs water without opening the app. */
class AddWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val d = deps(context)
        d.trackerRepository().addWater(d.userPreferences().defaultWaterIncrementFlow.first())
        LoomWidgets.refresh(context)
    }
}

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.load(context)
        provideContent { GlanceTheme { TodayContent(data) } }
    }
}

@Composable
private fun TodayContent(data: WidgetData) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground).cornerRadius(20.dp).padding(14.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Text("Today", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
        Spacer(GlanceModifier.height(6.dp))
        MacroLine("Calories", data.calories, data.calorieTarget, "kcal")
        Spacer(GlanceModifier.height(6.dp))
        MacroLine("Protein", data.protein, data.proteinTarget, "g")
        Spacer(GlanceModifier.height(8.dp))
        data.fastingText?.let {
            Text(it, style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp), maxLines = 2)
            Spacer(GlanceModifier.height(6.dp))
        }
        val task = data.nextTask
        Text(
            if (task == null) "No open tasks" else "Next: ${task.title}" + (if (task.targetDate.isNotBlank()) " · ${task.targetDate}" else ""),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
            maxLines = 2
        )
    }
}

@Composable
private fun MacroLine(label: String, value: Int, target: Int, unit: String) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp), modifier = GlanceModifier.width(64.dp))
        Text("$value / $target $unit", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold))
    }
    LinearProgressIndicator(progress = progress(value, target), modifier = GlanceModifier.fillMaxWidth().height(5.dp))
}

class WaterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WaterWidget()
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

object LoomWidgets {
    suspend fun refresh(context: Context) {
        WaterWidget().updateAll(context)
        TodayWidget().updateAll(context)
    }

    /** While the app process is alive, keep widgets in sync with every change to today's data. */
    @OptIn(FlowPreview::class)
    fun keepUpdated(context: Context, scope: CoroutineScope) {
        val d = deps(context)
        val prefs = d.userPreferences()
        scope.launch {
            combine(
                d.trackerRepository().getTodayRecord(),
                d.trackerDao().getAllTasks(),
                prefs.isFastingFlow,
                prefs.waterDailyGoalFlow,
                prefs.targetCaloriesFlow
            ) { _, _, _, _, _ -> Unit }
                .debounce(500)
                .collect { runCatching { refresh(context) } }
        }
    }
}
