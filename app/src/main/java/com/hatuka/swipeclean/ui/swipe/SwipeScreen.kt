package com.hatuka.swipeclean.ui.swipe

import androidx.compose.runtime.mutableIntStateOf
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import com.hatuka.swipeclean.ui.common.shareMedia
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.hatuka.swipeclean.core.move.TargetPathRules
import com.hatuka.swipeclean.core.plan.Progress
import androidx.compose.material3.LinearProgressIndicator
import com.hatuka.swipeclean.ui.common.FolderPickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.core.media.MediaRow
import com.hatuka.swipeclean.core.review.SessionCounters
import com.hatuka.swipeclean.core.review.SwipeDirection
import com.hatuka.swipeclean.data.db.BinSummary
import com.hatuka.swipeclean.ui.common.BinButton
import com.hatuka.swipeclean.ui.common.ForceLtr
import com.hatuka.swipeclean.ui.common.formatSize
import com.hatuka.swipeclean.ui.theme.LocalActionColors
import kotlinx.coroutines.launch
import java.text.NumberFormat

@Composable
fun SwipeRoute(
    onBack: () -> Unit,
    onOpenBin: () -> Unit,
    onOpenMoves: () -> Unit,
    onOpenDonate: () -> Unit = {},
    viewModel: SwipeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bin by viewModel.binSummary.collectAsStateWithLifecycle()
    val moves by viewModel.movesSummary.collectAsStateWithLifecycle()
    val defaultTarget by viewModel.defaultTarget.collectAsStateWithLifecycle()
    val dailyGoal by viewModel.dailyGoal.collectAsStateWithLifecycle()
    val goalReached by viewModel.goalReached.collectAsStateWithLifecycle()
    val showTour by viewModel.showTour.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    PreloadUpcoming(state.upcoming, viewModel::uriOf)
    SwipeContent(
        state = state,
        bin = bin,
        uriOf = viewModel::uriOf,
        onKeep = viewModel::keep,
        onMarkForDeletion = viewModel::markForDeletion,
        onUndo = viewModel::undo,
        onUnavailable = viewModel::onItemUnavailable,
        onBack = onBack,
        onOpenBin = onOpenBin,
        moves = moves,
        defaultTarget = defaultTarget,
        onMoveToDefault = { viewModel.moveToDefault() },
        onMoveTo = viewModel::moveTo,
        loadFolderOptions = viewModel::folderOptions,
        onOpenMoves = onOpenMoves,
        dailyGoal = dailyGoal,
        goalReached = goalReached,
        onContinueAfterGoal = viewModel::dismissGoalReached,
        onFinishForToday = {
            viewModel.dismissGoalReached()
            activity?.finish()
        },
        onShare = { item -> context.shareMedia(viewModel.uriOf(item), item.type) },
        showTour = showTour,
        onTourFinished = { completed ->
            viewModel.finishTour()
            // The tour ends with the donation page; skipping it does not.
            if (completed) onOpenDonate()
        },
    )
}

/** Decodes the next few photos (and video posters) at screen size so swiping never waits. */
@Composable
private fun PreloadUpcoming(upcoming: List<MediaRow>, uriOf: (MediaRow) -> Uri) {
    val context = LocalContext.current
    val window = LocalWindowInfo.current.containerSize
    LaunchedEffect(upcoming.map { it.id }) {
        val loader = SingletonImageLoader.get(context)
        val w = window.width.coerceAtLeast(1)
        val h = window.height.coerceAtLeast(1)
        upcoming.forEach { loader.enqueue(ImageRequest.Builder(context).data(uriOf(it)).size(w, h).build()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeContent(
    state: SwipeUiState,
    bin: BinSummary,
    uriOf: (MediaRow) -> Uri?,
    onKeep: () -> Unit,
    onMarkForDeletion: () -> Unit,
    onUndo: () -> Unit,
    onUnavailable: (Long) -> Unit,
    onBack: () -> Unit,
    onOpenBin: () -> Unit,
    moves: BinSummary = BinSummary(0, 0),
    defaultTarget: String = TargetPathRules.DEFAULT_TARGET,
    onMoveToDefault: () -> Boolean = { false },
    onMoveTo: (String, Boolean) -> Unit = { _, _ -> },
    loadFolderOptions: suspend () -> List<String> = { emptyList() },
    onOpenMoves: () -> Unit = {},
    dailyGoal: Progress? = null,
    goalReached: Boolean = false,
    onContinueAfterGoal: () -> Unit = {},
    onFinishForToday: () -> Unit = {},
    onShare: (MediaRow) -> Unit = {},
    showTour: Boolean = false,
    onTourFinished: (completed: Boolean) -> Unit = {},
    initialTourStep: Int = 0,
) {
    var muted by rememberSaveable { mutableStateOf(true) }
    var tourStep by rememberSaveable { mutableIntStateOf(initialTourStep) }
    val anchors = remember { TourAnchors() }
    var picking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val current = state.current
    val cardState = remember(current?.id) { CardSwipeState() }
    val defaultFits = current != null && TargetPathRules.isValidFor(current.type, defaultTarget)

    fun openPicker() {
        cardState.reset()
        picking = true
    }

    fun commit(direction: SwipeDirection) {
        when (direction) {
            SwipeDirection.RIGHT -> onKeep()
            SwipeDirection.LEFT -> onMarkForDeletion()
            SwipeDirection.UP -> if (!onMoveToDefault()) openPicker()
        }
    }

    fun byButton(direction: SwipeDirection) {
        if (current == null || cardState.isAnimating) return
        if (direction == SwipeDirection.UP && !defaultFits) {
            openPicker()
            return
        }
        scope.launch {
            cardState.flyOut(direction)
            commit(direction)
        }
    }

    if (picking && current != null) {
        var options by remember(current.id) { mutableStateOf<List<String>?>(null) }
        LaunchedEffect(current.id) { options = loadFolderOptions() }
        FolderPickerDialog(
            options = options,
            defaultTarget = defaultTarget,
            onPick = { path, makeDefault ->
                picking = false
                scope.launch {
                    cardState.flyOut(SwipeDirection.UP)
                    onMoveTo(path, makeDefault)
                }
            },
            onDismiss = { picking = false },
        )
    }

    if (goalReached && dailyGoal != null) {
        GoalReachedDialog(dailyGoal.todayReviewed, onContinue = onContinueAfterGoal, onFinish = onFinishForToday)
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(state.sourceName ?: stringResource(R.string.home_all), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!state.loading) {
                                Text(
                                    pluralStringResource(R.plurals.items_left, state.remaining, NumberFormat.getIntegerInstance().format(state.remaining)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (current != null) {
                            IconButton(onClick = { onShare(current) }, modifier = Modifier.tourAnchor(anchors, TourTarget.SHARE)) {
                                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                            }
                        }
                        Box(Modifier.tourAnchor(anchors, TourTarget.BIN)) { BinButton(bin.count, onOpenBin) }
                    },
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                CountersRow(state.counters)
                dailyGoal?.let { DailyGoalBar(it) }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        state.loading -> CircularProgressIndicator()
                        current == null -> FinishedContent(state.counters, bin, moves, onOpenBin, onOpenMoves, onBack)
                        else -> {
                            state.upcoming.firstOrNull()?.let { next ->
                                SwipeCard(
                                    item = next,
                                    uri = uriOf(next),
                                    state = remember(next.id) { CardSwipeState() },
                                    isTop = false,
                                    allowUp = false,
                                    muted = true,
                                    onToggleMute = {},
                                    onSwiped = {},
                                    onUnavailable = {},
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = 0.94f
                                            scaleY = 0.94f
                                            alpha = 0.7f
                                        },
                                )
                            }
                            key(current.id) {
                                SwipeCard(
                                    item = current,
                                    uri = uriOf(current),
                                    state = cardState,
                                    isTop = true,
                                    allowUp = true,
                                    muted = muted,
                                    onToggleMute = { muted = !muted },
                                    onSwiped = ::commit,
                                    onUnavailable = { onUnavailable(current.id) },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .tourAnchor(anchors, TourTarget.CARD),
                                )
                            }
                        }
                    }
                }
                if (!state.finished) ActionButtons(
                    enabled = current != null,
                    canUndo = state.canUndo,
                    moveLabel = TargetPathRules.displayName(defaultTarget),
                    onDelete = { byButton(SwipeDirection.LEFT) },
                    onUndo = onUndo,
                    onMove = { byButton(SwipeDirection.UP) },
                    onPickFolder = { if (current != null && !cardState.isAnimating) openPicker() },
                    onKeep = { byButton(SwipeDirection.RIGHT) },
                    anchors = anchors,
                )
            }
        }
        if (showTour && current != null && !state.loading) {
            TourOverlay(
                anchors = anchors,
                stepIndex = tourStep.coerceIn(0, TOUR_STEPS.lastIndex),
                onNext = { if (tourStep >= TOUR_STEPS.lastIndex) onTourFinished(true) else tourStep++ },
                onSkip = { onTourFinished(false) },
            )
        }
    }
}


/** Shown once a day when the daily goal is reached: keep sorting, or close the app for today. */
@Composable
private fun GoalReachedDialog(reviewed: Int, onContinue: () -> Unit, onFinish: () -> Unit) {
    AlertDialog(
        onDismissRequest = onContinue,
        icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) },
        title = { Text(stringResource(R.string.goal_dialog_title), textAlign = TextAlign.Center) },
        text = { Text(stringResource(R.string.goal_dialog_text, NumberFormat.getIntegerInstance().format(reviewed))) },
        confirmButton = { Button(onClick = onContinue) { Text(stringResource(R.string.goal_dialog_continue)) } },
        dismissButton = { TextButton(onClick = onFinish) { Text(stringResource(R.string.goal_dialog_finish)) } },
    )
}

/** Progress toward the plan's daily goal (only while the plan is on). */
@Composable
private fun DailyGoalBar(goal: Progress) {
    val nf = NumberFormat.getIntegerInstance()
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            if (goal.quotaMet) stringResource(R.string.today_goal_reached)
            else stringResource(R.string.today_progress, nf.format(goal.todayReviewed), nf.format(goal.quota)),
            style = MaterialTheme.typography.labelMedium,
            color = if (goal.quotaMet) LocalActionColors.current.keep else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(progress = { goal.todayFraction }, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
    }
}

@Composable
private fun CountersRow(counters: SessionCounters) {
    val nf = NumberFormat.getIntegerInstance()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.swipe_reviewed, nf.format(counters.reviewed)), style = MaterialTheme.typography.labelLarge)
        Text(
            stringResource(R.string.swipe_to_delete, nf.format(counters.markedForDeletion)),
            style = MaterialTheme.typography.labelLarge,
            color = LocalActionColors.current.delete,
        )
        Text(stringResource(R.string.swipe_frees, formatSize(counters.bytesToFree)), style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Physical layout (undo, delete, move, keep from left to right) in every language.
 * Move: tap = default folder, long-press or the small folder button = choose a folder.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionButtons(
    enabled: Boolean,
    canUndo: Boolean,
    moveLabel: String,
    onDelete: () -> Unit,
    onUndo: () -> Unit,
    onMove: () -> Unit,
    onPickFolder: () -> Unit,
    onKeep: () -> Unit,
    anchors: TourAnchors = TourAnchors(),
) {
    val colors = LocalActionColors.current
    val moveDescription = stringResource(R.string.action_move_to, moveLabel)
    val pickDescription = stringResource(R.string.action_pick_folder)
    ForceLtr {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(48.dp).tourAnchor(anchors, TourTarget.UNDO)) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.action_undo))
            }
            FilledIconButton(
                onClick = onDelete,
                enabled = enabled,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.delete, contentColor = Color.White),
                modifier = Modifier.size(64.dp).tourAnchor(anchors, TourTarget.DELETE),
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_mark_delete), modifier = Modifier.size(32.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.tourAnchor(anchors, TourTarget.MOVE)) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (enabled) colors.move else colors.move.copy(alpha = 0.38f))
                        .combinedClickable(
                            enabled = enabled,
                            onClick = onMove,
                            onLongClick = onPickFolder,
                            onClickLabel = moveDescription,
                            onLongClickLabel = pickDescription,
                        )
                        .semantics { contentDescription = moveDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(moveLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.widthIn(max = 72.dp))
                    IconButton(onClick = onPickFolder, enabled = enabled, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = pickDescription, modifier = Modifier.size(16.dp))
                    }
                }
            }
            FilledIconButton(
                onClick = onKeep,
                enabled = enabled,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.keep, contentColor = Color.White),
                modifier = Modifier.size(64.dp).tourAnchor(anchors, TourTarget.KEEP),
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = stringResource(R.string.action_keep), modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun FinishedContent(
    counters: SessionCounters,
    bin: BinSummary,
    moves: BinSummary,
    onOpenBin: () -> Unit,
    onOpenMoves: () -> Unit,
    onBack: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.TaskAlt, contentDescription = null, tint = LocalActionColors.current.keep, modifier = Modifier.size(72.dp))
        Text(
            stringResource(if (counters.reviewed > 0) R.string.swipe_done_title else R.string.swipe_nothing_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(stringResource(R.string.swipe_done_body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        if (moves.count > 0) {
            Button(onClick = onOpenMoves) {
                Text(stringResource(R.string.open_moves_count, NumberFormat.getIntegerInstance().format(moves.count)))
            }
        }
        if (bin.count > 0) {
            Button(onClick = onOpenBin) {
                Text(stringResource(R.string.open_bin_count, NumberFormat.getIntegerInstance().format(bin.count), formatSize(bin.bytes)))
            }
        }
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back_to_folders)) }
    }
}
