package com.hatuka.swipeclean.data.media

import android.content.ContentResolver
import android.content.ContentValues
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the system confirmation requests for changing media the app does not own.
 * Nothing here touches a file directly: the user confirms in a system dialog first.
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

    /**
     * Moves one item after write access was granted by changing its RELATIVE_PATH. The media ID
     * stays the same, so the item remains "reviewed". Returns false if MediaStore refused.
     */
    fun moveTo(uri: Uri, relativePath: String): Boolean = runCatching {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath) }
        resolver.update(uri, values, null, null) > 0
    }.onFailure { Log.w(TAG, "Move to $relativePath failed", it) }.getOrDefault(false)

    /** Deletes items permanently. */
    fun deleteRequest(uris: List<Uri>): IntentSender =
        MediaStore.createDeleteRequest(resolver, uris).intentSender

    private companion object {
        const val TAG = "MediaModifier"
    }
}
