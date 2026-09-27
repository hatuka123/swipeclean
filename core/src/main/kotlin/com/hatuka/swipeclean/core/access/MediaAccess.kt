package com.hatuka.swipeclean.core.access

/** How much of the gallery the app can currently read. */
enum class MediaAccess { NONE, PARTIAL, FULL }

/**
 * Maps the granted runtime permissions to a [MediaAccess] level for a given API level.
 * Permission names are the plain manifest strings so this stays free of Android classes.
 */
object MediaAccessResolver {
    const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
    const val READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES"
    const val READ_MEDIA_VIDEO = "android.permission.READ_MEDIA_VIDEO"
    const val READ_MEDIA_VISUAL_USER_SELECTED = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"

    private const val TIRAMISU = 33
    private const val UPSIDE_DOWN_CAKE = 34

    /** Permissions to request together; on 34+ this lets the system offer "Select photos". */
    fun permissionsToRequest(sdkInt: Int): List<String> = when {
        sdkInt >= UPSIDE_DOWN_CAKE -> listOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED)
        sdkInt >= TIRAMISU -> listOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO)
        else -> listOf(READ_EXTERNAL_STORAGE)
    }

    fun resolve(sdkInt: Int, isGranted: (String) -> Boolean): MediaAccess {
        if (sdkInt < TIRAMISU) {
            return if (isGranted(READ_EXTERNAL_STORAGE)) MediaAccess.FULL else MediaAccess.NONE
        }
        val images = isGranted(READ_MEDIA_IMAGES)
        val video = isGranted(READ_MEDIA_VIDEO)
        return when {
            images && video -> MediaAccess.FULL
            images || video -> MediaAccess.PARTIAL
            sdkInt >= UPSIDE_DOWN_CAKE && isGranted(READ_MEDIA_VISUAL_USER_SELECTED) -> MediaAccess.PARTIAL
            else -> MediaAccess.NONE
        }
    }
}
