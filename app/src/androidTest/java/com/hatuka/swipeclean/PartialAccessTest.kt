package com.hatuka.swipeclean

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hatuka.swipeclean.core.access.MediaAccess
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android 14+: the CI script revokes full media access and grants only
 * READ_MEDIA_VISUAL_USER_SELECTED (the state after the user picks "Select photos").
 */
@RunWith(AndroidJUnit4::class)
class PartialAccessTest {

    @Before
    fun requirePartialAccess() = check(currentAccess() == MediaAccess.PARTIAL) { "Run with only user-selected access (see emulator-tests.sh)" }

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun showsLimitedAccessBannerWithWayToExpand() {
        compose.waitUntil(10_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasText("Limited access")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Select more or allow all").assertExists()
        takeScreenshot("home_partial")
    }
}
