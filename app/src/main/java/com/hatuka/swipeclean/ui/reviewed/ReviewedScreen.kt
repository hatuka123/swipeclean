package com.hatuka.swipeclean.ui.reviewed

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.data.db.DecisionEntity
import com.hatuka.swipeclean.ui.bin.DecisionEditor
import com.hatuka.swipeclean.ui.bin.DecisionEditorCallbacks
import com.hatuka.swipeclean.ui.bin.editorCallbacks
import com.hatuka.swipeclean.ui.common.MediaTile

const val REVIEWED_ITEM_TAG = "reviewed_item"

@Composable
fun ReviewedRoute(onBack: () -> Unit, viewModel: ReviewedViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val defaultTarget by viewModel.defaultTarget.collectAsStateWithLifecycle()
    ReviewedContent(state, viewModel::uriOf, onBack, viewModel::setFolder, viewModel.editorCallbacks(defaultTarget))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewedContent(
    state: ReviewedUiState,
    uriOf: (DecisionEntity) -> Uri?,
    onBack: () -> Unit,
    onFolder: (String?) -> Unit,
    editor: DecisionEditorCallbacks,
) {
    var reviewing by rememberSaveable { mutableStateOf<Long?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reviewed_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (!state.loading && state.items.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.reviewed_empty), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(108.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "hint", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(4.dp)) {
                    Text(stringResource(R.string.reviewed_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = state.folder == null, onClick = { onFolder(null) }, label = { Text(stringResource(R.string.reviewed_all_folders)) })
                        state.folders.forEach { folder ->
                            FilterChip(selected = state.folder == folder, onClick = { onFolder(folder) }, label = { Text(folder) })
                        }
                    }
                }
            }
            items(state.items, key = { it.mediaId }) { item ->
                MediaTile(item = item, uri = uriOf(item), selected = false, tag = REVIEWED_ITEM_TAG, onClick = { reviewing = item.mediaId })
            }
        }
    }

    state.items.firstOrNull { it.mediaId == reviewing }?.let { item ->
        DecisionEditor(item = item, uri = uriOf(item), callbacks = editor, onClose = { reviewing = null })
    }
}
