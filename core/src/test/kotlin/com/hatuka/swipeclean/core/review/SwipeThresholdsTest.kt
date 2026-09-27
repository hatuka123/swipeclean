package com.hatuka.swipeclean.core.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeThresholdsTest {

    private val t = SwipeThresholds()
    private val w = 1000f
    private val h = 2000f

    private fun resolve(dx: Float, dy: Float = 0f, vx: Float = 0f, vy: Float = 0f, allowUp: Boolean = true) =
        t.resolve(dx, dy, vx, vy, w, h, allowUp)

    @Test
    fun `short drags spring back`() {
        assertNull(resolve(200f))
        assertNull(resolve(-250f))
        assertNull(resolve(0f, dy = -300f))
    }

    @Test
    fun `far drags commit in the physical direction`() {
        assertEquals(SwipeDirection.RIGHT, resolve(350f))
        assertEquals(SwipeDirection.LEFT, resolve(-350f))
        assertEquals(SwipeDirection.UP, resolve(20f, dy = -450f))
    }

    @Test
    fun `fast flings commit after a short distance`() {
        assertEquals(SwipeDirection.RIGHT, resolve(100f, vx = 2500f))
        assertEquals(SwipeDirection.LEFT, resolve(-100f, vx = -2500f))
        assertEquals(SwipeDirection.UP, resolve(0f, dy = -200f, vy = -3000f))
        // A fling against the drag direction does not commit.
        assertNull(resolve(100f, vx = -2500f))
    }

    @Test
    fun `up is ignored when moves are unavailable and down never commits`() {
        assertNull(resolve(0f, dy = -800f, allowUp = false))
        assertNull(resolve(0f, dy = 800f))
    }

    @Test
    fun `lean reports direction and progress`() {
        assertEquals(SwipeDirection.RIGHT to 0.5f, t.lean(150f, 0f, w, h, true))
        assertEquals(SwipeDirection.LEFT to 1f, t.lean(-900f, 10f, w, h, true))
        assertEquals(SwipeDirection.UP to 0.5f, t.lean(0f, -200f, w, h, true))
        assertNull(t.lean(0f, -200f, w, h, false))
        assertNull(t.lean(0f, 0f, w, h, true))
    }
}
