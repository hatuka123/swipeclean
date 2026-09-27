package com.hatuka.swipeclean.ui.nav

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hatuka.swipeclean.ui.settings.SettingsRoute
import com.hatuka.swipeclean.ui.stats.StatsRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.hatuka.swipeclean.core.access.MediaAccess
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.SortOrder
import com.hatuka.swipeclean.ui.moves.MovesRoute
import com.hatuka.swipeclean.ui.reviewed.ReviewedRoute
import com.hatuka.swipeclean.ui.bin.BinRoute
import com.hatuka.swipeclean.permissions.MediaAccessMonitor
import com.hatuka.swipeclean.ui.home.HomeRoute
import com.hatuka.swipeclean.ui.onboarding.OnboardingScreen
import com.hatuka.swipeclean.ui.swipe.SwipeRoute

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SWIPE = "swipe?bucket={bucket}&filter={filter}&sort={sort}"
    const val BIN = "bin"
    const val MOVES = "moves"
    const val REVIEWED = "reviewed"
    const val SETTINGS = "settings"
    const val STATS = "stats"

    /** Link in the reminder notification; turned into a [swipe] route by [swipeFromDeepLink]. */
    fun swipeDeepLink(bucketId: Long?, filter: MediaFilter, sort: SortOrder) =
        "swipeclean://swipe?bucket=${bucketId ?: ALL_BUCKETS}&filter=${filter.name}&sort=${sort.name}"

    /** [bucketId] null means "All photos & videos". */
    fun swipe(bucketId: Long?, filter: MediaFilter, sort: SortOrder = SortOrder.OLDEST_FIRST) =
        "swipe?bucket=${bucketId ?: ALL_BUCKETS}&filter=${filter.name}&sort=${sort.name}"

    const val ALL_BUCKETS = -1L

    /** The swipe route for a reminder link from [swipeDeepLink], or null if [uri] is not one. */
    fun swipeFromDeepLink(uri: Uri): String? {
        if (uri.scheme != "swipeclean" || uri.host != "swipe") return null
        val bucket = uri.getQueryParameter("bucket")?.toLongOrNull()?.takeIf { it != ALL_BUCKETS }
        val filter = MediaFilter.entries.find { it.name == uri.getQueryParameter("filter") } ?: MediaFilter.BOTH
        val sort = SortOrder.entries.find { it.name == uri.getQueryParameter("sort") } ?: SortOrder.OLDEST_FIRST
        return swipe(bucket, filter, sort)
    }
}

@Composable
fun AppNavHost(
    accessMonitor: MediaAccessMonitor,
    reminderLinks: Flow<Uri> = emptyFlow(),
    onSwipeSessionEnd: () -> Unit = {},
) {
    val access by accessMonitor.access.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var denied by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        accessMonitor.refresh()
        denied = accessMonitor.access.value == MediaAccess.NONE
    }
    val requestAccess = { permissionLauncher.launch(accessMonitor.permissionsToRequest) }

    // The user may change permissions in system settings while we are in the background.
    LifecycleResumeEffect(Unit) {
        accessMonitor.refresh()
        onPauseOrDispose { }
    }

    val nav = rememberNavController()

    val start = remember { if (access == MediaAccess.NONE) Routes.ONBOARDING else Routes.HOME }

    LaunchedEffect(access) {
        if (access != MediaAccess.NONE && nav.currentDestination?.route == Routes.ONBOARDING) {
            nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
        }
    }

    val onboarding = @Composable {
        OnboardingScreen(denied = denied, onAllow = requestAccess, onOpenSettings = { context.openAppSettings() })
    }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.ONBOARDING) { onboarding() }
        composable(Routes.HOME) {
            // Access can be revoked from system settings; fall back to the explanation screen.
            if (access == MediaAccess.NONE) {
                onboarding()
            } else {
                HomeRoute(
                    onOpenBucket = { bucket, filter, sort -> nav.navigate(Routes.swipe(bucket, filter, sort)) },
                    onChangeAccess = requestAccess,
                    onOpenBin = { nav.navigate(Routes.BIN) },
                    onOpenMoves = { nav.navigate(Routes.MOVES) },
                    onOpenReviewed = { nav.navigate(Routes.REVIEWED) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenStats = { nav.navigate(Routes.STATS) },
                )
            }
        }
        composable(
            Routes.SWIPE,
            arguments = listOf(
                navArgument("bucket") { type = NavType.LongType; defaultValue = Routes.ALL_BUCKETS },
                navArgument("filter") { type = NavType.StringType; defaultValue = MediaFilter.BOTH.name },
                navArgument("sort") { type = NavType.StringType; defaultValue = SortOrder.OLDEST_FIRST.name },
            ),
        ) {
            SwipeRoute(
                onBack = { nav.popBackStack() },
                onOpenBin = { nav.navigate(Routes.BIN) },
                onOpenMoves = { nav.navigate(Routes.MOVES) },
            )
        }
        composable(Routes.BIN) {
            BinRoute(onBack = { nav.popBackStack() })
        }
        composable(Routes.MOVES) {
            MovesRoute(onBack = { nav.popBackStack() })
        }
        composable(Routes.REVIEWED) {
            ReviewedRoute(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(onBack = { nav.popBackStack() })
        }
        composable(Routes.STATS) {
            StatsRoute(onBack = { nav.popBackStack() }, onOpenSettings = { nav.navigate(Routes.SETTINGS) })
        }
    }

    // Back from the swipe screen to Home (arrow, system Back or "back to folders") is the natural
    // break where an ad may be shown later.
    val sessionEnd by rememberUpdatedState(onSwipeSessionEnd)
    DisposableEffect(nav) {
        var previous: String? = null
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (previous == Routes.SWIPE && destination.route == Routes.HOME) sessionEnd()
            previous = destination.route
        }
        nav.addOnDestinationChangedListener(listener)
        onDispose { nav.removeOnDestinationChangedListener(listener) }
    }

    // A tapped reminder opens the swipe screen on top of Home, like tapping a folder, so Back returns
    // to Home. (NavController's own deep-link handling would leave the swipe screen alone in the
    // back stack, or restart the whole task when the intent carries NEW_TASK.)
    LaunchedEffect(nav) {
        reminderLinks.collect { uri ->
            val route = Routes.swipeFromDeepLink(uri) ?: return@collect
            if (accessMonitor.access.value == MediaAccess.NONE) return@collect
            nav.navigate(route) { popUpTo(Routes.HOME) }
        }
    }
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
