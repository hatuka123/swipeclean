package com.hatuka.swipeclean.ui.common

import android.content.ContentResolver
import android.net.Uri
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options

/**
 * Coil model for a small grid/list thumbnail. Uses [ContentResolver.loadThumbnail], which is
 * served from MediaStore's own thumbnail cache — far cheaper than decoding the full photo or
 * extracting a video frame, which matters with tens of thousands of items.
 */
data class MediaThumb(val uri: Uri, val sizePx: Int)

class MediaThumbFetcher(
    private val resolver: ContentResolver,
    private val data: MediaThumb,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val bitmap = resolver.loadThumbnail(data.uri, Size(data.sizePx, data.sizePx), null)
        return ImageFetchResult(image = bitmap.asImage(), isSampled = true, dataSource = DataSource.DISK)
    }

    class Factory(private val resolver: ContentResolver) : Fetcher.Factory<MediaThumb> {
        override fun create(data: MediaThumb, options: Options, imageLoader: ImageLoader): Fetcher =
            MediaThumbFetcher(resolver, data)
    }
}

/** Lets Coil memory-cache thumbnails by URI and size. */
class MediaThumbKeyer : Keyer<MediaThumb> {
    override fun key(data: MediaThumb, options: Options): String = "${data.uri}#${data.sizePx}"
}
