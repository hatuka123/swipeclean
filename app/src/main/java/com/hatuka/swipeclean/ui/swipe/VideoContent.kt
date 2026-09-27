package com.hatuka.swipeclean.ui.swipe

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil3.compose.AsyncImage

const val VIDEO_POSTER_TAG = "video_poster"

/**
 * Auto-playing, looping video for the top card. Starts muted; audio focus is only requested
 * while unmuted, so a muted preview never interrupts the user's music. The poster frame is
 * shown until the first video frame is rendered.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoContent(
    uri: Uri,
    muted: Boolean,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentOnError by rememberUpdatedState(onError)
    // Prepared only after the listener is attached (below), so no size/first-frame event is missed.
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
        }
    }
    var aspect by remember(uri) { mutableFloatStateOf(0f) }
    var firstFrame by remember(uri) { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }

            override fun onRenderedFirstFrame() {
                firstFrame = true
            }

            override fun onPlayerError(error: PlaybackException) {
                currentOnError()
            }
        }
        player.addListener(listener)
        player.prepare()
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, muted) {
        player.volume = if (muted) 0f else 1f
        player.setAudioAttributes(AudioAttributes.DEFAULT, !muted)
    }

    // Pause in the background, resume when visible again.
    LifecycleResumeEffect(player) {
        player.play()
        onPauseOrDispose { player.pause() }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        // The surface exists from the start (the player needs it to render any frame); it only
        // changes shape once the video size is known. The poster covers it until the first frame.
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = if (aspect > 0f) Modifier.aspectRatio(aspect) else Modifier.fillMaxSize(),
        )
        if (!firstFrame) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().testTag(VIDEO_POSTER_TAG),
            )
        }
    }
}
