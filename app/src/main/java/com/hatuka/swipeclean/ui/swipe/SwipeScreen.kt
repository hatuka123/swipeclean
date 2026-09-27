package com.hatuka.swipeclean.ui.swipe

import android.net.Uri
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
    viewModel: SwipeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bin by viewModel.binSummary.collectAsStateWithLifecycle()
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
) {
    var muted by rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val current = state.current
    val cardState = remember(current?.id) { CardSwipeState() }

    fun commit(direction: SwipeDirection) = when (direction) {
        SwipeDirection.RIGHT -> onKeep()
        SwipeDirection.LEFT -> onMarkForDeletion()
        SwipeDirection.UP -> Unit // Moves arrive in phase 3.
    }

    fun byButton(direction: SwipeDirection) {
        if (current == null || cardState.isAnimating) return
        scope.launch {
            cardState.flyOut(direction)
            commit(direction)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.sourceName ?: stringResource(R.string.home_all), maxLines = 1)
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
                actions = { BinButton(bin.count, onOpenBin) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            CountersRow(state.counters)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.loading -> CircularProgressIndicator()
                    current == null -> FinishedContent(state.counters, bin, onOpenBin, onBack)
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
                                allowUp = false,
                                muted = muted,
                                onToggleMute = { muted = !muted },
                                onSwiped = ::commit,
                                onUnavailable = { onUnavailable(current.id) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
            if (!state.finished) ActionButtons(
                enabled = current != null,
                canUndo = state.canUndo,
                onDelete = { byButton(SwipeDirection.LEFT) },
                onUndo = onUndo,
                onKeep = { byButton(SwipeDirection.RIGHT) },
            )
        }
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

/** Physical layout (delete on the left, keep on the right) in every language. */
@Composable
private fun ActionButtons(enabled: Boolean, canUndo: Boolean, onDelete: () -> Unit, onUndo: () -> Unit, onKeep: () -> Unit) {
    val colors = LocalActionColors.current
    ForceLtr {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 20.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = onDelete,
                enabled = enabled,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.delete, contentColor = Color.White),
                modifier = Modifier.size(68.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_mark_delete), modifier = Modifier.size(34.dp))
            }
            FilledTonalIconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(52.dp)) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.action_undo))
            }
            FilledIconButton(
                onClick = onKeep,
                enabled = enabled,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.keep, contentColor = Color.White),
                modifier = Modifier.size(68.dp),
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = stringResource(R.string.action_keep), modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun FinishedContent(counters: SessionCounters, bin: BinSummary, onOpenBin: () -> Unit, onBack: () -> Unit) {
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
        if (bin.count > 0) {
            Button(onClick = onOpenBin) {
                Text(stringResource(R.string.open_bin_count, NumberFormat.getIntegerInstance().format(bin.count), formatSize(bin.bytes)))
            }
        }
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back_to_folders)) }
    }
}
