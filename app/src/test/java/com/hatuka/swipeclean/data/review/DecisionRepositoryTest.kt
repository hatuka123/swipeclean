package com.hatuka.swipeclean.data.review

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hatuka.swipeclean.core.review.DecisionState
import com.hatuka.swipeclean.core.review.SwipeRecord
import com.hatuka.swipeclean.data.db.AppDatabase
import com.hatuka.swipeclean.data.db.BinSummary
import com.hatuka.swipeclean.testing.FIXED_CLOCK
import com.hatuka.swipeclean.testing.inMemoryDb
import com.hatuka.swipeclean.testing.row
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class DecisionRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: DecisionRepository
    private val today = LocalDate.now(FIXED_CLOCK).toEpochDay()

    @Before
    fun setUp() {
        db = inMemoryDb()
        repo = DecisionRepository(db, FIXED_CLOCK)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun todayStats() = db.stats().observeDay(today).first()

    @Test
    fun `record and revert keep the database and daily stats in sync`() = runTest {
        val keep = SwipeRecord(row(1), DecisionState.KEEP)
        val delete = SwipeRecord(row(2, size = 5000), DecisionState.DELETE_PENDING)
        repo.record(keep)
        repo.record(delete)
        assertEquals(setOf(1L, 2L), repo.decidedIds())
        assertEquals(BinSummary(1, 5000), repo.binSummary().first())
        assertEquals(2, todayStats()?.reviewed)
        assertEquals(1, todayStats()?.markedDelete)

        repo.revert(delete)
        assertEquals(setOf(1L), repo.decidedIds())
        assertEquals(BinSummary(0, 0), repo.binSummary().first())
        assertEquals(1, todayStats()?.reviewed)
        assertEquals(0, todayStats()?.markedDelete)
    }

    @Test
    fun `restoring from the bin keeps the item decided so it never reappears`() = runTest {
        repo.record(SwipeRecord(row(1), DecisionState.DELETE_PENDING))
        repo.changeDecision(listOf(1), DecisionState.KEEP)
        assertEquals(DecisionState.KEEP, repo.get(1)?.state)
        assertEquals(setOf(1L), repo.decidedIds())
        assertEquals(0, repo.binSummary().first().count)
    }

    @Test
    fun `deleted items become tombstones, count as freed and stay excluded`() = runTest {
        repo.record(SwipeRecord(row(1, size = 100), DecisionState.DELETE_PENDING))
        repo.record(SwipeRecord(row(2, size = 200), DecisionState.DELETE_PENDING))
        repo.markDeleted(repo.pendingDeletes())
        assertEquals(DecisionState.DELETED, repo.get(1)?.state)
        assertEquals(setOf(1L, 2L), repo.decidedIds())
        assertEquals(2, todayStats()?.deleted)
        assertEquals(300L, todayStats()?.bytesFreed)
        assertEquals(0, repo.binSummary().first().count)
    }

    @Test
    fun `stale items are dropped from the bin without counting as freed`() = runTest {
        repo.record(SwipeRecord(row(1, size = 100), DecisionState.DELETE_PENDING))
        repo.dropStale(listOf(1))
        assertEquals(0, repo.binSummary().first().count)
        assertEquals(0L, todayStats()?.bytesFreed)
        assertEquals(setOf(1L), repo.decidedIds())
    }

    @Test
    fun `revert of an unknown item is harmless`() = runTest {
        repo.revert(SwipeRecord(row(9), DecisionState.KEEP))
        assertNull(repo.get(9))
        assertEquals(0, todayStats()?.reviewed)
    }
}
