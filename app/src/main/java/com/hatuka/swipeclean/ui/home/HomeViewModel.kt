package com.hatuka.swipeclean.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatuka.swipeclean.core.access.MediaAccess
import com.hatuka.swipeclean.core.media.BucketList
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.MediaType
import com.hatuka.swipeclean.data.media.MediaRepository
import com.hatuka.swipeclean.permissions.MediaAccessMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val access: MediaAccess = MediaAccess.NONE,
    val filter: MediaFilter = MediaFilter.BOTH,
    val buckets: BucketList = BucketList.EMPTY,
    val loading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository,
    access: MediaAccessMonitor,
) : ViewModel() {

    private val filter = MutableStateFlow(MediaFilter.BOTH)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> = combine(access.access, filter) { a, f -> a to f }
        .flatMapLatest { (a, f) ->
            if (a == MediaAccess.NONE) {
                flowOf(HomeUiState(access = a, filter = f, loading = false))
            } else {
                // Reload on every gallery change (items added/deleted/moved by any app).
                repository.galleryChanges()
                    .map { HomeUiState(a, f, repository.loadBuckets(f), loading = false) }
                    .onStart { emit(HomeUiState(a, f, loading = true)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setFilter(value: MediaFilter) {
        filter.value = value
    }

    fun coverUri(type: MediaType?, id: Long?): Uri? =
        if (type == null || id == null) null else repository.uriOf(type, id)
}
