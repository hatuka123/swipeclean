package com.hatuka.swipeclean.ui.nav

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hatuka.swipeclean.core.access.MediaAccess
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.permissions.MediaAccessMonitor
import com.hatuka.swipeclean.ui.home.HomeRoute
import com.hatuka.swipeclean.ui.onboarding.OnboardingScreen
import com.hatuka.swipeclean.ui.swipe.SwipePlaceholderScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SWIPE = "swipe?bucket={bucket}&filter={filter}"

    /** [bucketId] null means "All photos & videos". */
    fun swipe(bucketId: Long?, filter: MediaFilter) = "swipe?bucket=${bucketId ?: ALL_BUCKETS}&filter=${filter.name}"

    const val ALL_BUCKETS = -1L
}

@Composable
fun AppNavHost(accessMonitor: MediaAccessMonitor) {
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
                    onOpenBucket = { bucket, filter -> nav.navigate(Routes.swipe(bucket, filter)) },
                    onChangeAccess = requestAccess,
                )
            }
        }
        composable(
            Routes.SWIPE,
            arguments = listOf(
                navArgument("bucket") { type = NavType.LongType; defaultValue = Routes.ALL_BUCKETS },
                navArgument("filter") { type = NavType.StringType; defaultValue = MediaFilter.BOTH.name },
            ),
        ) {
            SwipePlaceholderScreen(onBack = { nav.popBackStack() })
        }
    }
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
