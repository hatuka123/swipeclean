package com.hatuka.swipeclean.data.review

import androidx.room.withTransaction
import com.hatuka.swipeclean.core.review.DecisionState
import com.hatuka.swipeclean.core.review.SwipeRecord
import com.hatuka.swipeclean.data.db.AppDatabase
import com.hatuka.swipeclean.data.db.BinSummary
import com.hatuka.swipeclean.data.db.DecisionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All review decisions live here. Swipes only write rows; files change later, and only
 * through a system confirmation dialog.
 */
@Singleton
class DecisionRepository @Inject constructor(
    private val db: AppDatabase,
    private val clock: Clock,
) {
    private val decisions = db.decisions()
    private val stats = db.stats()

    private fun today() = LocalDate.now(clock).toEpochDay()

    suspend fun decidedIds(): Set<Long> = decisions.allDecidedIds().toHashSet()

    suspend fun record(record: SwipeRecord) = db.withTransaction {
        decisions.upsert(DecisionEntity.from(record.item, record.decision, record.targetPath, clock.millis()))
        stats.add(today(), reviewed = 1, markedDelete = if (record.decision == DecisionState.DELETE_PENDING) 1 else 0)
    }

    /** Undo of a swipe: the item had no decision before, so the row is removed again. */
    suspend fun revert(record: SwipeRecord) = db.withTransaction {
        decisions.delete(record.item.id)
        stats.add(today(), reviewed = -1, markedDelete = if (record.decision == DecisionState.DELETE_PENDING) -1 else 0)
    }

    fun binItems(): Flow<List<DecisionEntity>> = decisions.observeByState(DecisionState.DELETE_PENDING)

    fun binSummary(): Flow<BinSummary> = decisions.observeSummary(DecisionState.DELETE_PENDING)

    suspend fun pendingDeletes(): List<DecisionEntity> = decisions.byState(DecisionState.DELETE_PENDING)

    suspend fun get(mediaId: Long): DecisionEntity? = decisions.get(mediaId)

    /**
     * Replaces an earlier decision (e.g. "keep" for an item in the bin). The item stays decided,
     * so it never returns to a swipe session by itself.
     */
    suspend fun changeDecision(ids: List<Long>, state: DecisionState, targetPath: String? = null) {
        if (ids.isEmpty()) return
        decisions.setState(ids, state, targetPath, clock.millis())
    }

    /** Items the system confirmed as trashed/deleted become tombstones and count as freed space. */
    suspend fun markDeleted(items: List<DecisionEntity>) {
        if (items.isEmpty()) return
        db.withTransaction {
            decisions.setState(items.map { it.mediaId }, DecisionState.DELETED, null, clock.millis())
            stats.add(today(), deleted = items.size, bytesFreed = items.sumOf { it.sizeBytes })
        }
    }

    /** Items that vanished outside the app: silently tombstoned, not counted as freed by us. */
    suspend fun dropStale(ids: List<Long>) {
        if (ids.isEmpty()) return
        decisions.setState(ids, DecisionState.DELETED, null, clock.millis())
    }
}
