package com.hatuka.swipeclean.ui.home

import android.Manifest
import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hatuka.swipeclean.core.access.MediaAccess
import com.hatuka.swipeclean.core.media.BucketAggregator
import com.hatuka.swipeclean.core.media.BucketList
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.MediaRow
import com.hatuka.swipeclean.core.media.MediaType
import com.hatuka.swipeclean.data.media.MediaRepository
import com.hatuka.swipeclean.permissions.MediaAccessMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class HomeViewModelTest {

    private class FakeRepository : MediaRepository {
        val rows = mutableListOf<MediaRow>()
        val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var loads = 0

        override fun galleryChanges(): Flow<Unit> = changes.onStart { emit(Unit) }
        override suspend fun loadBuckets(filter: MediaFilter): BucketList {
            loads++
            return BucketAggregator.aggregate(rows.asSequence(), filter)
        }
        override fun uriOf(type: MediaType, id: Long): Uri = Uri.parse("content://test/$id")
    }

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val repo = FakeRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun grantAll() = shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)

    @Test
    fun `no permission shows no access and never queries`() = runTest {
        val vm = HomeViewModel(repo, MediaAccessMonitor(app))
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        val state = vm.state.first { !it.loading }
        assertEquals(MediaAccess.NONE, state.access)
        assertEquals(0, repo.loads)
        job.cancel()
    }

    @Test
    fun `loads buckets, reacts to filter and gallery changes`() = runTest {
        grantAll()
        repo.rows += MediaRow(1, 10, "Camera", "DCIM/Camera/", 100, MediaType.IMAGE, 1)
        repo.rows += MediaRow(2, 10, "Camera", "DCIM/Camera/", 300, MediaType.VIDEO, 2)
        val vm = HomeViewModel(repo, MediaAccessMonitor(app))
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        assertEquals(2, vm.state.first { !it.loading }.buckets.all.count)

        vm.setFilter(MediaFilter.VIDEOS)
        val videos = vm.state.first { !it.loading && it.filter == MediaFilter.VIDEOS }
        assertEquals(1, videos.buckets.all.count)
        assertEquals(300L, videos.buckets.all.sizeBytes)

        repo.rows += MediaRow(3, 20, "Screenshots", "Pictures/Screenshots/", 5, MediaType.VIDEO, 3)
        repo.changes.emit(Unit)
        assertEquals(2, vm.state.first { it.buckets.all.count == 2 }.buckets.buckets.size)
        job.cancel()
    }
}
