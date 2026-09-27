package com.hatuka.swipeclean.core.plan

import java.time.DayOfWeek
import java.time.LocalDate

/** Daily totals as stored per day (epoch day). */
data class DayStats(
    val epochDay: Long,
    val reviewed: Int = 0,
    val deleted: Int = 0,
    val bytesFreed: Long = 0,
)

data class Progress(
    val todayReviewed: Int,
    val quota: Int,
    /** Planned days in a row (up to today or yesterday) on which the quota was met. */
    val streak: Int,
    val totalReviewed: Int,
    val totalDeleted: Int,
    val totalBytesFreed: Long,
    /** The last [ProgressCalculator.HISTORY_DAYS] days, oldest first (missing days as zeros). */
    val recent: List<DayStats>,
) {
    val quotaMet: Boolean get() = quota > 0 && todayReviewed >= quota
    val todayFraction: Float get() = if (quota <= 0) 0f else (todayReviewed.toFloat() / quota).coerceIn(0f, 1f)
}

object ProgressCalculator {
    const val HISTORY_DAYS = 14
    private const val MAX_LOOKBACK_DAYS = 3650

    fun progress(stats: List<DayStats>, plannedDays: Set<DayOfWeek>, quota: Int, today: LocalDate): Progress {
        val byDay = stats.associateBy { it.epochDay }
        val todayEpoch = today.toEpochDay()
        return Progress(
            todayReviewed = byDay[todayEpoch]?.reviewed ?: 0,
            quota = quota,
            streak = streak(byDay.mapValues { it.value.reviewed }, plannedDays, quota, today),
            totalReviewed = stats.sumOf { it.reviewed },
            totalDeleted = stats.sumOf { it.deleted },
            totalBytesFreed = stats.sumOf { it.bytesFreed },
            recent = (HISTORY_DAYS - 1 downTo 0).map { back -> byDay[todayEpoch - back] ?: DayStats(todayEpoch - back) },
        )
    }

    /**
     * Counts consecutive planned days on which at least [quota] items were reviewed. Days that
     * are not in the plan neither count nor break the streak. Today only counts once its quota
     * is met; until then the streak runs up to yesterday and is not broken yet.
     */
    fun streak(reviewedByDay: Map<Long, Int>, plannedDays: Set<DayOfWeek>, quota: Int, today: LocalDate): Int {
        if (plannedDays.isEmpty() || quota <= 0) return 0
        fun met(date: LocalDate) = (reviewedByDay[date.toEpochDay()] ?: 0) >= quota
        var day = today
        var count = 0
        if (day.dayOfWeek in plannedDays) {
            if (met(day)) count++
        }
        repeat(MAX_LOOKBACK_DAYS) {
            day = day.minusDays(1)
            if (day.dayOfWeek !in plannedDays) return@repeat
            if (!met(day)) return count
            count++
        }
        return count
    }
}
