package com.hatuka.swipeclean.core.review

import kotlin.math.abs
import kotlin.math.min

/** Physical swipe directions (right = keep in every language, including RTL). */
enum class SwipeDirection { LEFT, RIGHT, UP }

/**
 * When a drag commits. A card commits once it has travelled far enough, or when it is flung
 * fast in a direction after a short distance. Everything else springs back.
 */
data class SwipeThresholds(
    /** Fraction of the card width needed to commit left/right without a fling. */
    val horizontalDistance: Float = 0.30f,
    /** Fraction of the card height needed to commit up without a fling. */
    val verticalDistance: Float = 0.20f,
    /** Fling speed (px/s) that commits after a short distance. */
    val flingVelocity: Float = 1500f,
    /** Minimum travel (fraction of the size) for a fling to count. */
    val flingMinDistance: Float = 0.08f,
) {
    fun resolve(dx: Float, dy: Float, vx: Float, vy: Float, width: Float, height: Float, allowUp: Boolean): SwipeDirection? {
        val horizontal = abs(dx) >= abs(dy)
        return if (horizontal) {
            val far = abs(dx) > width * horizontalDistance
            val flung = abs(dx) > width * flingMinDistance && abs(vx) > flingVelocity && vx * dx > 0
            when {
                !far && !flung -> null
                dx > 0 -> SwipeDirection.RIGHT
                else -> SwipeDirection.LEFT
            }
        } else {
            if (!allowUp || dy >= 0) return null
            val far = -dy > height * verticalDistance
            val flung = -dy > height * flingMinDistance && vy < -flingVelocity
            if (far || flung) SwipeDirection.UP else null
        }
    }

    /**
     * Which direction the card currently leans to and how close it is to committing (0..1),
     * for the colored overlay while dragging.
     */
    fun lean(dx: Float, dy: Float, width: Float, height: Float, allowUp: Boolean): Pair<SwipeDirection, Float>? {
        if (dx == 0f && dy == 0f) return null
        return if (abs(dx) >= abs(dy)) {
            (if (dx > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT) to min(1f, abs(dx) / (width * horizontalDistance))
        } else if (allowUp && dy < 0) {
            SwipeDirection.UP to min(1f, -dy / (height * verticalDistance))
        } else {
            null
        }
    }
}
