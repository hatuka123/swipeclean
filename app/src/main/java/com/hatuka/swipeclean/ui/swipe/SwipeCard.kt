package com.hatuka.swipeclean.ui.swipe

import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.core.media.MediaRow
import com.hatuka.swipeclean.core.media.MediaType
import com.hatuka.swipeclean.core.review.SwipeDirection
import com.hatuka.swipeclean.ui.common.formatSize
import com.hatuka.swipeclean.ui.theme.LocalActionColors
import kotlinx.coroutines.launch
import java.util.Date

const val SWIPE_CARD_TAG = "swipe_card"

/**
 * One full-screen card. The top card is draggable (rotation, colored overlay, threshold before
 * committing), photos are pinch/double-tap zoomable and videos auto-play muted with tap-to-unmute.
 * Drag offsets are physical, so right = keep in RTL too.
 */
@Composable
fun SwipeCard(
    item: MediaRow,
    uri: Uri?,
    state: CardSwipeState,
    isTop: Boolean,
    allowUp: Boolean,
    muted: Boolean,
    onToggleMute: () -> Unit,
    onSwiped: (SwipeDirection) -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val currentOnSwiped by rememberUpdatedState(onSwiped)
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var pan by remember(item.id) { mutableStateOf(Offset.Zero) }
    val isVideo = item.type == MediaType.VIDEO

    val gestures = if (!isTop) {
        Modifier
    } else {
        Modifier
            .pointerInput(item.id, allowUp) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
                    var zooming = scale > 1f
                    var dragging = false
                    var travelled = Offset.Zero
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        if (!isVideo && (pressed >= 2 || zooming)) {
                            // Pinch to zoom; while zoomed, one finger pans the photo instead of swiping.
                            zooming = true
                            scale = (scale * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            pan = if (scale == 1f) Offset.Zero else clampPan(pan + event.calculatePan(), scale, state)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else if (!zooming) {
                            val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()
                            val delta = change.positionChange()
                            if (!dragging) {
                                travelled += delta
                                if (travelled.getDistance() > viewConfiguration.touchSlop) dragging = true
                            }
                            if (dragging) {
                                state.dragBy(delta)
                                change.consume()
                                tracker.addPosition(change.uptimeMillis, change.position)
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    if (dragging && !zooming) {
                        val velocity = tracker.calculateVelocity()
                        scope.launch { state.settle(velocity, allowUp)?.let(currentOnSwiped) }
                    }
                }
            }
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { if (isVideo) onToggleMute() },
                    onDoubleTap = {
                        if (!isVideo) {
                            scale = if (scale > 1f) 1f else DOUBLE_TAP_ZOOM
                            pan = Offset.Zero
                        }
                    },
                )
            }
    }

    Box(
        modifier
            .onSizeChanged { if (isTop) state.size = it }
            .graphicsLayer {
                if (isTop) {
                    translationX = state.offset.x
                    translationY = state.offset.y
                    rotationZ = state.rotation
                }
            }
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .then(if (isTop) Modifier.testTag(SWIPE_CARD_TAG) else Modifier)
            .then(gestures),
    ) {
        when {
            uri == null -> Icon(
                Icons.Filled.BrokenImage,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.Center).size(72.dp),
            )
            isVideo && isTop -> VideoContent(uri, muted, onError = onUnavailable, modifier = Modifier.fillMaxSize())
            else -> AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { if (isTop) onUnavailable() },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = pan.x
                        translationY = pan.y
                    },
            )
        }

        if (isVideo) {
            Icon(
                if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = stringResource(if (muted) R.string.video_unmute else R.string.video_mute),
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(8.dp),
            )
        }

        InfoScrim(item, Modifier.align(Alignment.BottomCenter))

        if (isTop) {
            state.lean(allowUp)?.let { (direction, progress) -> LeanOverlay(direction, progress) }
        }
    }
}

@Composable
private fun InfoScrim(item: MediaRow, modifier: Modifier) {
    val context = LocalContext.current
    val date = remember(item.dateMillis) { DateFormat.getMediumDateFormat(context).format(Date(item.dateMillis)) }
    val folder = item.bucketName ?: item.relativePath.orEmpty()
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
            .padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 16.dp),
    ) {
        Text(
            stringResource(R.string.item_info, date, formatSize(item.sizeBytes), folder),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LeanOverlay(direction: SwipeDirection, progress: Float) {
    val colors = LocalActionColors.current
    val (color, label, alignment) = when (direction) {
        SwipeDirection.RIGHT -> Triple(colors.keep, R.string.action_keep, AbsoluteAlignment.TopLeft)
        SwipeDirection.LEFT -> Triple(colors.delete, R.string.action_delete, AbsoluteAlignment.TopRight)
        SwipeDirection.UP -> Triple(colors.move, R.string.action_move, Alignment.BottomCenter)
    }
    Box(Modifier.fillMaxSize().background(color.copy(alpha = 0.35f * progress))) {
        Text(
            stringResource(label).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .align(alignment)
                .padding(28.dp)
                .graphicsLayer { alpha = progress }
                .border(3.dp, Color.White, RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

private fun clampPan(pan: Offset, scale: Float, state: CardSwipeState): Offset {
    val maxX = state.size.width * (scale - 1f) / 2f
    val maxY = state.size.height * (scale - 1f) / 2f
    return Offset(pan.x.coerceIn(-maxX, maxX), pan.y.coerceIn(-maxY, maxY))
}

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
