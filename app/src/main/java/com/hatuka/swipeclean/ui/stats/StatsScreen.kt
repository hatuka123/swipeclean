package com.hatuka.swipeclean.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.core.plan.CleanupPlan
import com.hatuka.swipeclean.core.plan.Progress
import com.hatuka.swipeclean.core.plan.ProgressCalculator
import com.hatuka.swipeclean.data.review.ProgressRepository
import com.hatuka.swipeclean.data.settings.SettingsRepository
import com.hatuka.swipeclean.ui.common.formatSize
import com.hatuka.swipeclean.ui.theme.LocalActionColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import javax.inject.Inject

val EMPTY_PROGRESS = ProgressCalculator.progress(emptyList(), emptySet(), CleanupPlan.DEFAULT_QUOTA, LocalDate.now())

@HiltViewModel
class StatsViewModel @Inject constructor(progress: ProgressRepository, settings: SettingsRepository) : ViewModel() {
    val progress: StateFlow<Progress> = progress.progress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EMPTY_PROGRESS)
    val plan: StateFlow<CleanupPlan> = settings.plan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CleanupPlan())
}

@Composable
fun StatsRoute(onBack: () -> Unit, onOpenSettings: () -> Unit = {}, viewModel: StatsViewModel = hiltViewModel()) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    StatsContent(progress, plan, onBack, onOpenSettings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsContent(progress: Progress, plan: CleanupPlan, onBack: () -> Unit, onOpenSettings: () -> Unit) {
    val nf = NumberFormat.getIntegerInstance()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (plan.enabled) {
                TodayCard(progress)
            } else {
                Text(stringResource(R.string.stats_plan_off), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.stats_set_up_plan)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.stats_freed), formatSize(progress.totalBytesFreed), Modifier.weight(1f))
                StatTile(stringResource(R.string.stats_streak), nf.format(progress.streak), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.stats_reviewed_total), nf.format(progress.totalReviewed), Modifier.weight(1f))
                StatTile(stringResource(R.string.stats_deleted_total), nf.format(progress.totalDeleted), Modifier.weight(1f))
            }
            Text(stringResource(R.string.stats_last_days), style = MaterialTheme.typography.titleMedium)
            RecentChart(progress, if (plan.enabled) plan.quota else 0)
        }
    }
}

@Composable
fun TodayCard(progress: Progress, modifier: Modifier = Modifier) {
    val nf = NumberFormat.getIntegerInstance()
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (progress.quotaMet) stringResource(R.string.today_goal_reached) else stringResource(R.string.today_progress, nf.format(progress.todayReviewed), nf.format(progress.quota)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            LinearProgressIndicator(progress = { progress.todayFraction }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("🔥 " + pluralStringResource(R.plurals.streak_days, progress.streak, nf.format(progress.streak)), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.stats_freed_total, formatSize(progress.totalBytesFreed)), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Bars of reviewed items per day, oldest to newest (right to left in RTL), with the goal as a dashed line. */
@Composable
private fun RecentChart(progress: Progress, quota: Int) {
    val barColor = MaterialTheme.colorScheme.primary
    val metColor = LocalActionColors.current.keep
    val lineColor = MaterialTheme.colorScheme.outline
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val days = progress.recent
    val max = maxOf(quota, days.maxOfOrNull { it.reviewed } ?: 0, 1)
    val locale = LocalConfiguration.current.locales[0]
    Column {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val slot = size.width / days.size
            val barWidth = slot * 0.6f
            days.forEachIndexed { index, day ->
                val position = if (rtl) days.size - 1 - index else index
                val h = size.height * day.reviewed / max
                drawRect(
                    color = if (quota > 0 && day.reviewed >= quota) metColor else barColor,
                    topLeft = Offset(position * slot + (slot - barWidth) / 2, size.height - h),
                    size = Size(barWidth, h),
                )
            }
            if (quota > 0) {
                val y = size.height - size.height * quota / max
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            days.forEach { day ->
                Text(
                    LocalDate.ofEpochDay(day.epochDay).dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
