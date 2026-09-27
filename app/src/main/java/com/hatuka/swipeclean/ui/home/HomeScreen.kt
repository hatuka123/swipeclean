package com.hatuka.swipeclean.ui.home

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.core.access.MediaAccess
import com.hatuka.swipeclean.core.media.BucketSummary
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.MediaType
import com.hatuka.swipeclean.core.media.SortOrder
import com.hatuka.swipeclean.ui.common.BinButton
import com.hatuka.swipeclean.ui.common.MediaThumbnail
import com.hatuka.swipeclean.ui.common.countAndSize
import com.hatuka.swipeclean.ui.common.formatSize
import com.hatuka.swipeclean.ui.stats.TodayCard
import com.hatuka.swipeclean.core.plan.Progress
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import java.text.NumberFormat

/** Test tag prefix for folder rows, used by instrumented tests. */
const val FOLDER_ROW_TAG = "folder:"

@Composable
fun HomeRoute(
    onOpenBucket: (bucketId: Long?, filter: MediaFilter, sort: SortOrder) -> Unit,
    onChangeAccess: () -> Unit,
    onOpenBin: () -> Unit,
    onOpenMoves: () -> Unit,
    onOpenReviewed: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bin by viewModel.binSummary.collectAsStateWithLifecycle()
    val moves by viewModel.movesSummary.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var reset by remember { mutableStateOf<ResetRequest?>(null) }

    HomeContent(
        state = state,
        binCount = bin.count,
        movesCount = moves.count,
        onOpenBin = onOpenBin,
        onOpenMoves = onOpenMoves,
        onOpenReviewed = onOpenReviewed,
        onOpenSettings = onOpenSettings,
        onOpenStats = onOpenStats,
        progress = progress,
        planEnabled = plan.enabled,
        coverUri = viewModel::coverUri,
        onFilter = viewModel::setFilter,
        onSort = viewModel::setSort,
        onOpenBucket = { onOpenBucket(it, state.filter, state.sort) },
        onResetBucket = { id, name -> scope.launch { reset = viewModel.resetRequest(id, name) } },
        onChangeAccess = onChangeAccess,
    )

    reset?.let { request ->
        ResetDialog(
            request = request,
            onConfirm = {
                viewModel.resetProgress(request.bucketId)
                reset = null
            },
            onDismiss = { reset = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    binCount: Int,
    onOpenBin: () -> Unit,
    coverUri: (MediaType?, Long?) -> Uri?,
    onFilter: (MediaFilter) -> Unit,
    onOpenBucket: (Long?) -> Unit,
    onChangeAccess: () -> Unit,
    movesCount: Int = 0,
    onOpenMoves: () -> Unit = {},
    onOpenReviewed: () -> Unit = {},
    onSort: (SortOrder) -> Unit = {},
    onResetBucket: (Long?, String?) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    progress: Progress? = null,
    planEnabled: Boolean = false,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    BinButton(binCount, onOpenBin)
                    OverflowMenu(onOpenReviewed, onOpenSettings, onOpenStats)
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.access == MediaAccess.PARTIAL) {
                item(key = "partial") { PartialAccessBanner(onChangeAccess) }
            }
            if (movesCount > 0) {
                item(key = "moves") { MovesBanner(movesCount, onOpenMoves) }
            }
            if (progress != null && planEnabled) {
                item(key = "today") { TodayCard(progress, Modifier.clickable(onClick = onOpenStats)) }
            } else if (progress != null && progress.totalBytesFreed > 0) {
                item(key = "freed") {
                    TextButton(onClick = onOpenStats) {
                        Text(stringResource(R.string.stats_freed_total, formatSize(progress.totalBytesFreed)))
                    }
                }
            }
            item(key = "filters") { FilterRow(state.filter, onFilter, state.sort, onSort) }
            when {
                state.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.buckets.isEmpty -> item(key = "empty") {
                    Text(
                        stringResource(if (state.access == MediaAccess.PARTIAL) R.string.home_empty_partial else R.string.home_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 32.dp),
                    )
                }
                else -> {
                    val all = state.buckets.all
                    item(key = "all") {
                        val allName = stringResource(R.string.home_all)
                        BucketRow(
                            bucket = all.copy(name = allName),
                            cover = coverUri(all.coverType, all.coverId),
                            emphasized = true,
                            onClick = { onOpenBucket(null) },
                            onLongClick = { onResetBucket(null, allName) },
                        )
                    }
                    item(key = "header") {
                        Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                            Text(
                                stringResource(R.string.home_folders),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                stringResource(R.string.home_reset_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(state.buckets.buckets, key = { it.bucketId ?: -1L }) { bucket ->
                        BucketRow(
                            bucket = bucket,
                            cover = coverUri(bucket.coverType, bucket.coverId),
                            emphasized = false,
                            onClick = { onOpenBucket(bucket.bucketId) },
                            onLongClick = { onResetBucket(bucket.bucketId, bucket.name) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverflowMenu(onOpenReviewed: () -> Unit, onOpenSettings: () -> Unit, onOpenStats: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(
                R.string.settings_title to onOpenSettings,
                R.string.stats_title to onOpenStats,
                R.string.reviewed_title to onOpenReviewed,
            ).forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = {
                        open = false
                        action()
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterRow(selected: MediaFilter, onFilter: (MediaFilter) -> Unit, sort: SortOrder, onSort: (SortOrder) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            MediaFilter.BOTH to R.string.filter_both,
            MediaFilter.PHOTOS to R.string.filter_photos,
            MediaFilter.VIDEOS to R.string.filter_videos,
        ).forEach { (filter, label) ->
            FilterChip(
                selected = selected == filter,
                onClick = { onFilter(filter) },
                label = { Text(stringResource(label)) },
            )
        }
        SortChip(sort, onSort)
    }
}

private fun SortOrder.label() = when (this) {
    SortOrder.OLDEST_FIRST -> R.string.sort_oldest
    SortOrder.NEWEST_FIRST -> R.string.sort_newest
    SortOrder.LARGEST_FIRST -> R.string.sort_largest
}

@Composable
private fun SortChip(sort: SortOrder, onSort: (SortOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text(stringResource(sort.label())) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort_title)) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(stringResource(order.label())) },
                    onClick = {
                        open = false
                        onSort(order)
                    },
                )
            }
        }
    }
}

@Composable
private fun MovesBanner(count: Int, onOpenMoves: () -> Unit) {
    Card(
        onClick = onOpenMoves,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.DriveFileMove, contentDescription = null)
            Text(
                stringResource(R.string.home_moves_banner, NumberFormat.getIntegerInstance().format(count)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun PartialAccessBanner(onChangeAccess: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null)
                Text(stringResource(R.string.partial_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(stringResource(R.string.partial_body), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            TextButton(onClick = onChangeAccess, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.partial_change))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BucketRow(bucket: BucketSummary, cover: Uri?, emphasized: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val nf = NumberFormat.getIntegerInstance()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (emphasized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(FOLDER_ROW_TAG + (bucket.bucketId ?: "all")),
    ) {
        Row(
            Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MediaThumbnail(uri = cover, isVideo = bucket.coverType == MediaType.VIDEO, size = if (emphasized) 64.dp else 56.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    bucket.name,
                    style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    countAndSize(bucket.count, bucket.sizeBytes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (bucket.unreviewed == 0) {
                        stringResource(R.string.home_all_reviewed)
                    } else {
                        pluralStringResource(R.plurals.to_review, bucket.unreviewed, nf.format(bucket.unreviewed))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun ResetDialog(request: ResetRequest, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val name = request.name.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reset_title, name)) },
        text = {
            Text(
                if (request.count == 0) {
                    stringResource(R.string.reset_nothing)
                } else {
                    pluralStringResource(R.plurals.reset_body, request.count, NumberFormat.getIntegerInstance().format(request.count))
                },
            )
        },
        confirmButton = {
            if (request.count > 0) TextButton(onClick = onConfirm) { Text(stringResource(R.string.reset_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
