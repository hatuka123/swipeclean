package com.hatuka.swipeclean.ui.reviewed

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.hatuka.swipeclean.core.move.TargetPathRules
import com.hatuka.swipeclean.core.review.DecisionState
import com.hatuka.swipeclean.data.db.DecisionEntity
import com.hatuka.swipeclean.data.media.MediaRepository
import com.hatuka.swipeclean.data.review.DecisionActions
import com.hatuka.swipeclean.data.review.DecisionRepository
import com.hatuka.swipeclean.data.settings.SettingsRepository
import com.hatuka.swipeclean.ui.bin.DecisionEditingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewedUiState(
    val items: List<DecisionEntity> = emptyList(),
    /** Folders (current location) that have reviewed items, for the filter. */
    val folders: List<String> = emptyList(),
    val folder: String? = null,
    val loading: Boolean = true,
)

/**
 * History of kept and moved items. Any of them can be changed on purpose (e.g. keep → delete);
 * nothing here returns to a swipe session unless the user resets a folder.
 */
@HiltViewModel
class ReviewedViewModel @Inject constructor(
    private val decisions: DecisionRepository,
    private val media: MediaRepository,
    settings: SettingsRepository,
    actions: DecisionActions,
) : DecisionEditingViewModel(actions, settings) {

    private val folder = MutableStateFlow<String?>(null)

    val state: StateFlow<ReviewedUiState> = combine(decisions.reviewedItems(), folder) { items, selected ->
        val folders = items.map(::folderOf).distinct().sortedBy { it.lowercase() }
        val chosen = selected?.takeIf { it in folders }
        ReviewedUiState(
            items = if (chosen == null) items else items.filter { folderOf(it) == chosen },
            folders = folders,
            folder = chosen,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewedUiState())

    init {
        // Items deleted or moved away outside the app disappear from the history silently.
        viewModelScope.launch {
            val ids = decisions.reviewedItems().first().map { it.mediaId }
            if (ids.isNotEmpty()) {
                val visible = media.visibleIds(ids)
                decisions.dropStale(ids.filter { it !in visible })
            }
        }
    }

    fun setFolder(value: String?) {
        folder.value = value
    }

    fun uriOf(item: DecisionEntity): Uri = media.uriOf(item.toRow().type, item.mediaId)

    companion object {
        /** Where the item is now: the move target for moved items, else its original folder. */
        fun folderOf(item: DecisionEntity): String =
            (if (item.state == DecisionState.MOVED) item.targetPath?.let(TargetPathRules::displayName) else null)
                ?: item.bucketName ?: item.relativePath?.let(TargetPathRules::displayName).orEmpty()
    }
}
