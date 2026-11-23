package com.example.loom.data.health

import android.content.Context
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import com.example.loom.data.repository.TrackerRepository
import com.example.loom.util.DateUtils
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Two-way sync with Health Connect for the last [SYNC_DAYS] days:
 * - writes Loom's weight, sleep and water (one record per day, keyed by a stable client id so
 *   re-syncing updates instead of duplicating);
 * - reads weight and sleep recorded by other apps/devices for days Loom has nothing logged.
 */
object HealthConnectSync {
    private const val PREFS = "loom_health_connect"
    private const val SYNC_DAYS = 30L

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class)
    )

    fun isAvailable(context: Context): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun isEnabled(context: Context) = prefs(context).getBoolean("enabled", false)
    fun lastSyncMillis(context: Context) = prefs(context).getLong("lastSync", 0L)
    fun setEnabled(context: Context, enabled: Boolean) = prefs(context).edit { putBoolean("enabled", enabled) }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    suspend fun hasAllPermissions(context: Context): Boolean =
        isAvailable(context) && HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions().containsAll(permissions)

    /** Runs a sync if enabled and permitted. Returns a short summary, or null if skipped. */
    suspend fun syncIfEnabled(context: Context, repository: TrackerRepository): String? {
        if (!isEnabled(context) || !hasAllPermissions(context)) return null
        return sync(context, repository)
    }

    suspend fun sync(context: Context, repository: TrackerRepository): String {
        val client = HealthConnectClient.getOrCreate(context)
        val zone = ZoneId.systemDefault()
        val today = DateUtils.getLogicalToday()
        val from = today.minusDays(SYNC_DAYS - 1)
        val records = repository.getRecordsBetween(from, today).first().associateBy { it.dateId }

        // --- Write Loom data ---
        val writes = buildList {
            records.values.forEach { r ->
                val date = LocalDate.parse(r.dateId)
                if (r.weightLogKg > 0f) {
                    val t = date.atTime(8, 0).atZone(zone)
                    add(WeightRecord(time = t.toInstant(), zoneOffset = t.offset, weight = Mass.kilograms(r.weightLogKg.toDouble()),
                        metadata = loomMetadata("weight-${r.dateId}")))
                }
                if (r.sleepHours > 0f) {
                    // Loom stores sleep as hours per day; record it as a night ending that morning
                    val end = date.atTime(7, 0).atZone(zone)
                    val start = end.minusMinutes((r.sleepHours * 60).toLong())
                    add(SleepSessionRecord(startTime = start.toInstant(), startZoneOffset = start.offset, endTime = end.toInstant(), endZoneOffset = end.offset,
                        metadata = loomMetadata("sleep-${r.dateId}")))
                }
                if (r.waterIntakeMl > 0) {
                    val start = date.atStartOfDay(zone)
                    val end = date.atTime(LocalTime.of(23, 59)).atZone(zone)
                    add(HydrationRecord(startTime = start.toInstant(), startZoneOffset = start.offset, endTime = end.toInstant(), endZoneOffset = end.offset,
                        volume = Volume.milliliters(r.waterIntakeMl.toDouble()), metadata = loomMetadata("water-${r.dateId}")))
                }
            }
        }
        if (writes.isNotEmpty()) client.insertRecords(writes)

        // --- Read other sources into days Loom has nothing for ---
        val range = TimeRangeFilter.between(from.atStartOfDay(zone).toInstant(), today.plusDays(1).atStartOfDay(zone).toInstant())
        var imported = 0

        client.readRecords(ReadRecordsRequest(WeightRecord::class, range)).records
            .filter { it.metadata.dataOrigin.packageName != context.packageName }
            .groupBy { it.time.atZone(zone).toLocalDate() }
            .forEach { (date, list) ->
                val latest = list.maxBy { it.time }
                if ((records[date.toString()]?.weightLogKg ?: 0f) <= 0f) {
                    repository.editRecord(date.toString()) { it.copy(weightLogKg = latest.weight.inKilograms.toFloat()) }
                    imported++
                }
            }

        client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, range)).records
            .filter { it.metadata.dataOrigin.packageName != context.packageName }
            .groupBy { it.endTime.atZone(zone).toLocalDate() }
            .forEach { (date, sessions) ->
                val hours = sessions.sumOf { java.time.Duration.between(it.startTime, it.endTime).toMinutes() } / 60f
                if (hours > 0f && (records[date.toString()]?.sleepHours ?: 0f) <= 0f) {
                    repository.editRecord(date.toString()) { it.copy(sleepHours = (hours * 2).toInt() / 2f) } // nearest half hour, like the slider
                    imported++
                }
            }

        prefs(context).edit { putLong("lastSync", System.currentTimeMillis()) }
        return "Synced ${writes.size} entries to Health Connect, imported $imported"
    }

    // Stable client id per day and kind: Health Connect updates the record instead of duplicating it
    private fun loomMetadata(id: String) = Metadata(clientRecordId = "loom-$id", clientRecordVersion = Instant.now().toEpochMilli())
}
