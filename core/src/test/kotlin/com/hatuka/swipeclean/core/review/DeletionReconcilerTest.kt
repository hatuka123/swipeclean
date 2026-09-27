package com.hatuka.swipeclean.core.review

import org.junit.Assert.assertEquals
import org.junit.Test

class DeletionReconcilerTest {

    @Test
    fun `items no longer visible are deleted, the rest failed`() {
        val outcome = DeletionReconciler.reconcile(listOf(1, 2, 3), stillVisible = setOf(2), confirmed = true)
        assertEquals(listOf(1L, 3L), outcome.deleted)
        assertEquals(listOf(2L), outcome.failed)
    }

    @Test
    fun `cancelled dialog deletes nothing and reports nothing`() {
        val outcome = DeletionReconciler.reconcile(listOf(1, 2), stillVisible = emptySet(), confirmed = false)
        assertEquals(DeletionOutcome(emptyList(), emptyList()), outcome)
    }

    @Test
    fun `large requests are chunked`() {
        val chunks = DeletionReconciler.chunks((1..1201).toList())
        assertEquals(listOf(500, 500, 201), chunks.map { it.size })
    }
}
