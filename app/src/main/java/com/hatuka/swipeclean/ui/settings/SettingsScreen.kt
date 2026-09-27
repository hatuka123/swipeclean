package com.hatuka.swipeclean.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.move.TargetPathRules
import com.hatuka.swipeclean.core.plan.CleanupPlan
import com.hatuka.swipeclean.core.plan.PlanSlot
import com.hatuka.swipeclean.data.settings.DeleteMode
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Date
import kotlin.math.roundToInt

const val PLAN_SWITCH_TAG = "plan_switch"

@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val next by viewModel.nextReminder.collectAsStateWithLifecycle()
    val canNotify by viewModel.canNotify.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    val deleteMode by viewModel.deleteMode.collectAsStateWithLifecycle()
    val defaultTarget by viewModel.defaultTarget.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshNotificationState()
    }
    fun allowNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshNotificationState()
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) {
        viewModel.testResults.collect { posted ->
            snackbar.showSnackbar(resources.getString(if (posted) R.string.plan_test_sent else R.string.plan_test_failed))
        }
    }

    SettingsContent(
        plan = plan,
        next = next,
        canNotify = canNotify,
        sources = sources,
        deleteMode = deleteMode,
        defaultTarget = defaultTarget,
        snackbar = snackbar,
        onBack = onBack,
        onEnabled = { enabled ->
            viewModel.setEnabled(enabled)
            if (enabled && !canNotify) allowNotifications()
        },
        onAllowNotifications = ::allowNotifications,
        onSlot = viewModel::setSlot,
        onAddSlot = viewModel::addSlot,
        onRemoveSlot = viewModel::removeSlot,
        onQuota = viewModel::setQuota,
        onSource = viewModel::setSource,
        onSourceFilter = viewModel::setSourceFilter,
        onTestReminder = viewModel::sendTestReminder,
        onDeleteMode = viewModel::setDeleteMode,
        onResetDefaultFolder = viewModel::resetDefaultFolder,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    plan: CleanupPlan,
    next: ZonedDateTime?,
    canNotify: Boolean,
    sources: List<SourceOption>,
    deleteMode: DeleteMode,
    defaultTarget: String,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onEnabled: (Boolean) -> Unit,
    onAllowNotifications: () -> Unit,
    onSlot: (Int, PlanSlot) -> Unit,
    onAddSlot: () -> Unit,
    onRemoveSlot: (Int) -> Unit,
    onQuota: (Int) -> Unit,
    onSource: (SourceOption) -> Unit,
    onSourceFilter: (MediaFilter) -> Unit,
    onTestReminder: () -> Unit,
    onDeleteMode: (DeleteMode) -> Unit,
    onResetDefaultFolder: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.plan_section))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.plan_enabled), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.plan_enabled_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = plan.enabled, onCheckedChange = onEnabled, modifier = Modifier.testTag(PLAN_SWITCH_TAG))
            }

            if (plan.enabled && !canNotify) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.NotificationsOff, contentDescription = null)
                        Text(stringResource(R.string.plan_notifications_off), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = onAllowNotifications) { Text(stringResource(R.string.plan_allow_notifications)) }
                    }
                }
            }

            Text(stringResource(R.string.plan_times), style = MaterialTheme.typography.titleSmall)
            plan.slots.forEachIndexed { index, slot ->
                SlotCard(slot = slot, removable = plan.slots.size > 1, onChange = { onSlot(index, it) }, onRemove = { onRemoveSlot(index) })
            }
            TextButton(onClick = onAddSlot) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.plan_add_time), modifier = Modifier.padding(start = 8.dp))
            }

            QuotaSlider(plan.quota, onQuota)
            SourcePicker(plan, sources, onSource, onSourceFilter)

            if (plan.enabled && next != null) {
                Text(stringResource(R.string.plan_next, formatDateTime(next)), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.plan_approximate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onTestReminder, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.plan_test_reminder)) }

            HorizontalDivider()
            SectionTitle(stringResource(R.string.settings_moves_section))
            Text(stringResource(R.string.settings_default_folder), style = MaterialTheme.typography.titleSmall)
            Text(defaultTarget, style = MaterialTheme.typography.bodyMedium)
            if (!TargetPathRules.sameFolder(defaultTarget, TargetPathRules.DEFAULT_TARGET)) {
                TextButton(onClick = onResetDefaultFolder) { Text(stringResource(R.string.settings_reset_default_folder)) }
            }

            HorizontalDivider()
            SectionTitle(stringResource(R.string.settings_delete_section))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.bin_permanent_option), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(
                    checked = deleteMode == DeleteMode.PERMANENT,
                    onCheckedChange = { onDeleteMode(if (it) DeleteMode.PERMANENT else DeleteMode.TRASH) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotCard(slot: PlanSlot, removable: Boolean, onChange: (PlanSlot) -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val days = (0L until 7L).map { firstDay.plus(it) }
    var picking by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(context, slot.time),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).clickable { picking = true },
                )
                if (removable) {
                    IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.plan_remove_time)) }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                days.forEach { day ->
                    val selected = day in slot.days
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val newDays = if (selected) slot.days - day else slot.days + day
                            if (newDays.isNotEmpty()) onChange(slot.copy(days = newDays))
                        },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                    )
                }
            }
        }
    }
    if (picking) {
        val state = rememberTimePickerState(slot.time.hour, slot.time.minute, DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { picking = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    picking = false
                    onChange(slot.copy(time = LocalTime.of(state.hour, state.minute)))
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun QuotaSlider(quota: Int, onQuota: (Int) -> Unit) {
    var value by remember(quota) { mutableFloatStateOf(quota.toFloat()) }
    val shown = (value / 10f).roundToInt() * 10
    Column {
        Text(stringResource(R.string.plan_quota), style = MaterialTheme.typography.titleSmall)
        Text(
            pluralStringResource(R.plurals.plan_quota_value, shown, shown, CleanupPlan(quota = shown).estimatedMinutes),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onQuota(shown) },
            // Continuous track, rounded to tens (48 tick marks would clutter the slider).
            valueRange = CleanupPlan.MIN_QUOTA.toFloat()..CleanupPlan.MAX_QUOTA.toFloat(),
        )
    }
}

@Composable
private fun SourcePicker(plan: CleanupPlan, sources: List<SourceOption>, onSource: (SourceOption) -> Unit, onFilter: (MediaFilter) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val allName = stringResource(R.string.home_all)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.plan_source), style = MaterialTheme.typography.titleSmall)
        Box {
            OutlinedButton(onClick = { open = true }) { Text(plan.source.bucketName ?: allName) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                sources.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name ?: allName) },
                        onClick = {
                            open = false
                            onSource(option)
                        },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(MediaFilter.BOTH to R.string.filter_both, MediaFilter.PHOTOS to R.string.filter_photos, MediaFilter.VIDEOS to R.string.filter_videos)
                .forEach { (filter, label) ->
                    FilterChip(selected = plan.source.filter == filter, onClick = { onFilter(filter) }, label = { Text(stringResource(label)) })
                }
        }
    }
}

private fun formatTime(context: android.content.Context, time: LocalTime): String {
    val date = Date(ZonedDateTime.now().with(time).toInstant().toEpochMilli())
    return DateFormat.getTimeFormat(context).format(date)
}

@Composable
private fun formatDateTime(value: ZonedDateTime): String {
    val context = LocalContext.current
    val date = Date(value.toInstant().toEpochMilli())
    val day = value.dayOfWeek.getDisplayName(TextStyle.FULL, LocalConfiguration.current.locales[0])
    return "$day, ${DateFormat.getTimeFormat(context).format(date)}"
}

