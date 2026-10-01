package com.hatuka.swipeclean

import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.hatuka.swipeclean.core.access.MediaAccessResolver
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Regression test for "4 items could not be moved" on a real phone: WhatsApp media lives under
 * Android/media/com.whatsapp/..., which other apps may not move in place. The seeded
 * "WhatsApp Images" folder (2 photos) must still end up in Pictures/Found after "Move all".
 */
@RunWith(AndroidJUnit4::class)
class WhatsAppMoveTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(GrantPermissionRule.grant(*MediaAccessResolver.permissionsToRequest(Build.VERSION.SDK_INT).toTypedArray()))
        .around(compose)

    private fun waitForText(text: String, timeoutMs: Long = 20_000) = waitForText(compose, text, timeoutMs = timeoutMs)

    /** RELATIVE_PATH → count for the seeded wa_*.jpg files that are visible (not trashed). */
    private fun seedLocations(): Map<String, Int> {
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val result = HashMap<String, Int>()
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf("wa_%"),
            null,
        )?.use { c -> while (c.moveToNext()) result.merge(c.getString(0), 1, Int::plus) }
        return result
    }

    @Test
    fun whatsAppPhotosMoveToFound() {
        resetHomeFilter(compose)
        waitForText("WhatsApp Images")
        val before = seedLocations()
        assertEquals("seeded WhatsApp photos: $before", 2, before.values.sum())

        compose.onNodeWithText("WhatsApp Images").performClick()
        skipTour(compose)
        waitForText("2 left")
        compose.onNodeWithContentDescription("Move to Found").performClick()
        waitForText("1 left")
        compose.onNodeWithContentDescription("Move to Found").performClick()
        waitForText("Move 2 items now")
        compose.onNodeWithText("Move 2 items now").performClick()
        compose.onNodeWithText("Move all (2)").performClick()
        waitForSystemConfirm(compose, "no_whatsapp_write_dialog").click()
        waitForText("Nothing waiting to move", timeoutMs = 60_000)
        takeScreenshot("whatsapp_moved")

        assertEquals(mapOf("Pictures/Found/" to 2), seedLocations())
    }
}
