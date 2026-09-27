package com.hatuka.swipeclean.ui.bin

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.data.db.DecisionEntity
import com.hatuka.swipeclean.data.settings.DeleteMode
import com.hatuka.swipeclean.ui.common.MediaTile
import com.hatuka.swipeclean.ui.common.countAndSize
import com.hatuka.swipeclean.ui.common.formatSize
import com.hatuka.swipeclean.ui.theme.LocalActionColors
import java.text.NumberFormat
import android.text.format.Formatter as AndroidFormatter

const val BIN_ITEM_TAG = "bin_item"

@Composable
fun BinRoute(onBack: () -> Unit, viewModel: BinViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val request by viewModel.request.collectAsStateWithLifecycle()
    val defaultTarget by viewModel.defaultTarget.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
            val res = resources
            val nf = NumberFormat.getIntegerInstance()
            val text = when (event) {
                is BinEvent.Deleted -> buildString {
                    append(res.getString(R.string.bin_result_deleted, nf.format(event.count), AndroidFormatter.formatShortFileSize(context, event.bytes)))
                    if (event.failed > 0) append('\n').append(res.getString(R.string.bin_result_failed, nf.format(event.failed)))
                }
                is BinEvent.Failed -> res.getString(R.string.bin_result_failed, nf.format(event.count))
                BinEvent.Cancelled -> res.getString(R.string.bin_result_cancelled)
            }
            snackbar.showSnackbar(text)
        }
    }

    BinContent(
        state = state,
        snackbar = snackbar,
        uriOf = viewModel::uriOf,
        onBack = onBack,
        onToggle = viewModel::toggleSelection,
        onClearSelection = viewModel::clearSelection,
        onKeepSelected = viewModel::keepSelected,
        editor = viewModel.editorCallbacks(defaultTarget),
        onDeleteAll = viewModel::deleteAll,
        onDeleteMode = viewModel::setDeleteMode,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinContent(
    state: BinUiState,
    snackbar: SnackbarHostState,
    uriOf: (DecisionEntity) -> Uri?,
    onBack: () -> Unit,
    onToggle: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onKeepSelected: () -> Unit,
    editor: DecisionEditorCallbacks,
    onDeleteAll: () -> Unit,
    onDeleteMode: (DeleteMode) -> Unit,
) {
    var reviewing by rememberSaveable { mutableStateOf<Long?>(null) }
    val selecting = state.selection.isNotEmpty()
    val nf = NumberFormat.getIntegerInstance()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    if (selecting) {
                        Text(stringResource(R.string.bin_selected, nf.format(state.selection.size)))
                    } else {
                        Column {
                            Text(stringResource(R.string.bin_title))
                            if (state.summary.count > 0) {
                                Text(
                                    countAndSize(state.summary.count, state.summary.bytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (selecting) {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.bin_clear_selection))
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (selecting) {
                        TextButton(onClick = onKeepSelected) { Text(stringResource(R.string.bin_keep_selected)) }
                    } else {
                        DeleteModeMenu(state.deleteMode, onDeleteMode)
                    }
                },
            )
        },
        bottomBar = {
            if (state.items.isNotEmpty() && !selecting) {
                DeleteAllBar(state, onDeleteAll)
            }
        },
    ) { padding ->
        if (!state.loading && state.items.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.bin_empty), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.bin_empty_body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
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
                items(state.items, key = { it.mediaId }) { item ->
                    MediaTile(
                        item = item,
                        uri = uriOf(item),
                        selected = item.mediaId in state.selection,
                        tag = BIN_ITEM_TAG,
                        onClick = { if (selecting) onToggle(item.mediaId) else reviewing = item.mediaId },
                        onLongClick = { onToggle(item.mediaId) },
                    )
                }
            }
        }
    }

    state.items.firstOrNull { it.mediaId == reviewing }?.let { item ->
        DecisionEditor(item = item, uri = uriOf(item), callbacks = editor, onClose = { reviewing = null })
    }
}


@Composable
private fun DeleteAllBar(state: BinUiState, onDeleteAll: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(if (state.deleteMode == DeleteMode.TRASH) R.string.bin_trash_note else R.string.bin_permanent_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onDeleteAll,
                enabled = !state.busy,
                colors = ButtonDefaults.buttonColors(containerColor = LocalActionColors.current.delete, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.bin_delete_all, formatSize(state.summary.bytes)))
            }
        }
    }
}

@Composable
private fun DeleteModeMenu(mode: DeleteMode, onDeleteMode: (DeleteMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.bin_permanent_option)) },
                leadingIcon = { Checkbox(checked = mode == DeleteMode.PERMANENT, onCheckedChange = null) },
                onClick = {
                    onDeleteMode(if (mode == DeleteMode.PERMANENT) DeleteMode.TRASH else DeleteMode.PERMANENT)
                    open = false
                },
            )
        }
    }
}
