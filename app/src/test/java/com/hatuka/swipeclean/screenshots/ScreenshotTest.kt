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
import com.hatuka.swipeclean.ui.bin.DecisionEditorCallbacks
import com.hatuka.swipeclean.core.plan.CleanupPlan
import com.hatuka.swipeclean.core.plan.DayStats
import com.hatuka.swipeclean.core.plan.PlanSlot
import com.hatuka.swipeclean.core.plan.PlanSource
import com.hatuka.swipeclean.core.plan.ProgressCalculator
import com.hatuka.swipeclean.ui.settings.SettingsContent
import com.hatuka.swipeclean.ui.settings.SourceOption
import com.hatuka.swipeclean.ui.stats.StatsContent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import com.hatuka.swipeclean.ui.moves.MovesContent
import com.hatuka.swipeclean.ui.moves.MovesUiState
import com.hatuka.swipeclean.ui.reviewed.ReviewedContent
import com.hatuka.swipeclean.ui.reviewed.ReviewedUiState
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
            movesCount = 6,
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
            editor = DecisionEditorCallbacks("Pictures/Found/", { emptyList() }, { _, _, _, _ -> }),
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

    private val editor = DecisionEditorCallbacks("Pictures/Found/", { emptyList() }, { _, _, _, _ -> })

    private val moveItems = (1L..7L).map {
        DecisionEntity(it, 1, "Camera", "DCIM/Camera/", it * 900_000, it == 3L, it, DecisionState.MOVE_PENDING, if (it < 5) "Pictures/Found/" else "Pictures/Trip 2024/", it)
    }

    @Test
    fun moves() = shoot("moves") {
        MovesContent(MovesUiState(moveItems, loading = false), SnackbarHostState(), { null }, {}, {}, editor)
    }

    @Test
    fun reviewed() = shoot("reviewed") {
        val items = moveItems.map { it.copy(state = if (it.mediaId % 2 == 0L) DecisionState.MOVED else DecisionState.KEEP) }
        ReviewedContent(
            ReviewedUiState(items, folders = listOf("Camera", "Found", "Trip 2024"), folder = null, loading = false),
            { null },
            {},
            {},
            editor,
        )
    }

    private val samplePlan = CleanupPlan(
        enabled = true,
        slots = listOf(
            PlanSlot(setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY), LocalTime.of(20, 30)),
            PlanSlot(setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY), LocalTime.of(10, 0)),
        ),
        quota = 50,
        source = PlanSource(1, "Camera"),
    )

    private val sampleProgress = ProgressCalculator.progress(
        stats = (0L..13L).map { back ->
            DayStats(LocalDate.of(2026, 9, 27).minusDays(back).toEpochDay(), reviewed = listOf(55, 20, 60, 70, 0, 52, 50, 10, 80, 50, 51, 0, 65, 30)[back.toInt()], deleted = 7, bytesFreed = 42_000_000)
        },
        plannedDays = DayOfWeek.entries.toSet(),
        quota = 50,
        today = LocalDate.of(2026, 9, 27),
    )

    @Test
    fun settings() = shoot("settings") {
        SettingsContent(
            plan = samplePlan,
            next = ZonedDateTime.of(2026, 9, 27, 20, 30, 0, 0, ZoneOffset.UTC),
            canNotify = true,
            sources = listOf(SourceOption(null, null), SourceOption(1, "Camera")),
            deleteMode = DeleteMode.TRASH,
            defaultTarget = "Pictures/Found/",
            snackbar = SnackbarHostState(),
            onBack = {},
            onEnabled = {},
            onAllowNotifications = {},
            onSlot = { _, _ -> },
            onAddSlot = {},
            onRemoveSlot = {},
            onQuota = {},
            onSource = {},
            onSourceFilter = {},
            onTestReminder = {},
            onDeleteMode = {},
            onResetDefaultFolder = {},
        )
    }

    @Test
    fun stats() = shoot("stats") {
        StatsContent(sampleProgress, samplePlan, {}, {})
    }

    @Test
    fun homeWithPlan() = shoot("home_plan") {
        HomeContent(
            state = HomeUiState(MediaAccess.FULL, MediaFilter.BOTH, buckets, loading = false),
            binCount = 3,
            onOpenBin = {},
            coverUri = { _, _ -> null },
            onFilter = {},
            onOpenBucket = {},
            onChangeAccess = {},
            progress = sampleProgress,
            planEnabled = true,
        )
    }
}
