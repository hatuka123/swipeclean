package com.hatuka.swipeclean

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.hatuka.swipeclean.permissions.MediaAccessMonitor
import com.hatuka.swipeclean.reminders.ReminderScheduler
import com.hatuka.swipeclean.ui.nav.AppNavHost
import com.hatuka.swipeclean.ui.theme.SwipeCleanTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var accessMonitor: MediaAccessMonitor
    @Inject lateinit var reminderScheduler: ReminderScheduler

    private val newIntents = Channel<Intent>(Channel.BUFFERED)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // singleTask: a tapped reminder reuses this window (onNewIntent) instead of opening a second copy.
        // Keeps the reminder scheduled (e.g. after an app update or a changed plan).
        lifecycleScope.launch { reminderScheduler.reschedule() }
        setContent {
            SwipeCleanTheme {
                AppNavHost(accessMonitor, newIntents.receiveAsFlow())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        newIntents.trySend(intent)
    }
}
