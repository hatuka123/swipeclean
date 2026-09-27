package com.hatuka.swipeclean

import android.os.Build
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
 * Daily plan end-to-end: turn the plan on in settings (a reminder gets scheduled), send a test
 * reminder, open it from the real notification shade and land on the swipe screen.
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

        compose.onNodeWithText("Send a test reminder now").performScrollTo().performClick()
        waitForText(compose, "Test reminder sent")

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openNotification()
        val notification = device.wait(Until.findObject(By.text("Time to clean!")), 15_000)
        checkNotNull(notification) { "Reminder notification not found in the shade" }
        takeScreenshot("notification")
        notification.click()

        compose.waitUntil(20_000) {
            runCatching { compose.onAllNodes(hasTestTag(SWIPE_CARD_TAG)).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
        waitForText(compose, "Today:", substring = true)
        takeScreenshot("swipe_from_reminder")

        // Leave the plan off for other tests.
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Statistics").performClick()
        waitForText(compose, "Last 14 days")
        takeScreenshot("stats")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Daily plan & settings").performClick()
        compose.onNodeWithTag(PLAN_SWITCH_TAG).performClick()
    }
}
