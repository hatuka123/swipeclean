package com.hatuka.swipeclean

import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.hatuka.swipeclean.core.access.MediaAccessResolver
import com.hatuka.swipeclean.ui.common.FOLDER_PICKER_TAG
import com.hatuka.swipeclean.ui.common.NEW_FOLDER_FIELD_TAG
import com.hatuka.swipeclean.ui.moves.MOVE_ITEM_TAG
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * End-to-end on the seeded Download folder (3 photos): swipe-up to the default folder, choose a
 * new folder for the next one, keep the last, apply both moves through the real system write
 * dialog, check MediaStore (what every gallery app sees), then reset the "Found" folder.
 */
@RunWith(AndroidJUnit4::class)
class MoveFlowTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(GrantPermissionRule.grant(*MediaAccessResolver.permissionsToRequest(Build.VERSION.SDK_INT).toTypedArray()))
        .around(compose)

    private fun waitForText(text: String, substring: Boolean = false, timeoutMs: Long = 20_000) =
        waitForText(compose, text, substring, timeoutMs)

    /** Where the seeded download_*.jpg files are now, as RELATIVE_PATH → count. */
    private fun downloadSeedLocations(): Map<String, Int> {
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val result = HashMap<String, Int>()
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf("download_%"),
            null,
        )?.use { c -> while (c.moveToNext()) result.merge(c.getString(0), 1, Int::plus) }
        return result
    }

    @Test
    fun swipeUpPickFolderApplyAndReset() {
        resetHomeFilter(compose)
        waitForText("Download")
        compose.onNodeWithText("Download").performClick()
        skipTour(compose)
        waitForText("3 left")

        // 1) Move button = default folder (Pictures/Found).
        compose.onNodeWithContentDescription("Move to Found").performClick()
        waitForText("2 left")

        // 2) Choose a new folder for the next item.
        compose.onNodeWithContentDescription("Choose folder").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(FOLDER_PICKER_TAG).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(NEW_FOLDER_FIELD_TAG).performTextInput("SwipeTest")
        takeScreenshot("folder_picker")
        compose.onNodeWithText("Move").performClick()
        waitForText("1 left")

        // 3) Keep the last one; the finished screen offers to apply the moves.
        compose.onNodeWithContentDescription("Keep").performClick()
        waitForText("Move 2 items now")
        compose.onNodeWithText("Move 2 items now").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(MOVE_ITEM_TAG).fetchSemanticsNodes().size == 2 }
        Thread.sleep(1_000)
        takeScreenshot("moves")
        assertEquals(mapOf("Download/" to 3), downloadSeedLocations())

        // 4) One system write request for both, then MediaStore shows the new folders.
        compose.onNodeWithText("Move all (2)").performClick()
        val confirm = waitForSystemConfirm(compose, "no_write_dialog")
        takeScreenshot("write_dialog")
        confirm.click()
        waitForText("Nothing waiting to move", timeoutMs = 60_000)
        assertEquals(mapOf("Download/" to 1, "Pictures/Found/" to 1, "Pictures/SwipeTest/" to 1), downloadSeedLocations())

        // 5) The new folders appear on Home; reset "Found" and review it again.
        compose.onNodeWithContentDescription("Back").performClick() // moves → swipe
        compose.onNodeWithContentDescription("Back").performClick() // swipe → home
        waitForText("SwipeTest")
        waitForText("Found")
        takeScreenshot("home_after_move")
        compose.onNodeWithText("Found").performTouchInput { longClick() }
        waitForText("Show again")
        compose.onNodeWithText("Show again").performClick()
        compose.onNodeWithText("Found").performClick()
        waitForText("1 left")
    }
}
