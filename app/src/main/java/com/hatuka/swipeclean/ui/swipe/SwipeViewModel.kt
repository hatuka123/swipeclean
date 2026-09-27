package com.hatuka.swipeclean.ui.swipe

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.MediaRow
import com.hatuka.swipeclean.core.media.SortOrder
import com.hatuka.swipeclean.core.move.TargetPathRules
import com.hatuka.swipeclean.core.review.DecisionState
import com.hatuka.swipeclean.core.review.DeckBuilder
import com.hatuka.swipeclean.core.review.SessionCounters
import com.hatuka.swipeclean.core.review.SwipeSession
import com.hatuka.swipeclean.data.db.BinSummary
import com.hatuka.swipeclean.data.media.MediaRepository
import com.hatuka.swipeclean.core.plan.Progress
import com.hatuka.swipeclean.data.review.DecisionActions
import com.hatuka.swipeclean.data.review.ProgressRepository
import kotlinx.coroutines.flow.combine
import com.hatuka.swipeclean.data.review.DecisionRepository
import com.hatuka.swipeclean.data.settings.SettingsRepository
import com.hatuka.swipeclean.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class SwipeUiState(
    val loading: Boolean = true,
    /** Folder name; null for "All photos & videos". */
    val sourceName: String? = null,
    val current: MediaRow? = null,
    val upcoming: List<MediaRow> = emptyList(),
    val remaining: Int = 0,
    val counters: SessionCounters = SessionCounters(),
    val canUndo: Boolean = false,
) {
    val finished: Boolean get() = !loading && current == null
}

/**
 * Drives one swipe session. The deck/undo/counter logic lives in [SwipeSession] (:core); this
 * class only loads the deck and persists each decision. Swipes never touch files.
 */
@HiltViewModel
class SwipeViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val media: MediaRepository,
    private val decisions: DecisionRepository,
    private val settings: SettingsRepository,
    private val actions: DecisionActions,
    progressRepository: ProgressRepository,
) : ViewModel() {

    /** Today's progress toward the daily goal; null while the plan is off. */
    val dailyGoal: StateFlow<Progress?> = combine(progressRepository.progress, settings.plan) { p, plan -> p.takeIf { plan.enabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val bucketId: Long? = savedState.get<Long>("bucket")?.takeIf { it != Routes.ALL_BUCKETS }
    private val filter: MediaFilter = savedState.get<String>("filter")
        ?.let { runCatching { MediaFilter.valueOf(it) }.getOrNull() } ?: MediaFilter.BOTH
    private val sort: SortOrder = savedState.get<String>("sort")
        ?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() } ?: SortOrder.OLDEST_FIRST

    private val _state = MutableStateFlow(SwipeUiState())
    val state: StateFlow<SwipeUiState> = _state.asStateFlow()

    val binSummary: StateFlow<BinSummary> = decisions.binSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BinSummary(0, 0))

    val movesSummary: StateFlow<BinSummary> = decisions.movesSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BinSummary(0, 0))

    /** Folder for a plain swipe up (default Pictures/Found/). */
    val defaultTarget: StateFlow<String> = settings.defaultTarget
        .stateIn(viewModelScope, SharingStarted.Eagerly, TargetPathRules.DEFAULT_TARGET)

    private var session: SwipeSession? = null

    /** Keeps database writes in swipe order, so an undo never overtakes its own swipe. */
    private val writes = Mutex()

    init {
        viewModelScope.launch {
            val rows = media.loadRows(bucketId, filter)
            val deck = DeckBuilder.build(rows, decisions.decidedIds(), filter, sort)
            session = SwipeSession(deck)
            val name = if (bucketId == null) null else rows.firstOrNull()?.let { it.bucketName ?: it.relativePath }
            publish(sourceName = name)
        }
    }

    fun keep() = decide(DecisionState.KEEP)

    fun markForDeletion() = decide(DecisionState.DELETE_PENDING)

    /**
     * Swipe up: queue a move to the default folder. Returns false (and does nothing) when the
     * default folder cannot hold this kind of item, so the screen can open the folder picker.
     */
    fun moveToDefault(): Boolean {
        val item = session?.current ?: return false
        val target = defaultTarget.value
        if (!TargetPathRules.isValidFor(item.type, target)) return false
        moveTo(target, makeDefault = false)
        return true
    }

    /** Queue a move of the current item to [path]; moving into its own folder just keeps it. */
    fun moveTo(path: String, makeDefault: Boolean) {
        val item = session?.current ?: return
        val target = TargetPathRules.normalize(path) ?: return
        if (makeDefault) viewModelScope.launch { settings.setDefaultTarget(target) }
        if (TargetPathRules.sameFolder(target, item.relativePath)) decide(DecisionState.KEEP) else decide(DecisionState.MOVE_PENDING, target)
    }

    /** Folders the current item can be moved to. */
    suspend fun folderOptions(): List<String> {
        val item = session?.current ?: return emptyList()
        return actions.folderOptions(item.type, item.relativePath)
    }

    private fun decide(decision: DecisionState, targetPath: String? = null) {
        val s = session ?: return
        if (s.isFinished) return
        val record = s.decide(decision, targetPath)
        publish()
        viewModelScope.launch { writes.withLock { decisions.record(record) } }
    }

    fun undo() {
        val record = session?.undo() ?: return
        publish()
        viewModelScope.launch { writes.withLock { decisions.revert(record) } }
    }

    /** Called when an item cannot be displayed: if it is gone from MediaStore, skip it silently. */
    fun onItemUnavailable(id: Long) {
        viewModelScope.launch {
            if (id in media.visibleIds(listOf(id))) return@launch
            val s = session ?: return@launch
            if (s.current?.id == id) {
                s.dropCurrent()
                publish()
            }
        }
    }

    fun uriOf(row: MediaRow): Uri = media.uriOf(row.type, row.id)

    private fun publish(sourceName: String? = _state.value.sourceName) {
        val s = session ?: return
        _state.value = SwipeUiState(
            loading = false,
            sourceName = sourceName,
            current = s.current,
            upcoming = s.upcoming(PRELOAD_COUNT),
            remaining = s.remaining,
            counters = s.counters,
            canUndo = s.canUndo,
        )
    }

    private companion object {
        const val PRELOAD_COUNT = 3
    }
}
