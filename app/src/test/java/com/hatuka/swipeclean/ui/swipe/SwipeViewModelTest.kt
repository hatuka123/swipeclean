package com.hatuka.swipeclean.ui.swipe

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.review.DecisionState
import com.hatuka.swipeclean.data.db.AppDatabase
import com.hatuka.swipeclean.data.review.DecisionRepository
import com.hatuka.swipeclean.testing.FIXED_CLOCK
import com.hatuka.swipeclean.testing.FakeMediaRepository
import com.hatuka.swipeclean.testing.inMemoryDb
import com.hatuka.swipeclean.testing.testSettings
import com.hatuka.swipeclean.core.media.SortOrder
import com.hatuka.swipeclean.data.review.DecisionActions
import com.hatuka.swipeclean.testing.row
import com.hatuka.swipeclean.ui.nav.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SwipeViewModelTest {

    private val media = FakeMediaRepository()
    private lateinit var db: AppDatabase
    private lateinit var decisions: DecisionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = inMemoryDb()
        decisions = DecisionRepository(db, FIXED_CLOCK)
        media.rows += (1L..5L).map { row(it, bucket = 10) }
        media.rows += row(100, bucket = 20)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private val settings = testSettings()

    private fun viewModel(bucket: Long = 10, sort: SortOrder = SortOrder.OLDEST_FIRST) = SwipeViewModel(
        SavedStateHandle(mapOf("bucket" to bucket, "filter" to MediaFilter.BOTH.name, "sort" to sort.name)),
        media,
        decisions,
        settings,
        DecisionActions(decisions, media, settings),
    )

    private suspend fun SwipeViewModel.loaded() = state.first { !it.loading }

    /** Decisions are written asynchronously (Room runs on its own threads); wait for them. */
    private suspend fun eventually(condition: suspend () -> Boolean) {
        repeat(200) {
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("Condition not met in time")
    }

    @Test
    fun `deck is the chosen bucket, oldest first`() = runTest {
        val s = viewModel().loaded()
        assertEquals(1L, s.current?.id)
        assertEquals(5, s.remaining)
        assertEquals(listOf(2L, 3L, 4L), s.upcoming.map { it.id })
        assertEquals("Bucket10", s.sourceName)
        assertEquals(6, viewModel(Routes.ALL_BUCKETS).loaded().remaining)
    }

    @Test
    fun `swipes are persisted and undo reverts them`() = runTest {
        val vm = viewModel()
        vm.loaded()
        vm.keep()
        vm.markForDeletion()
        assertEquals(3L, vm.state.value.current?.id)
        assertEquals(1, vm.state.value.counters.markedForDeletion)
        eventually { decisions.get(2)?.state == DecisionState.DELETE_PENDING }

        vm.undo()
        assertEquals(2L, vm.state.value.current?.id)
        eventually { decisions.get(2) == null }
        assertNull(decisions.get(2))
        assertEquals(DecisionState.KEEP, decisions.get(1)?.state)
    }

    @Test
    fun `a new session never shows items reviewed before`() = runTest {
        val first = viewModel()
        first.loaded()
        first.keep()
        first.markForDeletion()
        eventually { decisions.decidedIds().size == 2 }
        // Restore item 2 from the bin as "keep".
        decisions.changeDecision(listOf(2), DecisionState.KEEP)

        val second = viewModel().loaded()
        assertEquals(3L, second.current?.id)
        assertEquals(3, second.remaining)
    }

    @Test
    fun `an item deleted outside the app is skipped silently`() = runTest {
        val vm = viewModel()
        vm.loaded()
        media.rows.removeAll { it.id == 1L }
        vm.onItemUnavailable(1)
        assertEquals(2L, vm.state.value.current?.id)
        assertEquals(0, vm.state.value.counters.reviewed)
        assertFalse(vm.state.value.canUndo)
    }

    @Test
    fun `an item that is still there is not skipped on a display error`() = runTest {
        val vm = viewModel()
        vm.loaded()
        vm.onItemUnavailable(1)
        assertEquals(1L, vm.state.value.current?.id)
    }

    @Test
    fun `finishing the deck`() = runTest {
        val vm = viewModel(bucket = 20)
        vm.loaded()
        vm.keep()
        assertTrue(vm.state.value.finished)
        vm.keep() // ignored
        assertEquals(1, vm.state.value.counters.reviewed)
    }

    @Test
    fun `swipe up queues a move to the default folder and undo reverts it`() = runTest {
        val vm = viewModel()
        vm.loaded()
        vm.defaultTarget.first()
        assertTrue(vm.moveToDefault())
        eventually { decisions.get(1)?.state == DecisionState.MOVE_PENDING }
        assertEquals("Pictures/Found/", decisions.get(1)?.targetPath)
        assertEquals(2L, vm.state.value.current?.id)
        vm.undo()
        eventually { decisions.get(1) == null }
        assertEquals(1L, vm.state.value.current?.id)
    }

    @Test
    fun `choosing a folder can make it the new default`() = runTest {
        val vm = viewModel()
        vm.loaded()
        vm.moveTo("Pictures//Trips", makeDefault = true)
        eventually { decisions.get(1)?.targetPath == "Pictures/Trips/" }
        eventually { settings.defaultTarget.first() == "Pictures/Trips/" }
    }

    @Test
    fun `moving into the folder the item is already in just keeps it`() = runTest {
        val vm = viewModel()
        vm.loaded()
        vm.moveTo("DCIM/Bucket10/", makeDefault = false)
        eventually { decisions.get(1)?.state == DecisionState.KEEP }
    }

    @Test
    fun `sort order comes from the navigation arguments`() = runTest {
        assertEquals(5L, viewModel(sort = SortOrder.NEWEST_FIRST).loaded().current?.id)
        assertEquals(5L, viewModel(sort = SortOrder.LARGEST_FIRST).loaded().current?.id)
    }
}
