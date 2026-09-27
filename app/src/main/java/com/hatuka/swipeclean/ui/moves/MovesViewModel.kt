package com.hatuka.swipeclean.ui.moves

import android.content.IntentSender
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.hatuka.swipeclean.core.review.DeletionReconciler
import com.hatuka.swipeclean.data.db.DecisionEntity
import com.hatuka.swipeclean.data.media.IoDispatcher
import com.hatuka.swipeclean.data.media.MediaModifier
import com.hatuka.swipeclean.data.media.MediaRepository
import com.hatuka.swipeclean.data.media.MoveResult
import com.hatuka.swipeclean.data.review.DecisionActions
import com.hatuka.swipeclean.data.review.DecisionRepository
import com.hatuka.swipeclean.data.settings.SettingsRepository
import com.hatuka.swipeclean.ui.bin.DecisionEditingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class MovesUiState(
    val items: List<DecisionEntity> = emptyList(),
    val busy: Boolean = false,
    val loading: Boolean = true,
)

sealed interface MovesEvent {
    /** [originalsKept]: copied, but the original could not be moved to the system trash. */
    data class Moved(val count: Int, val failed: Int, val originalsKept: Int = 0) : MovesEvent
    data class Failed(val count: Int) : MovesEvent
    data object Cancelled : MovesEvent
}

/**
 * Items queued by swipe-up. "Move all" asks for write access to all of them in one system
 * dialog (per 500 items), then changes each item's RELATIVE_PATH. Undo before applying is
 * instant because nothing touched the files yet.
 */
@HiltViewModel
class MovesViewModel @Inject constructor(
    private val decisions: DecisionRepository,
    private val media: MediaRepository,
    private val modifier: MediaModifier,
    @IoDispatcher private val io: CoroutineDispatcher,
    settings: SettingsRepository,
    actions: DecisionActions,
) : DecisionEditingViewModel(actions, settings) {

    private val busy = MutableStateFlow(false)

    val state: StateFlow<MovesUiState> = combine(decisions.pendingMovesFlow(), busy) { items, isBusy ->
        MovesUiState(items.sortedWith(compareBy<DecisionEntity> { it.targetPath }.thenByDescending { it.decidedAt }), isBusy, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovesUiState())

    private val _request = MutableStateFlow<IntentSender?>(null)
    val request: StateFlow<IntentSender?> = _request.asStateFlow()

    private val _events = Channel<MovesEvent>(Channel.BUFFERED)
    val events: Flow<MovesEvent> = _events.receiveAsFlow()

    private val queue = ArrayDeque<List<DecisionEntity>>()
    private var inFlight: List<DecisionEntity> = emptyList()
    private var moved = 0
    private var failed = 0
    private var originalsKept = 0

    init {
        viewModelScope.launch {
            val ids = decisions.pendingMoves().map { it.mediaId }
            if (ids.isNotEmpty()) {
                val visible = media.visibleIds(ids)
                decisions.dropStale(ids.filter { it !in visible })
            }
        }
    }

    fun uriOf(item: DecisionEntity): Uri = media.uriOf(item.toRow().type, item.mediaId)

    fun applyAll() {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            val items = decisions.pendingMoves().filter { it.targetPath != null }
            if (items.isEmpty()) {
                busy.value = false
                return@launch
            }
            queue.clear()
            queue.addAll(DeletionReconciler.chunks(items))
            moved = 0
            failed = 0
            originalsKept = 0
            next()
        }
    }

    fun onRequestLaunched() {
        _request.value = null
    }

    fun onRequestResult(granted: Boolean) {
        viewModelScope.launch {
            val chunk = inFlight
            inFlight = emptyList()
            if (!granted) {
                queue.clear()
                finish(cancelled = true)
                return@launch
            }
            val results = withContext(io) { chunk.map { it to modifier.moveTo(uriOf(it), it.toRow().type, it.targetPath!!) } }
            decisions.markMoved(results.filter { it.second is MoveResult.Moved }.map { it.first.mediaId })
            results.forEach { (item, result) ->
                if (result is MoveResult.Copied) {
                    decisions.recordCopy(item, result.newId)
                    if (!result.originalTrashed) originalsKept++
                }
            }
            moved += results.count { it.second !is MoveResult.Failed }
            failed += results.count { it.second is MoveResult.Failed }
            next()
        }
    }

    private suspend fun next() {
        val chunk = queue.removeFirstOrNull()
        if (chunk == null) {
            finish(cancelled = false)
            return
        }
        inFlight = chunk
        val sender = runCatching { modifier.writeRequest(chunk.map(::uriOf)) }
            .onFailure { Log.w(TAG, "Could not create the system write request", it) }
            .getOrNull()
        if (sender == null) {
            failed += chunk.size
            inFlight = emptyList()
            next()
        } else {
            _request.value = sender
        }
    }

    private suspend fun finish(cancelled: Boolean) {
        busy.value = false
        _events.send(
            when {
                moved > 0 -> MovesEvent.Moved(moved, failed, originalsKept)
                cancelled -> MovesEvent.Cancelled
                else -> MovesEvent.Failed(failed)
            },
        )
    }

    private companion object {
        const val TAG = "MovesViewModel"
    }
}
