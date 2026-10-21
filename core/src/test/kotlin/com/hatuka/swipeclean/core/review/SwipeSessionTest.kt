package com.hatuka.swipeclean.core.review

import com.hatuka.swipeclean.core.media.MediaRow
import com.hatuka.swipeclean.core.media.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SwipeSessionTest {

    private fun row(id: Long, size: Long = id * 100) = MediaRow(id, 1, "Camera", "DCIM/Camera/", size, MediaType.IMAGE, id)
    private fun session(n: Int = 5, capacity: Int = 20) = SwipeSession((1L..n).map { row(it) }, capacity)

    @Test
    fun `keep and delete advance and update counters`() {
        val s = session()
        assertEquals(1L, s.current?.id)
        s.decide(DecisionState.KEEP)
        s.decide(DecisionState.DELETE_PENDING)
        assertEquals(3L, s.current?.id)
        assertEquals(SessionCounters(reviewed = 2, markedForDeletion = 1, bytesToFree = 200), s.counters)
        assertEquals(3, s.remaining)
    }

    @Test
    fun `undo restores the item and counters for every action type`() {
        val s = session()
        s.decide(DecisionState.KEEP)
        s.decide(DecisionState.DELETE_PENDING)
        s.decide(DecisionState.MOVE_PENDING, "Pictures/Found/")

        assertEquals(DecisionState.MOVE_PENDING, s.undo()?.decision)
        assertEquals(3L, s.current?.id)
        assertEquals(DecisionState.DELETE_PENDING, s.undo()?.decision)
        assertEquals(2L, s.current?.id)
        assertEquals(SessionCounters(reviewed = 1), s.counters)
        assertEquals(DecisionState.KEEP, s.undo()?.decision)
        assertEquals(SessionCounters(), s.counters)
        assertNull(s.undo())
        assertFalse(s.canUndo)
    }

    @Test
    fun `undo is limited to the stack capacity`() {
        val s = session(n = 30, capacity = 20)
        repeat(25) { s.decide(DecisionState.KEEP) }
        var undone = 0
        while (s.undo() != null) undone++
        assertEquals(20, undone)
        assertEquals(6L, s.current?.id)
    }

    @Test
    fun `dropping a stale item does not count and undo still returns the right item`() {
        val s = session()
        s.decide(DecisionState.DELETE_PENDING) // item 1
        assertEquals(2L, s.dropCurrent()?.id)  // item 2 vanished from the gallery
        assertEquals(3L, s.current?.id)
        assertEquals(1, s.counters.reviewed)
        assertEquals(1L, s.undo()?.item?.id)
        assertEquals(1L, s.current?.id)
        s.decide(DecisionState.KEEP)
        assertEquals(3L, s.current?.id)
    }

    @Test
    fun `finishes after the last item`() {
        val s = session(n = 2)
        s.decide(DecisionState.KEEP)
        s.decide(DecisionState.KEEP)
        assertTrue(s.isFinished)
        assertNull(s.current)
        assertEquals(emptyList<MediaRow>(), s.upcoming(3))
    }

    @Test
    fun `upcoming returns the next items for preloading`() {
        val s = session()
        assertEquals(listOf(2L, 3L, 4L), s.upcoming(3).map { it.id })
        s.decide(DecisionState.KEEP)
        assertEquals(listOf(3L, 4L, 5L), s.upcoming(3).map { it.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a swipe cannot produce a final state`() {
        session().decide(DecisionState.DELETED)
    }

    @Test
    fun `shuffle mixes only the unreviewed items and always changes the current one`() {
        repeat(20) { seed ->
            val s = session(6)
            s.decide(DecisionState.KEEP)
            s.shuffleRemaining(Random(seed))
            assertTrue(s.current?.id != 2L)
            assertEquals(5, s.remaining)
            val rest = buildList { while (!s.isFinished) add(s.decide(DecisionState.KEEP).item.id) }
            assertEquals((2L..6L).toSet(), rest.toSet())
        }
    }

    @Test
    fun `undo after a shuffle brings back the last reviewed item`() {
        val s = session()
        s.decide(DecisionState.DELETE_PENDING)
        s.shuffleRemaining(Random(1))
        assertEquals(1L, s.undo()?.item?.id)
        assertEquals(1L, s.current?.id)
        assertEquals(5, s.remaining)
    }

    @Test
    fun `shuffle with one item left does nothing`() {
        val s = session(2)
        s.decide(DecisionState.KEEP)
        s.shuffleRemaining(Random(3))
        assertEquals(2L, s.current?.id)
    }
}
