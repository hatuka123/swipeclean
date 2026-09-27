package com.hatuka.swipeclean

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.hatuka.swipeclean.core.access.MediaAccess
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
        compose.onNodeWithText("Allow access").performScrollTo().performClick()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // "Allow" on API 30–33, "Allow all" on 34+; match the button by id or text.
        // AOSP images use com.android.permissioncontroller, Google images com.google.android.*.
        val allowButton = By.res(Pattern.compile("com\\.(google\\.)?android\\.permissioncontroller:id/permission_allow(_all)?_button"))
        // Slow CI emulators sometimes show "System UI isn't responding" on top; answer "Wait".
        val anrWait = By.res("android:id/aerr_wait")
        var allow: UiObject2? = null
        val deadline = System.currentTimeMillis() + 60_000
        while (allow == null && System.currentTimeMillis() < deadline) {
            device.findObject(anrWait)?.click()
            allow = device.wait(Until.findObject(allowButton), 3_000)
        }
        if (allow == null) {
            // Leave a window dump next to the screenshots to see what was on screen instead.
            val dir = java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "shots")
            dir.mkdirs()
            device.dumpWindowHierarchy(java.io.File(dir, "no_dialog_api${android.os.Build.VERSION.SDK_INT}.xml"))
            takeScreenshot("no_dialog")
        }
        checkNotNull(allow) { "System permission dialog did not appear" }
        takeScreenshot("permission_dialog")
        allow.click()

        // The Home screen's filter chips appear once access is granted (with or without media).
        compose.waitUntil(30_000) {
            compose.onAllNodes(hasText("Both")).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
