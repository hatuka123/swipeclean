package com.hatuka.swipeclean

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.hatuka.swipeclean.core.access.MediaAccessResolver
import com.hatuka.swipeclean.ui.swipe.SWIPE_CARD_TAG
import com.hatuka.swipeclean.ui.swipe.VIDEO_POSTER_TAG
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Regression test for "sound plays but the picture is frozen": opens the seeded videos
 * (DCIM/SeedCamera, filter Videos), waits for the first rendered frame and checks that the
 * picture keeps changing (the seeded clips are animated test patterns).
 */
@RunWith(AndroidJUnit4::class)
class VideoPlaybackTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(GrantPermissionRule.grant(*MediaAccessResolver.permissionsToRequest(Build.VERSION.SDK_INT).toTypedArray()))
        .around(compose)

    private fun exists(matcher: androidx.compose.ui.test.SemanticsMatcher) =
        compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun videoPictureMoves() {
        resetHomeFilter(compose)
        compose.waitUntil(20_000) { exists(hasText("SeedCamera")) }
        compose.onNodeWithText("Videos").performClick()
        compose.waitUntil(10_000) { !exists(hasText("SeedScreens")) }
        clickText(compose, "SeedCamera")
        compose.waitUntil(20_000) { exists(hasTestTag(SWIPE_CARD_TAG)) }

        // The poster disappears once the first video frame has been rendered.
        compose.waitUntil(30_000) { !exists(hasTestTag(VIDEO_POSTER_TAG)) }

        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        Thread.sleep(500)
        val first = checkNotNull(uiAutomation.takeScreenshot())
        Thread.sleep(1_500)
        val second = checkNotNull(uiAutomation.takeScreenshot())
        takeScreenshot("video_playing")

        val changed = changedFraction(first, second)
        assertTrue("Video picture did not change between frames (changed=$changed)", changed > 0.01)
    }

    /** Fraction of sampled pixels in the middle of the screen that differ between two frames. */
    private fun changedFraction(a: Bitmap, b: Bitmap): Double {
        val w = minOf(a.width, b.width)
        val h = minOf(a.height, b.height)
        var changed = 0
        var total = 0
        for (y in h / 4 until h * 3 / 4 step 8) {
            for (x in w / 8 until w * 7 / 8 step 8) {
                total++
                if (a.getPixel(x, y) != b.getPixel(x, y)) changed++
            }
        }
        return changed.toDouble() / total
    }
}
