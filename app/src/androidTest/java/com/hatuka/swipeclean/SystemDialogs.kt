package com.hatuka.swipeclean

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

/**
 * Waits for MediaProvider's confirmation dialog (trash/delete/write request) and returns its
 * positive button. The Compose test clock only advances when the test syncs with Compose, so
 * this keeps calling waitForIdle(); otherwise the effect that launches the request never runs.
 */
fun waitForSystemConfirm(compose: ComposeTestRule, screenshotName: String = "no_system_dialog"): UiObject2 {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val positive = By.res("android:id/button1")
    val byText = By.text(Pattern.compile("(?i)(allow|move to trash|delete)"))
    val anrWait = By.res("android:id/aerr_wait")
    val deadline = System.currentTimeMillis() + 30_000
    while (System.currentTimeMillis() < deadline) {
        compose.waitForIdle()
        device.findObject(anrWait)?.click()
        device.wait(Until.findObject(positive), 2_000)?.let { return it }
        device.findObject(byText)?.let { return it }
    }
    takeScreenshot(screenshotName)
    val screen = runCatching { compose.onRoot().printToString() }.getOrDefault("")
    error("System confirmation dialog did not appear. Screen: $screen")
}

/** The photo/video filter is saved in settings; tests start from "Both" to be independent. */
fun resetHomeFilter(compose: ComposeTestRule) {
    compose.waitUntil(20_000) { compose.onAllNodes(hasText("Both")).fetchSemanticsNodes().isNotEmpty() }
    compose.onNodeWithText("Both").performClick()
    compose.waitForIdle()
}

/**
 * Clicks a text node, retrying when the list is still recomposing (e.g. right after a filter
 * change), which otherwise makes touch injection fail intermittently on slow emulators.
 */
fun clickText(compose: ComposeTestRule, text: String, attempts: Int = 5) {
    repeat(attempts) { attempt ->
        compose.waitForIdle()
        val result = runCatching { compose.onNodeWithText(text).performClick() }
        if (result.isSuccess) return
        if (attempt == attempts - 1) throw result.exceptionOrNull()!!
        Thread.sleep(500)
    }
}
