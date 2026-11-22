package com.hatuka.income

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hatuka.income.data.Store
import com.hatuka.income.ui.AppTheme
import com.hatuka.income.ui.Bg
import com.hatuka.income.ui.Card
import com.hatuka.income.ui.CrashBanner
import com.hatuka.income.ui.EntriesScreen
import com.hatuka.income.ui.Green
import com.hatuka.income.ui.GreenSoft
import com.hatuka.income.ui.JobsScreen
import com.hatuka.income.ui.SummaryScreen
import com.hatuka.income.ui.TaxScreen
import com.hatuka.income.ui.TextDim
import com.hatuka.income.ui.TextMain

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            AppTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppRoot()
                }
            }
        }
    }
}

private val tabs = listOf("💼" to "עבודות", "📝" to "רישום", "📊" to "סיכום", "🧾" to "מס")

@Composable
private fun AppRoot() {
    // Start on "רישום" once there is a workplace – that is what she will do most.
    var tab by rememberSaveable { mutableIntStateOf(if (Store.jobs.isEmpty()) 0 else 1) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
            .imePadding()
    ) {
        Text(
            "ההכנסות שלי",
            color = Green, fontSize = 24.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        CrashBanner()
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> JobsScreen()
                1 -> EntriesScreen(onGoJobs = { tab = 0 })
                2 -> SummaryScreen()
                else -> TaxScreen()
            }
        }
        NavigationBar(containerColor = Card) {
            tabs.forEachIndexed { i, (icon, label) ->
                NavigationBarItem(
                    selected = tab == i,
                    onClick = { tab = i },
                    icon = { Text(icon, fontSize = 22.sp) },
                    label = { Text(label, fontSize = 13.sp, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = GreenSoft,
                        selectedTextColor = TextMain,
                        unselectedTextColor = TextDim,
                    ),
                )
            }
        }
    }
}
