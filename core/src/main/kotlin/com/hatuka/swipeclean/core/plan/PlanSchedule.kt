package com.hatuka.swipeclean.core.plan

import java.time.ZonedDateTime

/** When the next reminder is due. */
object PlanSchedule {

    /**
     * The earliest slot occurrence strictly after [now], in [now]'s time zone, or null when no
     * slot has any day. Times that don't exist on a day (DST spring-forward gap) are moved
     * forward by the gap, as [ZonedDateTime.of] does.
     */
    fun nextTrigger(now: ZonedDateTime, slots: List<PlanSlot>): ZonedDateTime? {
        var best: ZonedDateTime? = null
        val today = now.toLocalDate()
        for (offset in 0L..7L) {
            val date = today.plusDays(offset)
            for (slot in slots) {
                if (date.dayOfWeek !in slot.days) continue
                val candidate = ZonedDateTime.of(date, slot.time, now.zone)
                if (candidate.isAfter(now) && (best == null || candidate.isBefore(best))) best = candidate
            }
            if (best != null) return best
        }
        return best
    }
}
