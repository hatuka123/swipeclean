package com.hatuka.swipeclean.ui.moves

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.core.move.TargetPathRules
import com.hatuka.swipeclean.data.db.DecisionEntity
import com.hatuka.swipeclean.ui.bin.DecisionEditor
import com.hatuka.swipeclean.ui.bin.DecisionEditorCallbacks
import com.hatuka.swipeclean.ui.bin.editorCallbacks
import com.hatuka.swipeclean.ui.common.MediaTile
import com.hatuka.swipeclean.ui.theme.LocalActionColors
import java.text.NumberFormat

const val MOVE_ITEM_TAG = "move_item"

@Composable
fun MovesRoute(onBack: () -> Unit, viewModel: MovesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val request by viewModel.request.collectAsStateWithLifecycle()
    val defaultTarget by viewModel.defaultTarget.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onRequestResult(it.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(request) {
        request?.let {
            viewModel.onRequestLaunched()
            launcher.launch(IntentSenderRequest.Builder(it).build())
        }
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val nf = NumberFormat.getIntegerInstance()
            val text = when (event) {
                is MovesEvent.Moved -> buildString {
                    append(resources.getString(R.string.moves_result_moved, nf.format(event.count)))
                    if (event.failed > 0) append('\n').append(resources.getString(R.string.moves_result_failed, nf.format(event.failed)))
                    if (event.originalsKept > 0) append('\n').append(resources.getString(R.string.moves_result_originals_kept, nf.format(event.originalsKept)))
                }
                is MovesEvent.Failed -> resources.getString(R.string.moves_result_failed, nf.format(event.count))
                MovesEvent.Cancelled -> resources.getString(R.string.moves_result_cancelled)
            }
            snackbar.showSnackbar(text)
        }
    }

    MovesContent(state, snackbar, viewModel::uriOf, onBack, viewModel::applyAll, viewModel.editorCallbacks(defaultTarget))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovesContent(
    state: MovesUiState,
    snackbar: SnackbarHostState,
    uriOf: (DecisionEntity) -> Uri?,
    onBack: () -> Unit,
    onApplyAll: () -> Unit,
    editor: DecisionEditorCallbacks,
) {
    var reviewing by rememberSaveable { mutableStateOf<Long?>(null) }
    val nf = NumberFormat.getIntegerInstance()
    val groups = state.items.groupBy { it.targetPath.orEmpty() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.moves_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            if (state.items.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.moves_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onApplyAll,
                            enabled = !state.busy,
                            colors = ButtonDefaults.buttonColors(containerColor = LocalActionColors.current.move, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.moves_apply_all, nf.format(state.items.size)))
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (!state.loading && state.items.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.moves_empty), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.moves_empty_body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(108.dp),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                groups.forEach { (target, items) ->
                    item(key = "header:$target", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "→ " + TargetPathRules.displayName(target) + " · " +
                                pluralStringResource(R.plurals.items_count, items.size, nf.format(items.size)),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp),
                        )
                    }
                    items(items, key = { it.mediaId }) { item ->
                        MediaTile(item = item, uri = uriOf(item), selected = false, tag = MOVE_ITEM_TAG, onClick = { reviewing = item.mediaId })
                    }
                }
            }
        }
    }

    state.items.firstOrNull { it.mediaId == reviewing }?.let { item ->
        DecisionEditor(item = item, uri = uriOf(item), callbacks = editor, onClose = { reviewing = null })
    }
}
