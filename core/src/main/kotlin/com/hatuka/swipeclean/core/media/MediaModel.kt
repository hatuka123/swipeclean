package com.hatuka.swipeclean.core.media

enum class MediaType { IMAGE, VIDEO }

/** Which kinds of media a session or the folder list includes. */
enum class MediaFilter {
    PHOTOS, VIDEOS, BOTH;

    fun includes(type: MediaType): Boolean = when (this) {
        PHOTOS -> type == MediaType.IMAGE
        VIDEOS -> type == MediaType.VIDEO
        BOTH -> true
    }
}

enum class SortOrder { OLDEST_FIRST, NEWEST_FIRST, LARGEST_FIRST }

/** One MediaStore row, reduced to what the folder list needs. */
data class MediaRow(
    val id: Long,
    val bucketId: Long,
    val bucketName: String?,
    val relativePath: String?,
    val sizeBytes: Long,
    val type: MediaType,
    val dateMillis: Long,
)

/**
 * A MediaStore bucket (a folder such as Camera or WhatsApp Images).
 * [bucketId] is null for the synthetic "All photos & videos" entry.
 */
data class BucketSummary(
    val bucketId: Long?,
    val name: String,
    val relativePath: String?,
    val count: Int,
    val sizeBytes: Long,
    val coverId: Long?,
    val coverType: MediaType?,
)

data class BucketList(
    val all: BucketSummary,
    val buckets: List<BucketSummary>,
) {
    val isEmpty: Boolean get() = all.count == 0

    companion object {
        val EMPTY = BucketList(BucketAggregator.emptyAll(), emptyList())
    }
}
