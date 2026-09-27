package com.hatuka.swipeclean.core.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

class ProgressCalculatorTest {

    // 2026-09-27 is a Sunday.
    private val today = LocalDate.of(2026, 9, 27)
    private val everyDay = DayOfWeek.entries.toSet()

    private fun reviewed(vararg daysBackToCount: Pair<Int, Int>) =
        daysBackToCount.associate { (back, n) -> today.minusDays(back.toLong()).toEpochDay() to n }

    @Test
    fun `consecutive days meeting the quota`() {
        val data = reviewed(0 to 50, 1 to 60, 2 to 50, 3 to 49, 4 to 80)
        assertEquals(3, ProgressCalculator.streak(data, everyDay, 50, today))
    }

    @Test
    fun `today not done yet does not break the streak`() {
        val data = reviewed(0 to 10, 1 to 50, 2 to 50)
        assertEquals(2, ProgressCalculator.streak(data, everyDay, 50, today))
        assertEquals(0, ProgressCalculator.streak(reviewed(0 to 10, 2 to 50), everyDay, 50, today))
    }

    @Test
    fun `days outside the plan are skipped`() {
        // Plan: Monday, Wednesday, Saturday. Sat (1 back) met, Wed (4 back) met, Mon (6 back) missed.
        val plan = setOf(MONDAY, WEDNESDAY, SATURDAY)
        val data = reviewed(1 to 50, 4 to 70, 6 to 0)
        assertEquals(2, ProgressCalculator.streak(data, plan, 50, today))
    }

    @Test
    fun `no plan or no quota means no streak`() {
        assertEquals(0, ProgressCalculator.streak(reviewed(0 to 100), emptySet(), 50, today))
        assertEquals(0, ProgressCalculator.streak(reviewed(0 to 100), everyDay, 0, today))
    }

    @Test
    fun `progress totals and recent days`() {
        val stats = listOf(
            DayStats(today.toEpochDay(), reviewed = 20, deleted = 3, bytesFreed = 300),
            DayStats(today.minusDays(1).toEpochDay(), reviewed = 50, deleted = 10, bytesFreed = 1000),
            DayStats(today.minusDays(40).toEpochDay(), reviewed = 5, deleted = 1, bytesFreed = 50),
        )
        val p = ProgressCalculator.progress(stats, everyDay, 50, today)
        assertEquals(20, p.todayReviewed)
        assertFalse(p.quotaMet)
        assertEquals(0.4f, p.todayFraction, 0.001f)
        assertEquals(1, p.streak)
        assertEquals(75, p.totalReviewed)
        assertEquals(14, p.totalDeleted)
        assertEquals(1350L, p.totalBytesFreed)
        assertEquals(ProgressCalculator.HISTORY_DAYS, p.recent.size)
        assertEquals(today.toEpochDay(), p.recent.last().epochDay)
        assertEquals(50, p.recent[p.recent.size - 2].reviewed)
        assertEquals(0, p.recent.first().reviewed)
        assertTrue(ProgressCalculator.progress(stats, everyDay, 20, today).quotaMet)
    }
}
