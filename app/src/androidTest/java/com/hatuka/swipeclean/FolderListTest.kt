package com.hatuka.swipeclean

import android.os.Build
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.hatuka.swipeclean.core.access.MediaAccessResolver
import com.hatuka.swipeclean.ui.home.FOLDER_ROW_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Needs the seeded media pushed by .github/scripts/emulator-tests.sh:
 * DCIM/SeedCamera (10 photos + 2 videos), Pictures/SeedScreens (5 photos), Download (3 photos).
 */
@RunWith(AndroidJUnit4::class)
class FolderListTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(GrantPermissionRule.grant(*MediaAccessResolver.permissionsToRequest(Build.VERSION.SDK_INT).toTypedArray()))
        .around(compose)

    private fun waitForText(text: String) = compose.waitUntil(20_000) {
        compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun showsSeededFoldersWithCounts() {
        resetHomeFilter(compose)
        waitForText("SeedCamera")
        compose.onAllNodesWithTag(FOLDER_ROW_TAG + "all").fetchSemanticsNodes().single()
        compose.onNodeWithText("All photos & videos").assertExists()
        compose.onNodeWithText("12 items", substring = true).assertExists()
        compose.onNodeWithText("SeedScreens").assertExists()
        compose.onNodeWithText("5 items", substring = true).assertExists()
        Thread.sleep(THUMBNAIL_SETTLE_MS) // thumbnails load asynchronously; slow on CI emulators
        takeScreenshot("home")
    }

    @Test
    fun videoFilterHidesFoldersWithoutVideos() {
        resetHomeFilter(compose)
        waitForText("SeedScreens")
        compose.onNodeWithText("Videos").performClick()
        // Wait for the reloaded list (a loading state briefly shows no folders at all).
        compose.waitUntil(20_000) { !hasNodeWithText(compose, "SeedScreens") && hasNodeWithText(compose, "SeedCamera") }
        Thread.sleep(THUMBNAIL_SETTLE_MS)
        takeScreenshot("home_videos")
        resetHomeFilter(compose)
    }

    @Test
    fun openingAFolderNavigatesToSwipe() {
        resetHomeFilter(compose)
        waitForText("SeedCamera")
        clickText(compose, "SeedCamera")
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasText("12 left")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val THUMBNAIL_SETTLE_MS = 3_000L
    }
}
