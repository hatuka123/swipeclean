package com.hatuka.swipeclean.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.hatuka.swipeclean.core.media.MediaType
import javax.inject.Inject
import javax.inject.Singleton

/** What happened to one item of a batched move. */
sealed interface MoveResult {
    /** RELATIVE_PATH changed in place; the media ID is unchanged. */
    data object Moved : MoveResult

    /**
     * The file could not be moved in place (e.g. WhatsApp media under Android/media/, which
     * Android does not let other apps relocate), so it was copied to the target as [newId].
     * [originalTrashed] = the original went to the system trash (recoverable ~30 days).
     */
    data class Copied(val newId: Long, val originalTrashed: Boolean) : MoveResult

    data object Failed : MoveResult
}

/**
 * Builds the system confirmation requests for changing media the app does not own, and applies
 * moves once the user granted write access. No file changes without a system dialog first.
 */
@Singleton
class MediaModifier @Inject constructor(
    private val resolver: ContentResolver,
) {
    /** Moves items to the system trash (recoverable for about 30 days). */
    fun trashRequest(uris: List<Uri>): IntentSender =
        MediaStore.createTrashRequest(resolver, uris, true).intentSender

    /** Asks for write access to items owned by other apps (needed to move them). */
    fun writeRequest(uris: List<Uri>): IntentSender =
        MediaStore.createWriteRequest(resolver, uris).intentSender

    /** Deletes items permanently. */
    fun deleteRequest(uris: List<Uri>): IntentSender =
        MediaStore.createDeleteRequest(resolver, uris).intentSender

    /**
     * Moves one item (after write access was granted). First tries to change RELATIVE_PATH in
     * place; if MediaStore refuses, copies the file to the target and moves the original to the
     * system trash, which gives the same result for the user.
     */
    fun moveTo(uri: Uri, type: MediaType, relativePath: String): MoveResult {
        if (moveInPlace(uri, relativePath)) return MoveResult.Moved
        val newId = copyTo(uri, type, relativePath) ?: return MoveResult.Failed
        val trashed = runCatching {
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 1) }, null, null) > 0
        }.onFailure { Log.w(TAG, "Copied, but could not trash the original $uri", it) }.getOrDefault(false)
        return MoveResult.Copied(newId, trashed)
    }

    private fun moveInPlace(uri: Uri, relativePath: String): Boolean = runCatching {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath) }
        resolver.update(uri, values, null, null) > 0
    }.onFailure { Log.w(TAG, "In-place move of $uri to $relativePath refused; copying instead", it) }.getOrDefault(false)

    /** Copies [source] into [relativePath] as a new item owned by this app; returns its ID. */
    private fun copyTo(source: Uri, type: MediaType, relativePath: String): Long? {
        val meta = resolver.query(source, COPY_PROJECTION, null, null, null)?.use { c ->
            if (!c.moveToFirst()) return null
            Triple(c.getString(0), c.getString(1), if (c.isNull(2)) 0L else c.getLong(2))
        } ?: return null
        val (name, mime, dateTaken) = meta
        val collection = when (type) {
            MediaType.IMAGE -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            MediaType.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            if (dateTaken > 0) put(MediaStore.MediaColumns.DATE_TAKEN, dateTaken)
        }
        val target = runCatching { resolver.insert(collection, values) }
            .onFailure { Log.w(TAG, "Could not create a copy in $relativePath", it) }
            .getOrNull() ?: return null
        return try {
            resolver.openInputStream(source).use { input ->
                resolver.openOutputStream(target).use { output ->
                    checkNotNull(input) { "No input stream" }.copyTo(checkNotNull(output) { "No output stream" })
                }
            }
            resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            ContentUris.parseId(target)
        } catch (e: Exception) {
            Log.w(TAG, "Copy of $source to $relativePath failed", e)
            runCatching { resolver.delete(target, null, null) }
            null
        }
    }

    private companion object {
        const val TAG = "MediaModifier"
        val COPY_PROJECTION = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
        )
    }
}
