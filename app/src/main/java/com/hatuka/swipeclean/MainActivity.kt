package com.hatuka.swipeclean

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hatuka.swipeclean.permissions.MediaAccessMonitor
import com.hatuka.swipeclean.ui.nav.AppNavHost
import com.hatuka.swipeclean.ui.theme.SwipeCleanTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var accessMonitor: MediaAccessMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SwipeCleanTheme {
                AppNavHost(accessMonitor)
            }
        }
    }
}
