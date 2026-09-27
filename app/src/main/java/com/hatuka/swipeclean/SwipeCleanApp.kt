package com.hatuka.swipeclean

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import com.hatuka.swipeclean.ui.common.MediaThumbFetcher
import com.hatuka.swipeclean.ui.common.MediaThumbKeyer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SwipeCleanApp : Application(), SingletonImageLoader.Factory {

    /**
     * Coil with MediaStore thumbnails for lists and grids, and video-frame decoding for
     * full-screen video posters.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(MediaThumbKeyer())
                add(MediaThumbFetcher.Factory(contentResolver))
                add(VideoFrameDecoder.Factory())
            }
            .build()
}
