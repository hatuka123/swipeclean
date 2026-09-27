package com.hatuka.swipeclean.core.review

import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.MediaRow
import com.hatuka.swipeclean.core.media.SortOrder

/** Decides what a swipe session shows and in which order. */
object DeckBuilder {

    /**
     * @param rows every visible item of the chosen source (bucket or all)
     * @param decided IDs that already have a decision in any state; they are never shown again
     */
    fun build(
        rows: List<MediaRow>,
        decided: Set<Long>,
        filter: MediaFilter,
        sort: SortOrder,
    ): List<MediaRow> {
        val candidates = rows.filter { filter.includes(it.type) && it.id !in decided }
        val comparator = when (sort) {
            SortOrder.OLDEST_FIRST -> compareBy<MediaRow> { it.dateMillis }.thenBy { it.id }
            SortOrder.NEWEST_FIRST -> compareByDescending<MediaRow> { it.dateMillis }.thenByDescending { it.id }
            SortOrder.LARGEST_FIRST -> compareByDescending<MediaRow> { it.sizeBytes }.thenBy { it.id }
        }
        return candidates.sortedWith(comparator)
    }
}
