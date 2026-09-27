package com.hatuka.swipeclean.ui.swipe

import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import com.hatuka.swipeclean.core.review.SwipeDirection
import com.hatuka.swipeclean.core.review.SwipeThresholds

/**
 * Position of the top card while it is dragged or animated. The commit decision itself is the
 * pure [SwipeThresholds.resolve] from :core. One instance per card (keyed by media ID).
 */
@Stable
class CardSwipeState(val thresholds: SwipeThresholds = SwipeThresholds()) {
    var offset by mutableStateOf(Offset.Zero)
        private set
    var size by mutableStateOf(IntSize.Zero)
    var isAnimating by mutableStateOf(false)
        private set

    /** Card rotation in degrees, proportional to the horizontal drag. */
    val rotation: Float get() = if (size.width == 0) 0f else offset.x / size.width * MAX_ROTATION

    fun dragBy(delta: Offset) {
        if (!isAnimating) offset += delta
    }

    fun lean(allowUp: Boolean): Pair<SwipeDirection, Float>? =
        thresholds.lean(offset.x, offset.y, size.width.toFloat(), size.height.toFloat(), allowUp)

    /** After the finger lifts: commit (and fly out) or spring back. Returns the committed direction. */
    suspend fun settle(velocity: Velocity, allowUp: Boolean): SwipeDirection? {
        val direction = thresholds.resolve(
            offset.x, offset.y, velocity.x, velocity.y, size.width.toFloat(), size.height.toFloat(), allowUp,
        )
        if (direction == null) {
            isAnimating = true
            animate(Offset.VectorConverter, offset, Offset.Zero, animationSpec = spring()) { v, _ -> offset = v }
            isAnimating = false
        } else {
            flyOut(direction)
        }
        return direction
    }

    /** Animates the card off screen (also used by the action buttons). */
    suspend fun flyOut(direction: SwipeDirection) {
        isAnimating = true
        val w = size.width.coerceAtLeast(1) * 1.5f
        val h = size.height.coerceAtLeast(1) * 1.5f
        val target = when (direction) {
            SwipeDirection.RIGHT -> Offset(w, offset.y)
            SwipeDirection.LEFT -> Offset(-w, offset.y)
            SwipeDirection.UP -> Offset(offset.x, -h)
        }
        animate(Offset.VectorConverter, offset, target, animationSpec = tween(FLY_OUT_MS)) { v, _ -> offset = v }
    }

    private companion object {
        const val MAX_ROTATION = 12f
        const val FLY_OUT_MS = 220
    }
}
