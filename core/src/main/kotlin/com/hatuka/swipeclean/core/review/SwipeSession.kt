package com.hatuka.swipeclean.core.review

import com.hatuka.swipeclean.core.media.MediaRow

/** One committed swipe; undoing it removes the decision the swipe created. */
data class SwipeRecord(
    val item: MediaRow,
    val decision: DecisionState,
    val targetPath: String? = null,
)

data class SessionCounters(
    val reviewed: Int = 0,
    val markedForDeletion: Int = 0,
    val bytesToFree: Long = 0L,
)

/**
 * In-memory state of one swipe session: the ordered deck, the current position, live counters
 * and the undo stack. It has no I/O; the ViewModel persists each record and reverts it on undo.
 */
class SwipeSession(
    deck: List<MediaRow>,
    undoCapacity: Int = UndoStack.DEFAULT_CAPACITY,
) {
    private val deck = deck.toMutableList()
    private val undo = UndoStack<SwipeRecord>(undoCapacity)

    var position: Int = 0
        private set

    var counters: SessionCounters = SessionCounters()
        private set

    val current: MediaRow? get() = deck.getOrNull(position)
    val remaining: Int get() = deck.size - position
    val isFinished: Boolean get() = position >= deck.size
    val canUndo: Boolean get() = !undo.isEmpty

    /** The next [count] items after the current one, for preloading. */
    fun upcoming(count: Int): List<MediaRow> = deck.subList(minOf(position + 1, deck.size), minOf(position + 1 + count, deck.size)).toList()

    /** Records a decision for the current item and advances. */
    fun decide(decision: DecisionState, targetPath: String? = null): SwipeRecord {
        val item = checkNotNull(current) { "No item left to decide" }
        require(decision == DecisionState.KEEP || decision == DecisionState.DELETE_PENDING || decision == DecisionState.MOVE_PENDING) {
            "A swipe can only keep, mark for deletion or queue a move"
        }
        val record = SwipeRecord(item, decision, targetPath)
        undo.push(record)
        position++
        counters = counters.apply(record, +1)
        return record
    }

    /** Reverts the most recent decision and makes its item current again. */
    fun undo(): SwipeRecord? {
        val record = undo.pop() ?: return null
        position--
        // Stale items dropped after this record may have shifted the list; restore its slot.
        if (deck.getOrNull(position)?.id != record.item.id) deck.add(position, record.item)
        counters = counters.apply(record, -1)
        return record
    }

    /** Removes the current item without a decision (deleted or moved outside the app). */
    fun dropCurrent(): MediaRow? = if (isFinished) null else deck.removeAt(position)

    private fun SessionCounters.apply(record: SwipeRecord, sign: Int): SessionCounters {
        val isDelete = record.decision == DecisionState.DELETE_PENDING
        return copy(
            reviewed = reviewed + sign,
            markedForDeletion = markedForDeletion + if (isDelete) sign else 0,
            bytesToFree = bytesToFree + if (isDelete) sign * record.item.sizeBytes else 0,
        )
    }
}
