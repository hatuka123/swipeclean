package com.hatuka.swipeclean.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatuka.swipeclean.core.access.MediaAccess
import com.hatuka.swipeclean.core.media.BucketList
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.MediaType
import com.hatuka.swipeclean.core.media.SortOrder
import com.hatuka.swipeclean.data.db.BinSummary
import com.hatuka.swipeclean.data.media.MediaRepository
import com.hatuka.swipeclean.core.plan.CleanupPlan
import com.hatuka.swipeclean.core.plan.Progress
import com.hatuka.swipeclean.data.review.DecisionRepository
import com.hatuka.swipeclean.data.review.ProgressRepository
import com.hatuka.swipeclean.data.settings.SettingsRepository
import com.hatuka.swipeclean.permissions.MediaAccessMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val access: MediaAccess = MediaAccess.NONE,
    val filter: MediaFilter = MediaFilter.BOTH,
    val buckets: BucketList = BucketList.EMPTY,
    val loading: Boolean = true,
    val sort: SortOrder = SortOrder.OLDEST_FIRST,
)

/** A folder reset the user is being asked to confirm. */
data class ResetRequest(val bucketId: Long?, val name: String?, val count: Int)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository,
    access: MediaAccessMonitor,
    private val decisions: DecisionRepository,
    private val settings: SettingsRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {

    val progress: StateFlow<Progress?> = progressRepository.progress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val plan: StateFlow<CleanupPlan> = settings.plan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CleanupPlan())

    val binSummary: StateFlow<BinSummary> = decisions.binSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BinSummary(0, 0))

    val movesSummary: StateFlow<BinSummary> = decisions.movesSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BinSummary(0, 0))

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> = combine(access.access, settings.filter, settings.sortOrder) { a, f, s -> Triple(a, f, s) }
        .distinctUntilChanged()
        .flatMapLatest { (a, f, s) ->
            if (a == MediaAccess.NONE) {
                flowOf(HomeUiState(access = a, filter = f, sort = s, loading = false))
            } else {
                // Reload when the gallery changes (any app) or a decision changes (to-review counts).
                combine(repository.galleryChanges(), decisions.changes()) { _, _ -> }
                    .map { HomeUiState(a, f, repository.loadBuckets(f, decisions.decidedIds()), loading = false, sort = s) }
                    .onStart { emit(HomeUiState(a, f, loading = true, sort = s)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setFilter(value: MediaFilter) {
        viewModelScope.launch { settings.setFilter(value) }
    }

    fun setSort(value: SortOrder) {
        viewModelScope.launch { settings.setSortOrder(value) }
    }

    fun coverUri(type: MediaType?, id: Long?): Uri? =
        if (type == null || id == null) null else repository.uriOf(type, id)

    /** How many reviewed (kept/moved) items currently in this folder a reset would bring back. */
    suspend fun resetRequest(bucketId: Long?, name: String?): ResetRequest {
        val ids = repository.loadRows(bucketId, MediaFilter.BOTH).map { it.id }
        return ResetRequest(bucketId, name, decisions.countResettable(ids))
    }

    /** Shows the folder's kept and moved items in swipe sessions again (bin and moves untouched). */
    fun resetProgress(bucketId: Long?) {
        viewModelScope.launch {
            val ids = repository.loadRows(bucketId, MediaFilter.BOTH).map { it.id }
            decisions.resetProgress(ids)
        }
    }
}
