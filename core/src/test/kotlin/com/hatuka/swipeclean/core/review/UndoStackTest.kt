package com.hatuka.swipeclean.core.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoStackTest {

    @Test
    fun `pops in reverse order`() {
        val stack = UndoStack<Int>(5)
        (1..3).forEach(stack::push)
        assertEquals(3, stack.peek())
        assertEquals(listOf(3, 2, 1), List(3) { stack.pop() })
        assertNull(stack.pop())
        assertTrue(stack.isEmpty)
    }

    @Test
    fun `drops the oldest entries beyond capacity`() {
        val stack = UndoStack<Int>(20)
        (1..25).forEach(stack::push)
        assertEquals(20, stack.size)
        val popped = generateSequence { stack.pop() }.toList()
        assertEquals((25 downTo 6).toList(), popped)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects zero capacity`() {
        UndoStack<Int>(0)
    }
}
