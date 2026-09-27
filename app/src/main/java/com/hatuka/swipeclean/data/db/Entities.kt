package com.hatuka.swipeclean.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hatuka.swipeclean.core.media.MediaRow
import com.hatuka.swipeclean.core.media.MediaType
import com.hatuka.swipeclean.core.review.DecisionState

/**
 * One row per media item the user acted on. Rows are never deleted by normal use (a deletion
 * becomes a DELETED tombstone), which is what keeps reviewed items out of future sessions.
 */
@Entity(tableName = "decision", indices = [Index("state"), Index("bucketId")])
data class DecisionEntity(
    @PrimaryKey val mediaId: Long,
    val bucketId: Long,
    val bucketName: String?,
    val relativePath: String?,
    val sizeBytes: Long,
    val isVideo: Boolean,
    val dateMillis: Long,
    val state: DecisionState,
    val targetPath: String?,
    val decidedAt: Long,
) {
    fun toRow() = MediaRow(mediaId, bucketId, bucketName, relativePath, sizeBytes, if (isVideo) MediaType.VIDEO else MediaType.IMAGE, dateMillis)

    companion object {
        fun from(row: MediaRow, state: DecisionState, targetPath: String?, now: Long) = DecisionEntity(
            mediaId = row.id,
            bucketId = row.bucketId,
            bucketName = row.bucketName,
            relativePath = row.relativePath,
            sizeBytes = row.sizeBytes,
            isVideo = row.type == MediaType.VIDEO,
            dateMillis = row.dateMillis,
            state = state,
            targetPath = targetPath,
            decidedAt = now,
        )
    }
}

/** Per-day totals for quota progress, streaks and "storage freed". */
@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val epochDay: Long,
    val reviewed: Int = 0,
    @ColumnInfo(name = "marked_delete") val markedDelete: Int = 0,
    val deleted: Int = 0,
    @ColumnInfo(name = "bytes_freed") val bytesFreed: Long = 0,
)

data class BinSummary(val count: Int, val bytes: Long)
