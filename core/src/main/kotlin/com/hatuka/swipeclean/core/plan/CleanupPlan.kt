package com.hatuka.swipeclean.core.plan

import com.hatuka.swipeclean.core.media.MediaFilter
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale

/** One reminder time on some days of the week. */
data class PlanSlot(val days: Set<DayOfWeek>, val time: LocalTime)

/** Which items a planned session reviews. [bucketId] null = all photos & videos. */
data class PlanSource(val bucketId: Long? = null, val bucketName: String? = null, val filter: MediaFilter = MediaFilter.BOTH)

/** The daily cleanup plan: reminders at [slots], aiming for [quota] reviewed items per planned day. */
data class CleanupPlan(
    val enabled: Boolean = false,
    val slots: List<PlanSlot> = listOf(DEFAULT_SLOT),
    val quota: Int = DEFAULT_QUOTA,
    val source: PlanSource = PlanSource(),
) {
    /** Days on which the quota counts toward the streak. */
    val plannedDays: Set<DayOfWeek> get() = slots.flatMapTo(HashSet()) { it.days }

    /** Rough time estimate for the notification (about 6 seconds per item, at least a minute). */
    val estimatedMinutes: Int get() = ((quota * SECONDS_PER_ITEM) + 59) / 60

    companion object {
        const val DEFAULT_QUOTA = 50
        const val MIN_QUOTA = 10
        const val MAX_QUOTA = 500
        const val SECONDS_PER_ITEM = 6
        val DEFAULT_SLOT = PlanSlot(DayOfWeek.entries.toSet(), LocalTime.of(20, 0))
    }
}

/**
 * Compact text form of the slots for settings storage, e.g. "1,2,3@20:30;6,7@10:00"
 * (ISO day numbers, Monday = 1). Invalid parts are skipped.
 */
object PlanCodec {
    fun encode(slots: List<PlanSlot>): String = slots.joinToString(";") { slot ->
        slot.days.map { it.value }.sorted().joinToString(",") + "@" + String.format(Locale.ROOT, "%02d:%02d", slot.time.hour, slot.time.minute)
    }

    fun decode(text: String?): List<PlanSlot> {
        if (text.isNullOrBlank()) return emptyList()
        return text.split(';').mapNotNull { part ->
            val (daysText, timeText) = part.split('@').takeIf { it.size == 2 } ?: return@mapNotNull null
            val days = daysText.split(',').mapNotNull { it.trim().toIntOrNull()?.takeIf { d -> d in 1..7 }?.let(DayOfWeek::of) }.toSet()
            val time = runCatching { LocalTime.parse(timeText.trim()) }.getOrNull() ?: return@mapNotNull null
            if (days.isEmpty()) null else PlanSlot(days, time)
        }
    }
}
