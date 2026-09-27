package com.hatuka.swipeclean

import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.hatuka.swipeclean.core.access.MediaAccessResolver
import com.hatuka.swipeclean.ui.bin.BIN_ITEM_TAG
import com.hatuka.swipeclean.ui.bin.CHANGE_DECISION_TAG
import com.hatuka.swipeclean.ui.swipe.SWIPE_CARD_TAG
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * End-to-end on the seeded folder Pictures/SeedScreens (5 photos, oldest first):
 * swipe/buttons/undo → bin → change a decision → batched trash through the real system dialog →
 * a new session shows only the one item never reviewed.
 */
@RunWith(AndroidJUnit4::class)
class SwipeFlowTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(GrantPermissionRule.grant(*MediaAccessResolver.permissionsToRequest(Build.VERSION.SDK_INT).toTypedArray()))
        .around(compose)

    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun waitForText(text: String, substring: Boolean = false, timeoutMs: Long = 20_000) =
        waitForText(compose, text, substring, timeoutMs)

    private fun seedScreensCount(): Int {
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        return resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.BUCKET_DISPLAY_NAME} = ?",
            arrayOf("SeedScreens"),
            null,
        )?.use { it.count } ?: -1
    }

    @Test
    fun swipeUndoBinDeleteAndNoRepeats() {
        resetHomeFilter(compose)
        waitForText("SeedScreens")
        compose.onNodeWithText("SeedScreens").performClick()
        waitForText("5 left")
        Thread.sleep(2_000) // let the photo decode for the screenshot
        takeScreenshot("swipe")

        // Drag a little: colored "keep" overlay, below the threshold, springs back.
        compose.onNodeWithTag(SWIPE_CARD_TAG).performTouchInput {
            down(center)
            moveBy(Offset(width * 0.22f, 0f))
        }
        compose.waitForIdle()
        takeScreenshot("swipe_dragging")
        compose.onNodeWithTag(SWIPE_CARD_TAG).performTouchInput { up() }
        waitForText("5 left")

        // Swipe right = keep (item 1).
        compose.onNodeWithTag(SWIPE_CARD_TAG).performTouchInput { swipeRight() }
        waitForText("4 left")

        // Button delete (item 2), undo, delete again (2), delete (3), keep (4).
        compose.onNodeWithContentDescription("Mark for deletion").performClick()
        waitForText("3 left")
        compose.onNodeWithContentDescription("Undo").performClick()
        waitForText("4 left")
        compose.onNodeWithContentDescription("Mark for deletion").performClick()
        waitForText("3 left")
        compose.onNodeWithContentDescription("Mark for deletion").performClick()
        waitForText("2 left")
        compose.onNodeWithContentDescription("Keep").performClick()
        waitForText("1 left")
        waitForText("To delete 2")
        takeScreenshot("swipe_counters")

        // Bin: two items; change the decision for one of them to "keep".
        compose.onNodeWithContentDescription("Bin").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(BIN_ITEM_TAG).fetchSemanticsNodes().size == 2 }
        Thread.sleep(1_500)
        takeScreenshot("bin")
        compose.onAllNodesWithTag(BIN_ITEM_TAG)[0].performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(CHANGE_DECISION_TAG).fetchSemanticsNodes().isNotEmpty() }
        Thread.sleep(1_500)
        takeScreenshot("change_decision")
        compose.onNodeWithText("Keep").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(BIN_ITEM_TAG).fetchSemanticsNodes().size == 1 }

        // Delete all: one batched system request, confirmed in the real dialog.
        assertEquals(5, seedScreensCount())
        compose.onNodeWithText("Delete all", substring = true).performClick()
        val confirm = waitForSystemConfirm(compose, "no_trash_dialog")
        takeScreenshot("trash_dialog")
        confirm.click()
        waitForText("The bin is empty", timeoutMs = 60_000)
        assertEquals(4, seedScreensCount())
        takeScreenshot("bin_after_delete")

        // A new session in the same folder shows only item 5 – nothing reviewed comes back.
        compose.onNodeWithContentDescription("Back").performClick() // bin → swipe
        compose.onNodeWithContentDescription("Back").performClick() // swipe → home
        waitForText("4 items", substring = true)
        compose.onNodeWithText("SeedScreens").performClick()
        waitForText("1 left")
    }

}
