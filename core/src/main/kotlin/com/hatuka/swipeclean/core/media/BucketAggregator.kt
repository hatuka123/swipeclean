package com.hatuka.swipeclean.core.media

/**
 * Folds MediaStore rows into per-bucket summaries plus the "All" entry.
 * Buckets are sorted by item count (largest first), then by name, so the busiest folders
 * such as Camera come first. The cover of each bucket is its most recent item.
 */
object BucketAggregator {

    fun aggregate(rows: Sequence<MediaRow>, filter: MediaFilter): BucketList {
        val acc = LinkedHashMap<Long, Acc>()
        val all = Acc(name = null, relativePath = null)
        for (row in rows) {
            if (!filter.includes(row.type)) continue
            all.add(row)
            acc.getOrPut(row.bucketId) { Acc(row.bucketName, row.relativePath) }.add(row)
        }
        val buckets = acc.map { (id, a) -> a.toSummary(id, a.name ?: a.relativePath?.trimEnd('/')?.substringAfterLast('/') ?: "?") }
            .sortedWith(compareByDescending<BucketSummary> { it.count }.thenBy { it.name.lowercase() })
        return BucketList(all.toSummary(null, ""), buckets)
    }

    fun emptyAll() = BucketSummary(null, "", null, 0, 0L, null, null)

    private class Acc(val name: String?, val relativePath: String?) {
        var count = 0
        var size = 0L
        var coverId: Long? = null
        var coverType: MediaType? = null
        var coverDate = Long.MIN_VALUE

        fun add(row: MediaRow) {
            count++
            size += row.sizeBytes.coerceAtLeast(0)
            if (row.dateMillis > coverDate) {
                coverDate = row.dateMillis
                coverId = row.id
                coverType = row.type
            }
        }

        fun toSummary(id: Long?, displayName: String) =
            BucketSummary(id, displayName, relativePath, count, size, coverId, coverType)
    }
}
