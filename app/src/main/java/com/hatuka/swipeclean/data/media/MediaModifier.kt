package com.hatuka.swipeclean.data.media

import android.content.ContentResolver
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
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

    /** Deletes items permanently. */
    fun deleteRequest(uris: List<Uri>): IntentSender =
        MediaStore.createDeleteRequest(resolver, uris).intentSender
}
