package com.hatuka.swipeclean

import android.os.Build
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.hatuka.swipeclean.core.access.MediaAccessResolver
import com.hatuka.swipeclean.reminders.ReminderScheduler
import com.hatuka.swipeclean.ui.settings.PLAN_SWITCH_TAG
import com.hatuka.swipeclean.ui.swipe.SWIPE_CARD_TAG
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Daily plan end-to-end: turn the plan on in settings (a reminder gets scheduled), look at the
 * statistics, then send a test reminder, open it from the real notification shade and land on the
 * swipe screen.
 */
@RunWith(AndroidJUnit4::class)
class ReminderFlowTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    private val permissions = MediaAccessResolver.permissionsToRequest(Build.VERSION.SDK_INT) +
        if (Build.VERSION.SDK_INT >= 33) listOf("android.permission.POST_NOTIFICATIONS") else emptyList()

    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(GrantPermissionRule.grant(*permissions.toTypedArray()))
        .around(compose)

    @Test
    fun planScheduleNotificationOpensSwipe() {
        resetHomeFilter(compose)
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Daily plan & settings").performClick()
        waitForText(compose, "Remind me every day")
        compose.onNodeWithTag(PLAN_SWITCH_TAG).performClick()
        waitForText(compose, "Next reminder", substring = true)
        takeScreenshot("settings_plan")

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scheduled = WorkManager.getInstance(context).getWorkInfosForUniqueWork(ReminderScheduler.WORK_NAME).get()
        assertTrue("reminder not scheduled: $scheduled", scheduled.any { it.state == WorkInfo.State.ENQUEUED })

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Statistics").performClick()
        waitForText(compose, "Last 14 days")
        takeScreenshot("stats")
        compose.onNodeWithContentDescription("Back").performClick()

        // Turn the plan off again (other tests expect it off); the test reminder works either way.
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Daily plan & settings").performClick()
        waitForText(compose, "Remind me every day")
        compose.onNodeWithTag(PLAN_SWITCH_TAG).performClick()
        compose.onNodeWithText("Send a test reminder now").performScrollTo().performClick()
        waitForText(compose, "Test reminder sent")

        // Last step on purpose: after the tap from the shade, Compose touch injection is unreliable on
        // the emulators ("Failed to inject touch input"), so only assertions and key presses follow.
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // The shade can open while the heads-up is still animating and miss the new entry; retry.
        val notification = (1..3).firstNotNullOfOrNull {
            device.openNotification()
            device.wait(Until.findObject(By.text("Time to clean!")), 8_000) ?: run {
                device.pressBack()
                device.waitForIdle()
                null
            }
        }
        checkNotNull(notification) { "Reminder notification not found in the shade" }
        takeScreenshot("notification")
        notification.click()

        compose.waitUntil(20_000) {
            runCatching { compose.onAllNodes(hasTestTag(SWIPE_CARD_TAG)).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
        takeScreenshot("swipe_from_reminder")

        // Back from a reminder returns to Home (a key press: Compose touch input is what fails here).
        device.pressBack()
        compose.waitUntil(10_000) {
            runCatching { compose.onAllNodesWithContentDescription("More options").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
        takeScreenshot("home_after_reminder")
    }
}
