package com.example.loom.reminders

import android.content.Context
import androidx.core.content.edit

/**
 * User-configurable reminders. Stored in plain SharedPreferences (not DataStore) because the
 * alarm receiver reads them synchronously; included in backups via BackupManager.
 */
data class ReminderSettings(
    val waterEnabled: Boolean = false,
    val waterStartHour: Int = 9,
    val waterEndHour: Int = 21,
    val waterIntervalHours: Int = 2,

    val supplementsEnabled: Boolean = false,
    val morningHour: Int = 8,
    val afternoonHour: Int = 14,
    val eveningHour: Int = 19,
    val beforeBedHour: Int = 22,

    val skincareEnabled: Boolean = false,
    val skincareAmHour: Int = 8,
    val skincarePmHour: Int = 22,

    val tasksEnabled: Boolean = false,
    val tasksHour: Int = 9,

    val fastingEnabled: Boolean = false,

    val recapEnabled: Boolean = true,
    val recapHour: Int = 21,
    val recapMinute: Int = 0
) {
    /** Hour for a supplement's "time of use"; null for "Anytime" (no reminder). */
    fun hourForTimeOfUse(timeOfUse: String): Int? = when (timeOfUse) {
        "Morning" -> morningHour
        "Afternoon" -> afternoonHour
        "Evening" -> eveningHour
        "Before Bed" -> beforeBedHour
        else -> null
    }

    companion object {
        const val PREFS_NAME = "loom_reminders"

        fun load(context: Context): ReminderSettings {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val d = ReminderSettings()
            return ReminderSettings(
                waterEnabled = p.getBoolean("waterEnabled", d.waterEnabled),
                waterStartHour = p.getInt("waterStartHour", d.waterStartHour),
                waterEndHour = p.getInt("waterEndHour", d.waterEndHour),
                waterIntervalHours = p.getInt("waterIntervalHours", d.waterIntervalHours).coerceAtLeast(1),
                supplementsEnabled = p.getBoolean("supplementsEnabled", d.supplementsEnabled),
                morningHour = p.getInt("morningHour", d.morningHour),
                afternoonHour = p.getInt("afternoonHour", d.afternoonHour),
                eveningHour = p.getInt("eveningHour", d.eveningHour),
                beforeBedHour = p.getInt("beforeBedHour", d.beforeBedHour),
                skincareEnabled = p.getBoolean("skincareEnabled", d.skincareEnabled),
                skincareAmHour = p.getInt("skincareAmHour", d.skincareAmHour),
                skincarePmHour = p.getInt("skincarePmHour", d.skincarePmHour),
                tasksEnabled = p.getBoolean("tasksEnabled", d.tasksEnabled),
                tasksHour = p.getInt("tasksHour", d.tasksHour),
                fastingEnabled = p.getBoolean("fastingEnabled", d.fastingEnabled),
                recapEnabled = p.getBoolean("recapEnabled", d.recapEnabled),
                recapHour = p.getInt("recapHour", d.recapHour),
                recapMinute = p.getInt("recapMinute", d.recapMinute)
            )
        }

        fun save(context: Context, s: ReminderSettings) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putBoolean("waterEnabled", s.waterEnabled)
                putInt("waterStartHour", s.waterStartHour)
                putInt("waterEndHour", s.waterEndHour)
                putInt("waterIntervalHours", s.waterIntervalHours)
                putBoolean("supplementsEnabled", s.supplementsEnabled)
                putInt("morningHour", s.morningHour)
                putInt("afternoonHour", s.afternoonHour)
                putInt("eveningHour", s.eveningHour)
                putInt("beforeBedHour", s.beforeBedHour)
                putBoolean("skincareEnabled", s.skincareEnabled)
                putInt("skincareAmHour", s.skincareAmHour)
                putInt("skincarePmHour", s.skincarePmHour)
                putBoolean("tasksEnabled", s.tasksEnabled)
                putInt("tasksHour", s.tasksHour)
                putBoolean("fastingEnabled", s.fastingEnabled)
                putBoolean("recapEnabled", s.recapEnabled)
                putInt("recapHour", s.recapHour)
                putInt("recapMinute", s.recapMinute)
            }
        }
    }
}
