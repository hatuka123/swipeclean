package com.hatuka.swipeclean.ui.bin

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatuka.swipeclean.core.move.TargetPathRules
import com.hatuka.swipeclean.core.review.DecisionState
import com.hatuka.swipeclean.data.db.DecisionEntity
import com.hatuka.swipeclean.data.review.DecisionActions
import com.hatuka.swipeclean.data.settings.SettingsRepository
import com.hatuka.swipeclean.ui.common.FolderPickerDialog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Base for screens that let the user change earlier decisions (bin, moves, history). */
abstract class DecisionEditingViewModel(
    private val actions: DecisionActions,
    settings: SettingsRepository,
) : ViewModel() {

    val defaultTarget: StateFlow<String> = settings.defaultTarget
        .stateIn(viewModelScope, SharingStarted.Eagerly, TargetPathRules.DEFAULT_TARGET)

    suspend fun folderOptions(item: DecisionEntity): List<String> {
        val currentFolder = if (item.state == DecisionState.MOVED) item.targetPath else item.relativePath
        return actions.folderOptions(item.toRow().type, currentFolder)
    }

    fun change(item: DecisionEntity, state: DecisionState, targetPath: String?, makeDefault: Boolean) {
        viewModelScope.launch { actions.change(item.mediaId, state, targetPath, makeDefault) }
    }
}

/** Callbacks a screen passes to [DecisionEditor]. */
class DecisionEditorCallbacks(
    val defaultTarget: String,
    val loadFolderOptions: suspend (DecisionEntity) -> List<String>,
    val onChange: (DecisionEntity, DecisionState, String?, Boolean) -> Unit,
)

fun DecisionEditingViewModel.editorCallbacks(defaultTarget: String) =
    DecisionEditorCallbacks(defaultTarget, this::folderOptions, this::change)

/** The change-decision dialog for [item], switching to the folder picker for "Move". */
@Composable
fun DecisionEditor(
    item: DecisionEntity,
    uri: Uri?,
    callbacks: DecisionEditorCallbacks,
    onClose: () -> Unit,
) {
    var picking by remember(item.mediaId) { mutableStateOf(false) }
    var options by remember(item.mediaId) { mutableStateOf<List<String>?>(null) }
    if (picking) {
        LaunchedEffect(item.mediaId) { options = callbacks.loadFolderOptions(item) }
        FolderPickerDialog(
            options = options,
            defaultTarget = callbacks.defaultTarget,
            onPick = { path, makeDefault ->
                callbacks.onChange(item, DecisionState.MOVE_PENDING, path, makeDefault)
                onClose()
            },
            onDismiss = { picking = false },
        )
    } else {
        ChangeDecisionDialog(
            item = item,
            uri = uri,
            onKeep = {
                callbacks.onChange(item, DecisionState.KEEP, null, false)
                onClose()
            },
            onDelete = {
                callbacks.onChange(item, DecisionState.DELETE_PENDING, null, false)
                onClose()
            },
            onMove = { picking = true },
            onDismiss = onClose,
        )
    }
}
