package com.hatuka.swipeclean.ui.home

import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import com.hatuka.swipeclean.ui.common.BinButton
import com.hatuka.swipeclean.ui.common.MediaThumbnail
import com.hatuka.swipeclean.ui.common.countAndSize

/** Test tag prefix for folder rows, used by instrumented tests. */
const val FOLDER_ROW_TAG = "folder:"

@Composable
fun HomeRoute(
    onOpenBucket: (bucketId: Long?, filter: MediaFilter) -> Unit,
    onChangeAccess: () -> Unit,
    onOpenBin: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bin by viewModel.binSummary.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        binCount = bin.count,
        onOpenBin = onOpenBin,
        coverUri = viewModel::coverUri,
        onFilter = viewModel::setFilter,
        onOpenBucket = { onOpenBucket(it, state.filter) },
        onChangeAccess = onChangeAccess,
    )
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
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = { BinButton(binCount, onOpenBin) },
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
            item(key = "filters") { FilterRow(state.filter, onFilter) }
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
                        BucketRow(
                            bucket = all.copy(name = stringResource(R.string.home_all)),
                            cover = coverUri(all.coverType, all.coverId),
                            emphasized = true,
                            onClick = { onOpenBucket(null) },
                        )
                    }
                    item(key = "header") {
                        Text(
                            stringResource(R.string.home_folders),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(state.buckets.buckets, key = { it.bucketId ?: -1L }) { bucket ->
                        BucketRow(
                            bucket = bucket,
                            cover = coverUri(bucket.coverType, bucket.coverId),
                            emphasized = false,
                            onClick = { onOpenBucket(bucket.bucketId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(selected: MediaFilter, onFilter: (MediaFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
private fun BucketRow(bucket: BucketSummary, cover: Uri?, emphasized: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (emphasized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth().testTag(FOLDER_ROW_TAG + (bucket.bucketId ?: "all")),
    ) {
        Row(
            Modifier.padding(12.dp),
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
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
