package com.hatuka.swipeclean.data.media

import android.net.Uri
import com.hatuka.swipeclean.core.media.BucketAggregator
import com.hatuka.swipeclean.core.media.BucketList
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.MediaType
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

interface MediaRepository {
    /** Emits once immediately and then (debounced) whenever the gallery changes. */
    fun galleryChanges(): Flow<Unit>

    suspend fun loadBuckets(filter: MediaFilter): BucketList

    fun uriOf(type: MediaType, id: Long): Uri
}

@Singleton
class MediaStoreRepository @Inject constructor(
    private val source: MediaStoreSource,
    @IoDispatcher private val io: CoroutineDispatcher,
) : MediaRepository {

    @OptIn(FlowPreview::class)
    override fun galleryChanges(): Flow<Unit> =
        source.changes().debounce(CHANGE_DEBOUNCE_MS).onStart { emit(Unit) }

    override suspend fun loadBuckets(filter: MediaFilter): BucketList = withContext(io) {
        val types = MediaType.entries.filter(filter::includes).toSet()
        BucketAggregator.aggregate(source.queryRows(types).asSequence(), filter)
    }

    override fun uriOf(type: MediaType, id: Long): Uri = source.contentUri(type, id)

    private companion object {
        const val CHANGE_DEBOUNCE_MS = 500L
    }
}
