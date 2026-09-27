package com.hatuka.swipeclean.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.hatuka.swipeclean.core.media.MediaRow
import com.hatuka.swipeclean.core.media.MediaType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over MediaStore queries. Only content URIs are used, never file paths.
 * Queries return what the current permission allows: with partial access (Android 14+)
 * MediaStore itself limits the results to the items the user selected.
 */
@Singleton
class MediaStoreSource @Inject constructor(
    private val resolver: ContentResolver,
) {
    fun collection(type: MediaType): Uri = when (type) {
        MediaType.IMAGE -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        MediaType.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    }

    fun contentUri(type: MediaType, id: Long): Uri = ContentUris.withAppendedId(collection(type), id)

    /** Every visible image and video, reduced to the columns the folder list needs. */
    fun queryRows(types: Set<MediaType>): List<MediaRow> {
        val rows = ArrayList<MediaRow>()
        for (type in types) {
            resolver.query(collection(type), ROW_PROJECTION, null, null, null)?.use { c ->
                val id = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val bucketId = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                val bucketName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val path = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                val size = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val taken = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                val modified = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                rows.ensureCapacity(rows.size + c.count)
                while (c.moveToNext()) {
                    val takenMillis = if (c.isNull(taken)) 0L else c.getLong(taken)
                    rows += MediaRow(
                        id = c.getLong(id),
                        bucketId = c.getLong(bucketId),
                        bucketName = c.getString(bucketName),
                        relativePath = c.getString(path),
                        sizeBytes = c.getLong(size),
                        type = type,
                        dateMillis = if (takenMillis > 0) takenMillis else c.getLong(modified) * 1000,
                    )
                }
            }
        }
        return rows
    }

    /** Emits whenever images or videos change (added, deleted, moved) outside or inside the app. */
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        for (type in MediaType.entries) resolver.registerContentObserver(collection(type), true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    private companion object {
        val ROW_PROJECTION = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
    }
}
