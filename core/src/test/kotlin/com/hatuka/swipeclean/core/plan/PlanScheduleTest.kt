package com.hatuka.swipeclean.core.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PlanScheduleTest {

    private val jerusalem = ZoneId.of("Asia/Jerusalem")
    private val everyDay = DayOfWeek.entries.toSet()

    private fun at(text: String, zone: ZoneId = jerusalem) = ZonedDateTime.of(java.time.LocalDateTime.parse(text), zone)
    private fun slot(days: Set<DayOfWeek>, hh: Int, mm: Int = 0) = PlanSlot(days, LocalTime.of(hh, mm))

    @Test
    fun `later today or tomorrow`() {
        val slots = listOf(slot(everyDay, 20))
        assertEquals(at("2026-09-27T20:00"), PlanSchedule.nextTrigger(at("2026-09-27T10:15"), slots))
        assertEquals(at("2026-09-28T20:00"), PlanSchedule.nextTrigger(at("2026-09-27T20:00"), slots))
        assertEquals(at("2026-09-28T20:00"), PlanSchedule.nextTrigger(at("2026-09-27T21:00"), slots))
    }

    @Test
    fun `only on planned days`() {
        // 2026-09-27 is a Sunday.
        val slots = listOf(slot(setOf(MONDAY, FRIDAY), 9, 30))
        assertEquals(at("2026-09-28T09:30"), PlanSchedule.nextTrigger(at("2026-09-27T12:00"), slots))
        assertEquals(at("2026-10-02T09:30"), PlanSchedule.nextTrigger(at("2026-09-28T09:31"), slots))
        // A full week ahead when the only day is today but the time has passed.
        assertEquals(at("2026-10-04T08:00"), PlanSchedule.nextTrigger(at("2026-09-27T09:00"), listOf(slot(setOf(SUNDAY), 8))))
    }

    @Test
    fun `earliest of several slots`() {
        val slots = listOf(slot(everyDay, 21), slot(setOf(SUNDAY, SATURDAY), 8), slot(everyDay, 13))
        assertEquals(at("2026-09-27T13:00"), PlanSchedule.nextTrigger(at("2026-09-27T09:00"), slots))
        assertEquals(at("2026-09-27T21:00"), PlanSchedule.nextTrigger(at("2026-09-27T13:00"), slots))
        assertEquals(at("2026-09-28T13:00"), PlanSchedule.nextTrigger(at("2026-09-27T22:00"), slots))
    }

    @Test
    fun `no days means no reminder`() {
        assertNull(PlanSchedule.nextTrigger(at("2026-09-27T10:00"), listOf(slot(emptySet(), 20))))
        assertNull(PlanSchedule.nextTrigger(at("2026-09-27T10:00"), emptyList()))
    }

    @Test
    fun `daylight saving gap moves the time forward`() {
        // Europe/Berlin springs forward on 2026-03-29 at 02:00 -> 03:00.
        val berlin = ZoneId.of("Europe/Berlin")
        val next = PlanSchedule.nextTrigger(at("2026-03-29T00:30", berlin), listOf(slot(everyDay, 2, 30)))!!
        assertEquals(at("2026-03-29T03:30", berlin).toInstant(), next.toInstant())
        // After the change the wall-clock time stays 20:00.
        val evening = PlanSchedule.nextTrigger(at("2026-03-28T21:00", berlin), listOf(slot(everyDay, 20)))!!
        assertEquals(LocalTime.of(20, 0), evening.toLocalTime())
    }

    @Test
    fun `follows the device time zone`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val next = PlanSchedule.nextTrigger(at("2026-09-27T19:00", tokyo), listOf(slot(everyDay, 20)))!!
        assertEquals(tokyo, next.zone)
        assertEquals(at("2026-09-27T20:00", tokyo), next)
    }

    @Test
    fun `codec round trip and bad input`() {
        val slots = listOf(slot(setOf(MONDAY, FRIDAY), 20, 30), slot(setOf(SUNDAY), 7, 5))
        assertEquals("1,5@20:30;7@07:05", PlanCodec.encode(slots))
        assertEquals(slots, PlanCodec.decode(PlanCodec.encode(slots)))
        assertEquals(listOf(slot(setOf(MONDAY), 8)), PlanCodec.decode("1@08:00;9@10:00;x;2@99:99;@10:00"))
        assertEquals(emptyList<PlanSlot>(), PlanCodec.decode(null))
    }

    @Test
    fun `time estimate`() {
        assertEquals(5, CleanupPlan(quota = 50).estimatedMinutes)
        assertEquals(1, CleanupPlan(quota = 10).estimatedMinutes)
        assertEquals(setOf(MONDAY, FRIDAY, SUNDAY), CleanupPlan(slots = listOf(slot(setOf(MONDAY, FRIDAY), 9), slot(setOf(SUNDAY), 9))).plannedDays)
    }
}
