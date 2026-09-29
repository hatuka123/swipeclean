package com.hatuka.swipeclean.core.plan

/**
 * The "daily goal reached" message: once a day, at the moment the goal is reached while swiping,
 * not when the swipe screen opens with the goal already met.
 *
 * [wasMet] is the previous known state in this session (null before the first value, or while the
 * plan is off); [celebratedDay] is the epoch day the message was last shown.
 */
object GoalCelebration {
    fun shouldShow(wasMet: Boolean?, isMet: Boolean, celebratedDay: Long?, today: Long): Boolean =
        wasMet == false && isMet && celebratedDay != today
}
