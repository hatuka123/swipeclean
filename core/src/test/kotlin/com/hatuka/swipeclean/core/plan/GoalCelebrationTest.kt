package com.hatuka.swipeclean.core.plan

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalCelebrationTest {

    private val today = 20_000L

    @Test
    fun `shows when the goal is reached while swiping`() {
        assertTrue(GoalCelebration.shouldShow(wasMet = false, isMet = true, celebratedDay = null, today = today))
        assertTrue(GoalCelebration.shouldShow(wasMet = false, isMet = true, celebratedDay = today - 1, today = today))
    }

    @Test
    fun `not when the screen opens with the goal already met`() {
        assertFalse(GoalCelebration.shouldShow(wasMet = null, isMet = true, celebratedDay = null, today = today))
    }

    @Test
    fun `only once a day, even after undo and reaching it again`() {
        assertFalse(GoalCelebration.shouldShow(wasMet = false, isMet = true, celebratedDay = today, today = today))
    }

    @Test
    fun `not before the goal is met`() {
        assertFalse(GoalCelebration.shouldShow(wasMet = false, isMet = false, celebratedDay = null, today = today))
        assertFalse(GoalCelebration.shouldShow(wasMet = true, isMet = true, celebratedDay = null, today = today))
    }
}
