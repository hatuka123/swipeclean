package com.hatuka.swipeclean.ui.bin

import android.content.IntentSender
import android.util.Log
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatuka.swipeclean.core.review.DecisionState
import com.hatuka.swipeclean.core.review.DeletionReconciler
import com.hatuka.swipeclean.data.db.BinSummary
import com.hatuka.swipeclean.data.db.DecisionEntity
import com.hatuka.swipeclean.data.media.MediaModifier
import com.hatuka.swipeclean.data.media.MediaRepository
import com.hatuka.swipeclean.data.review.DecisionRepository
import com.hatuka.swipeclean.data.settings.DeleteMode
import com.hatuka.swipeclean.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BinUiState(
    val items: List<DecisionEntity> = emptyList(),
    val summary: BinSummary = BinSummary(0, 0),
    val selection: Set<Long> = emptySet(),
    val deleteMode: DeleteMode = DeleteMode.TRASH,
    val busy: Boolean = false,
    val loading: Boolean = true,
)

sealed interface BinEvent {
    data class Deleted(val count: Int, val bytes: Long, val failed: Int) : BinEvent
    data class Failed(val count: Int) : BinEvent
    data object Cancelled : BinEvent
}

/**
 * The review-before-delete step. Final deletion is one system request per chunk of up to 500
 * items (usually a single dialog), and only items MediaStore confirms as gone leave the bin.
 */
@HiltViewModel
class BinViewModel @Inject constructor(
    private val decisions: DecisionRepository,
    private val media: MediaRepository,
    private val modifier: MediaModifier,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val selection = MutableStateFlow<Set<Long>>(emptySet())
    private val busy = MutableStateFlow(false)

    val state: StateFlow<BinUiState> = combine(
        decisions.binItems(),
        decisions.binSummary(),
        selection,
        settings.deleteMode,
        busy,
    ) { items, summary, selected, mode, isBusy ->
        BinUiState(items, summary, selected intersect items.mapTo(HashSet()) { it.mediaId }, mode, isBusy, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BinUiState())

    private val _request = MutableStateFlow<IntentSender?>(null)

    /** A system confirmation to launch; the screen calls [onRequestLaunched] right away. */
    val request: StateFlow<IntentSender?> = _request.asStateFlow()

    private val _events = Channel<BinEvent>(Channel.BUFFERED)
    val events: Flow<BinEvent> = _events.receiveAsFlow()

    private companion object {
        const val TAG = "BinViewModel"
        const val RECHECK_ATTEMPTS = 15
        const val RECHECK_DELAY_MS = 200L
    }

    private val queue = ArrayDeque<List<DecisionEntity>>()
    private var inFlight: List<DecisionEntity> = emptyList()
    private var deletedCount = 0
    private var deletedBytes = 0L
    private var failedCount = 0

    init {
        viewModelScope.launch { dropVanishedItems() }
    }

    /** Items deleted or moved away outside the app are silently removed from the bin. */
    private suspend fun dropVanishedItems() {
        val ids = decisions.pendingDeletes().map { it.mediaId }
        if (ids.isEmpty()) return
        val visible = media.visibleIds(ids)
        decisions.dropStale(ids.filter { it !in visible })
    }

    fun uriOf(item: DecisionEntity): Uri = media.uriOf(item.toRow().type, item.mediaId)

    fun toggleSelection(id: Long) {
        selection.value = selection.value.let { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    /** "Restore" = change the decision to keep; the item is still reviewed and won't come back. */
    fun keepSelected() {
        val ids = selection.value.toList()
        selection.value = emptySet()
        viewModelScope.launch { decisions.changeDecision(ids, DecisionState.KEEP) }
    }

    fun keep(id: Long) {
        viewModelScope.launch { decisions.changeDecision(listOf(id), DecisionState.KEEP) }
    }

    fun setDeleteMode(mode: DeleteMode) {
        viewModelScope.launch { settings.setDeleteMode(mode) }
    }

    fun deleteAll() {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            dropVanishedItems()
            val items = decisions.pendingDeletes()
            if (items.isEmpty()) {
                busy.value = false
                return@launch
            }
            queue.clear()
            queue.addAll(DeletionReconciler.chunks(items))
            deletedCount = 0
            deletedBytes = 0
            failedCount = 0
            nextRequest()
        }
    }

    fun onRequestLaunched() {
        _request.value = null
    }

    fun onRequestResult(confirmed: Boolean) {
        viewModelScope.launch {
            val chunk = inFlight
            inFlight = emptyList()
            if (!confirmed) {
                queue.clear()
                finish(cancelled = true)
                return@launch
            }
            val ids = chunk.map { it.mediaId }
            // MediaProvider applies the change right after returning RESULT_OK, so items can still
            // be visible for a moment; re-check briefly before calling anything a failure.
            var visible = media.visibleIds(ids)
            var attempts = 0
            while (visible.isNotEmpty() && attempts < RECHECK_ATTEMPTS) {
                delay(RECHECK_DELAY_MS)
                visible = media.visibleIds(visible)
                attempts++
            }
            val outcome = DeletionReconciler.reconcile(ids, visible, confirmed = true)
            val deleted = chunk.filter { it.mediaId in outcome.deleted.toHashSet() }
            decisions.markDeleted(deleted)
            deletedCount += deleted.size
            deletedBytes += deleted.sumOf { it.sizeBytes }
            failedCount += outcome.failed.size
            nextRequest()
        }
    }

    private suspend fun nextRequest() {
        val chunk = queue.removeFirstOrNull()
        if (chunk == null) {
            finish(cancelled = false)
            return
        }
        inFlight = chunk
        val uris = chunk.map { uriOf(it) }
        val sender = runCatching {
            if (settings.deleteMode.first() == DeleteMode.PERMANENT) modifier.deleteRequest(uris) else modifier.trashRequest(uris)
        }.onFailure { Log.w(TAG, "Could not create the system delete request", it) }.getOrNull()
        if (sender == null) {
            failedCount += chunk.size
            inFlight = emptyList()
            nextRequest()
        } else {
            _request.value = sender
        }
    }

    private suspend fun finish(cancelled: Boolean) {
        busy.value = false
        _events.send(
            when {
                deletedCount > 0 -> BinEvent.Deleted(deletedCount, deletedBytes, failedCount)
                cancelled -> BinEvent.Cancelled
                else -> BinEvent.Failed(failedCount)
            },
        )
    }
}
