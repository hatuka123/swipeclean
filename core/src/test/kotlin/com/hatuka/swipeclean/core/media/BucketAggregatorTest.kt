package com.hatuka.swipeclean.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BucketAggregatorTest {

    private fun row(id: Long, bucket: Long, name: String?, size: Long, type: MediaType = MediaType.IMAGE, date: Long = id) =
        MediaRow(id, bucket, name, "DCIM/$name/", size, type, date)

    private val rows = listOf(
        row(1, 10, "Camera", 100),
        row(2, 10, "Camera", 200, MediaType.VIDEO, date = 50),
        row(3, 10, "Camera", 300),
        row(4, 20, "Screenshots", 10),
        row(5, 30, "WhatsApp Images", 5),
        row(6, 30, "WhatsApp Images", 5),
    )

    @Test
    fun `aggregates counts and sizes per bucket and in total`() {
        val list = BucketAggregator.aggregate(rows.asSequence(), MediaFilter.BOTH)
        assertEquals(6, list.all.count)
        assertEquals(620L, list.all.sizeBytes)
        assertNull(list.all.bucketId)
        val camera = list.buckets.first { it.bucketId == 10L }
        assertEquals(3, camera.count)
        assertEquals(600L, camera.sizeBytes)
    }

    @Test
    fun `sorts by count descending then name`() {
        val names = BucketAggregator.aggregate(rows.asSequence(), MediaFilter.BOTH).buckets.map { it.name }
        assertEquals(listOf("Camera", "WhatsApp Images", "Screenshots"), names)
    }

    @Test
    fun `filter excludes other media types`() {
        val videos = BucketAggregator.aggregate(rows.asSequence(), MediaFilter.VIDEOS)
        assertEquals(1, videos.all.count)
        assertEquals(listOf(10L), videos.buckets.map { it.bucketId })

        val photos = BucketAggregator.aggregate(rows.asSequence(), MediaFilter.PHOTOS)
        assertEquals(5, photos.all.count)
        assertEquals(400L, photos.buckets.first { it.bucketId == 10L }.sizeBytes)
    }

    @Test
    fun `cover is the most recent item`() {
        val camera = BucketAggregator.aggregate(rows.asSequence(), MediaFilter.BOTH).buckets.first { it.bucketId == 10L }
        assertEquals(2L, camera.coverId)
        assertEquals(MediaType.VIDEO, camera.coverType)
    }

    @Test
    fun `missing bucket name falls back to the last path segment`() {
        val list = BucketAggregator.aggregate(sequenceOf(MediaRow(1, 7, null, "Pictures/Found/", 1, MediaType.IMAGE, 1)), MediaFilter.BOTH)
        assertEquals("Found", list.buckets.single().name)
    }

    @Test
    fun `negative sizes do not reduce totals and empty input is empty`() {
        val list = BucketAggregator.aggregate(sequenceOf(row(1, 1, "A", -5)), MediaFilter.BOTH)
        assertEquals(0L, list.all.sizeBytes)
        assertTrue(BucketAggregator.aggregate(emptySequence(), MediaFilter.BOTH).isEmpty)
    }
}
