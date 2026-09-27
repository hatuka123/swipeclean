package com.hatuka.swipeclean

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import com.hatuka.swipeclean.ui.common.MediaThumbFetcher
import com.hatuka.swipeclean.ui.common.MediaThumbKeyer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SwipeCleanApp : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    /** WorkManager with Hilt-injected workers (the default initializer is removed in the manifest). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

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
