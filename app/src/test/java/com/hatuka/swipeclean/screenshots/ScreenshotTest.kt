package com.hatuka.swipeclean.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.hatuka.swipeclean.core.access.MediaAccess
import com.hatuka.swipeclean.core.media.BucketList
import com.hatuka.swipeclean.core.media.BucketSummary
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.MediaType
import androidx.compose.material3.SnackbarHostState
import com.hatuka.swipeclean.core.media.MediaRow
import com.hatuka.swipeclean.core.review.DecisionState
import com.hatuka.swipeclean.core.review.SessionCounters
import com.hatuka.swipeclean.data.db.BinSummary
import com.hatuka.swipeclean.data.db.DecisionEntity
import com.hatuka.swipeclean.data.settings.DeleteMode
import com.hatuka.swipeclean.ui.bin.BinContent
import com.hatuka.swipeclean.ui.bin.BinUiState
import com.hatuka.swipeclean.ui.home.HomeContent
import com.hatuka.swipeclean.ui.home.HomeUiState
import com.hatuka.swipeclean.ui.onboarding.OnboardingScreen
import com.hatuka.swipeclean.ui.swipe.SwipeContent
import com.hatuka.swipeclean.ui.swipe.SwipeUiState
import com.hatuka.swipeclean.ui.theme.SwipeCleanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the main screens in light/dark × English/Hebrew and writes PNGs to
 * app/build/screens, which CI uploads as the `screens` artifact for review.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class ScreenshotTest(private val variant: String, private val qualifiers: String) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = listOf(
            arrayOf("en_light", "en-ldltr-notnight"),
            arrayOf("en_dark", "en-ldltr-night"),
            arrayOf("he_light", "iw-ldrtl-notnight"),
            arrayOf("he_dark", "iw-ldrtl-night"),
        )
    }

    @get:Rule
    val compose = createEmptyComposeRule()

    private val buckets = BucketList(
        all = BucketSummary(null, "", null, 18_342, 41_200_000_000, null, null),
        buckets = listOf(
            BucketSummary(1, "Camera", "DCIM/Camera/", 12_904, 35_100_000_000, null, MediaType.IMAGE),
            BucketSummary(2, "WhatsApp Images", "Pictures/WhatsApp Images/", 3_870, 1_900_000_000, null, MediaType.IMAGE),
            BucketSummary(3, "Screenshots", "Pictures/Screenshots/", 1_213, 820_000_000, null, MediaType.IMAGE),
            BucketSummary(4, "WhatsApp Video", "Movies/WhatsApp Video/", 301, 3_300_000_000, null, MediaType.VIDEO),
            BucketSummary(5, "Download", "Download/", 54, 80_000_000, null, MediaType.IMAGE),
        ),
    )

    private fun shoot(name: String, content: @Composable () -> Unit) {
        RuntimeEnvironment.setQualifiers("+$qualifiers")
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent { SwipeCleanTheme(dynamicColor = false) { content() } }
            }
            compose.waitForIdle()
            scenario.onActivity { activity ->
                // Draw the window ourselves: captureToImage() waits for a frame Robolectric never sends.
                val view = activity.window.decorView
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                val dir = File("build/screens").apply { mkdirs() }
                File(dir, "${name}_$variant.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }

    @Test
    fun onboarding() = shoot("onboarding") {
        OnboardingScreen(denied = false, onAllow = {}, onOpenSettings = {})
    }

    @Test
    fun onboardingDenied() = shoot("onboarding_denied") {
        OnboardingScreen(denied = true, onAllow = {}, onOpenSettings = {})
    }

    @Test
    fun home() = shoot("home") {
        HomeContent(
            state = HomeUiState(MediaAccess.FULL, MediaFilter.BOTH, buckets, loading = false),
            binCount = 37,
            onOpenBin = {},
            coverUri = { _, _ -> null },
            onFilter = {},
            onOpenBucket = {},
            onChangeAccess = {},
        )
    }

    @Test
    fun homePartialAccess() = shoot("home_partial") {
        HomeContent(
            state = HomeUiState(
                MediaAccess.PARTIAL,
                MediaFilter.BOTH,
                BucketList(buckets.all.copy(count = 12, sizeBytes = 48_000_000), listOf(buckets.buckets[0].copy(count = 12, sizeBytes = 48_000_000))),
                loading = false,
            ),
            binCount = 37,
            onOpenBin = {},
            coverUri = { _, _ -> null },
            onFilter = {},
            onOpenBucket = {},
            onChangeAccess = {},
        )
    }

    private val photo = MediaRow(7, 1, "Camera", "DCIM/Camera/", 3_400_000, MediaType.IMAGE, 1_690_000_000_000)

    private fun swipeState(current: MediaRow?) = SwipeUiState(
        loading = false,
        sourceName = "Camera",
        current = current,
        upcoming = listOf(photo.copy(id = 8)),
        remaining = if (current == null) 0 else 12_893,
        counters = SessionCounters(reviewed = 11, markedForDeletion = 4, bytesToFree = 18_500_000),
        canUndo = current != null,
    )

    @Test
    fun swipe() = shoot("swipe") {
        SwipeContent(
            state = swipeState(photo),
            bin = BinSummary(37, 210_000_000),
            uriOf = { null },
            onKeep = {},
            onMarkForDeletion = {},
            onUndo = {},
            onUnavailable = {},
            onBack = {},
            onOpenBin = {},
        )
    }

    @Test
    fun swipeFinished() = shoot("swipe_finished") {
        SwipeContent(
            state = swipeState(null),
            bin = BinSummary(37, 210_000_000),
            uriOf = { null },
            onKeep = {},
            onMarkForDeletion = {},
            onUndo = {},
            onUnavailable = {},
            onBack = {},
            onOpenBin = {},
        )
    }

    private val binItems = (1L..14L).map {
        DecisionEntity(it, 1, "Camera", "DCIM/Camera/", it * 1_300_000, it % 5 == 0L, it, DecisionState.DELETE_PENDING, null, it)
    }

    private fun bin(state: BinUiState, name: String) = shoot(name) {
        BinContent(
            state = state,
            snackbar = SnackbarHostState(),
            uriOf = { null },
            onBack = {},
            onToggle = {},
            onClearSelection = {},
            onKeepSelected = {},
            onKeep = {},
            onDeleteAll = {},
            onDeleteMode = {},
        )
    }

    @Test
    fun binWithItems() = bin(BinUiState(binItems, BinSummary(14, binItems.sumOf { it.sizeBytes }), loading = false), "bin")

    @Test
    fun binSelecting() = bin(
        BinUiState(binItems, BinSummary(14, binItems.sumOf { it.sizeBytes }), selection = setOf(2L, 3L), deleteMode = DeleteMode.PERMANENT, loading = false),
        "bin_selecting",
    )

    @Test
    fun binEmpty() = bin(BinUiState(loading = false), "bin_empty")
}
