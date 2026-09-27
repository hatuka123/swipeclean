package com.hatuka.swipeclean

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.hatuka.swipeclean.core.access.MediaAccess
import com.hatuka.swipeclean.ui.home.FOLDER_ROW_TAG
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * Only meaningful after the CI script revoked media access (it runs this class on its own): checks the onboarding screen,
 * taps "Allow access" and accepts the real system permission dialog.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingTest {

    @Before
    fun requireNoAccess() = check(currentAccess() == MediaAccess.NONE) { "Run with media permissions revoked (see emulator-tests.sh)" }

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun grantingAccessFromOnboardingOpensFolderList() {
        compose.onNodeWithText("Nothing is deleted without your OK").assertExists()
        takeScreenshot("onboarding")
        compose.onNodeWithText("Allow access").performClick()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // "Allow" on API 30–33, "Allow all" on 34+; match the button by id or text.
        val allow = device.wait(
            Until.findObject(
                By.res(Pattern.compile("com\\.android\\.permissioncontroller:id/permission_allow(_all)?_button")),
            ),
            10_000,
        ) ?: device.wait(Until.findObject(By.text(Pattern.compile("(?i)allow( all)?"))), 5_000)
        checkNotNull(allow) { "System permission dialog did not appear" }
        takeScreenshot("permission_dialog")
        allow.click()

        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag(FOLDER_ROW_TAG + "all").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
