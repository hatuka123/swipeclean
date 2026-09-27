package com.hatuka.swipeclean

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.hatuka.swipeclean.ads.AdGate
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
    @Inject lateinit var adGate: AdGate

    private val reminderLinks = Channel<Uri>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // singleTask: a tapped reminder reuses this window (onNewIntent) instead of opening a second copy.
        // A recreated activity (rotation, process restore) already shows where the link led.
        if (savedInstanceState == null) intent.data?.let { reminderLinks.trySend(it) }
        // Keeps the reminder scheduled (e.g. after an app update or a changed plan).
        lifecycleScope.launch { reminderScheduler.reschedule() }
        setContent {
            SwipeCleanTheme {
                AppNavHost(
                    accessMonitor,
                    reminderLinks.receiveAsFlow(),
                    onSwipeSessionEnd = { lifecycleScope.launch { adGate.onNaturalBreak(this@MainActivity) } },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // No setIntent(): the link goes to the navigation through [reminderLinks], and
        // ActivityScenario in the instrumented tests tracks the activity by its original intent.
        intent.data?.let { reminderLinks.trySend(it) }
    }
}
