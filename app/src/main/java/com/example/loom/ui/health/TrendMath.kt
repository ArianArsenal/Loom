package com.example.loom.ui.health

import java.time.LocalDate

/** One day of a trend series; null = nothing logged that day. */
data class TrendPoint(val date: LocalDate, val value: Float?)

data class TrendSeries(val points: List<TrendPoint>, val target: Float? = null) {
    val logged: List<Float> get() = points.mapNotNull { it.value }
    val average: Float? get() = logged.takeIf { it.isNotEmpty() }?.average()?.toFloat()
    val latest: Float? get() = points.lastOrNull { it.value != null }?.value
    val first: Float? get() = points.firstOrNull { it.value != null }?.value
}

data class TrendData(
    val days: Int = 30,
    val weight: TrendSeries = TrendSeries(emptyList()),
    val sleep: TrendSeries = TrendSeries(emptyList()),
    val calories: TrendSeries = TrendSeries(emptyList()),
    val protein: TrendSeries = TrendSeries(emptyList()),
    val water: TrendSeries = TrendSeries(emptyList())
)

data class Streak(val current: Int = 0, val best: Int = 0)

data class Streaks(
    val workout: Streak = Streak(),
    val water: Streak = Streak(),
    val supplements: Streak = Streak(),
    val skincare: Streak = Streak(),
    val workoutsLast7Days: Int = 0
)

object StreakCalculator {
    /**
     * [status] maps each day to true (done), false (missed) or null (not applicable, e.g. no
     * supplements scheduled; neither counts nor breaks). Today not done yet doesn't break the
     * current streak: it is counted up to yesterday until the day is over.
     */
    fun compute(status: (LocalDate) -> Boolean?, from: LocalDate, today: LocalDate): Streak {
        var best = 0
        var run = 0
        var day = from
        while (!day.isAfter(today)) {
            when (status(day)) {
                true -> { run++; best = maxOf(best, run) }
                false -> if (day != today) run = 0
                null -> Unit
            }
            day = day.plusDays(1)
        }
        return Streak(current = run, best = best)
    }
}

/** One exercise's history: newest session, personal records and best weight per session (oldest first). */
data class ExerciseStats(
    val last: com.example.loom.data.local.entity.Exercise?,
    val prWeight: Float,
    val prVolume: Float,
    val sessions: List<Pair<String, Float>>
) {
    companion object {
        /** Heaviest weight of a logged exercise, using per-set weights when present. */
        fun topWeight(e: com.example.loom.data.local.entity.Exercise): Float =
            (com.example.loom.data.repository.decodeList<Float>(e.weightListJson).maxOrNull() ?: 0f).coerceAtLeast(e.weight)

        fun volume(e: com.example.loom.data.local.entity.Exercise): Float {
            val perSet = com.example.loom.data.repository.decodeList<Float>(e.weightListJson)
            return if (perSet.isNotEmpty()) perSet.sum() * e.reps else e.sets * e.reps * e.weight
        }

        fun from(history: List<com.example.loom.data.local.entity.Exercise>): ExerciseStats = ExerciseStats(
            last = history.firstOrNull(),
            prWeight = history.maxOfOrNull { topWeight(it) } ?: 0f,
            prVolume = history.maxOfOrNull { volume(it) } ?: 0f,
            sessions = history.groupBy { it.dateId }.map { (date, list) -> date to list.maxOf { topWeight(it) } }.sortedBy { it.first }
        )
    }
}
