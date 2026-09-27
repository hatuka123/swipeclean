package com.hatuka.swipeclean.core.review

import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.MediaRow
import com.hatuka.swipeclean.core.media.MediaType
import com.hatuka.swipeclean.core.media.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class DeckBuilderTest {

    private fun row(id: Long, date: Long, size: Long, type: MediaType = MediaType.IMAGE) =
        MediaRow(id, 1, "Camera", "DCIM/Camera/", size, type, date)

    private val rows = listOf(
        row(1, date = 300, size = 10),
        row(2, date = 100, size = 50, type = MediaType.VIDEO),
        row(3, date = 200, size = 30),
        row(4, date = 100, size = 20),
    )

    private fun ids(decided: Set<Long> = emptySet(), filter: MediaFilter = MediaFilter.BOTH, sort: SortOrder = SortOrder.OLDEST_FIRST) =
        DeckBuilder.build(rows, decided, filter, sort).map { it.id }

    @Test
    fun `oldest first is the default order with id as tie breaker`() {
        assertEquals(listOf(2L, 4L, 3L, 1L), ids())
    }

    @Test
    fun `newest and largest first`() {
        assertEquals(listOf(1L, 3L, 4L, 2L), ids(sort = SortOrder.NEWEST_FIRST))
        assertEquals(listOf(2L, 3L, 4L, 1L), ids(sort = SortOrder.LARGEST_FIRST))
    }

    @Test
    fun `items with any decision are never shown again`() {
        // Kept, pending delete, pending move, moved and deleted-tombstone IDs are all "decided".
        assertEquals(listOf(4L, 1L), ids(decided = setOf(2L, 3L)))
        assertEquals(emptyList<Long>(), ids(decided = setOf(1L, 2L, 3L, 4L)))
    }

    @Test
    fun `type filter`() {
        assertEquals(listOf(2L), ids(filter = MediaFilter.VIDEOS))
        assertEquals(listOf(4L, 3L, 1L), ids(filter = MediaFilter.PHOTOS))
    }
}
