package com.hatuka.swipeclean.testing

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hatuka.swipeclean.core.media.BucketAggregator
import com.hatuka.swipeclean.core.media.BucketList
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.MediaRow
import com.hatuka.swipeclean.core.media.MediaType
import com.hatuka.swipeclean.data.db.AppDatabase
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.hatuka.swipeclean.data.media.MediaRepository
import com.hatuka.swipeclean.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** In-memory stand-in for MediaStore: tests add/remove rows to simulate the gallery. */
class FakeMediaRepository : MediaRepository {
    val rows = mutableListOf<MediaRow>()
    val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    var loads = 0

    override fun galleryChanges(): Flow<Unit> = changes.onStart { emit(Unit) }

    override suspend fun loadBuckets(filter: MediaFilter, decided: Set<Long>): BucketList {
        loads++
        return BucketAggregator.aggregate(rows.asSequence(), filter, decided)
    }

    override suspend fun loadRows(bucketId: Long?, filter: MediaFilter): List<MediaRow> =
        rows.filter { (bucketId == null || it.bucketId == bucketId) && filter.includes(it.type) }

    override suspend fun visibleIds(ids: Collection<Long>): Set<Long> {
        val present = rows.mapTo(HashSet()) { it.id }
        return ids.filterTo(HashSet()) { it in present }
    }

    override fun uriOf(type: MediaType, id: Long): Uri = Uri.parse("content://test/$id")
}

fun row(id: Long, bucket: Long = 10, size: Long = id * 1000, type: MediaType = MediaType.IMAGE, date: Long = id) =
    MediaRow(id, bucket, "Bucket$bucket", "DCIM/Bucket$bucket/", size, type, date)

fun inMemoryDb(): AppDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** A settings store backed by a fresh temp file, independent of other tests. */
fun testSettings(): SettingsRepository = SettingsRepository(
    PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { File.createTempFile("settings", ".preferences_pb").apply { delete() } },
    ),
)

val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC)
