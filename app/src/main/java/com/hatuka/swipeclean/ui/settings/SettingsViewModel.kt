package com.hatuka.swipeclean.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.move.TargetPathRules
import com.hatuka.swipeclean.core.plan.CleanupPlan
import com.hatuka.swipeclean.core.plan.PlanSlot
import com.hatuka.swipeclean.data.media.MediaRepository
import com.hatuka.swipeclean.data.settings.DeleteMode
import com.hatuka.swipeclean.data.settings.SettingsRepository
import com.hatuka.swipeclean.reminders.ReminderNotifier
import com.hatuka.swipeclean.reminders.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

/** A folder the plan can review; bucketId null = all photos & videos. */
data class SourceOption(val bucketId: Long?, val name: String?)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
    private val media: MediaRepository,
) : ViewModel() {

    val plan: StateFlow<CleanupPlan> = settings.plan.stateIn(viewModelScope, SharingStarted.Eagerly, CleanupPlan())
    val deleteMode: StateFlow<DeleteMode> = settings.deleteMode.stateIn(viewModelScope, SharingStarted.Eagerly, DeleteMode.TRASH)
    val defaultTarget: StateFlow<String> = settings.defaultTarget.stateIn(viewModelScope, SharingStarted.Eagerly, TargetPathRules.DEFAULT_TARGET)

    private val _nextReminder = MutableStateFlow<ZonedDateTime?>(null)
    val nextReminder: StateFlow<ZonedDateTime?> = _nextReminder.asStateFlow()

    private val _canNotify = MutableStateFlow(true)
    val canNotify: StateFlow<Boolean> = _canNotify.asStateFlow()

    private val _sources = MutableStateFlow(listOf(SourceOption(null, null)))
    val sources: StateFlow<List<SourceOption>> = _sources.asStateFlow()

    private val _events = Channel<Boolean>(Channel.BUFFERED)

    /** Result of "send a test reminder": true = posted. */
    val testResults: Flow<Boolean> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { _nextReminder.value = scheduler.reschedule() }
        viewModelScope.launch {
            val buckets = media.loadBuckets(MediaFilter.BOTH).buckets
            _sources.value = listOf(SourceOption(null, null)) + buckets.map { SourceOption(it.bucketId, it.name) }
        }
        refreshNotificationState()
    }

    /** Called on resume and after the permission dialog. */
    fun refreshNotificationState() {
        _canNotify.value = notifier.canNotify()
    }

    private fun update(transform: (CleanupPlan) -> CleanupPlan) {
        viewModelScope.launch {
            settings.setPlan(transform(settings.plan.first()))
            _nextReminder.value = scheduler.reschedule()
        }
    }

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    fun setQuota(quota: Int) = update { it.copy(quota = quota.coerceIn(CleanupPlan.MIN_QUOTA, CleanupPlan.MAX_QUOTA)) }

    fun setSource(option: SourceOption) = update { it.copy(source = it.source.copy(bucketId = option.bucketId, bucketName = option.name)) }

    fun setSourceFilter(filter: MediaFilter) = update { it.copy(source = it.source.copy(filter = filter)) }

    fun setSlot(index: Int, slot: PlanSlot) = update { plan ->
        plan.copy(slots = plan.slots.mapIndexed { i, s -> if (i == index) slot else s })
    }

    fun addSlot() = update { plan -> plan.copy(slots = plan.slots + PlanSlot(CleanupPlan.DEFAULT_SLOT.days, LocalTime.of(9, 0))) }

    fun removeSlot(index: Int) = update { plan ->
        if (plan.slots.size <= 1) plan else plan.copy(slots = plan.slots.filterIndexed { i, _ -> i != index })
    }

    fun sendTestReminder() {
        viewModelScope.launch { _events.send(notifier.notifyIfDue(force = true)) }
    }

    fun setDeleteMode(mode: DeleteMode) {
        viewModelScope.launch { settings.setDeleteMode(mode) }
    }

    fun resetDefaultFolder() {
        viewModelScope.launch { settings.setDefaultTarget(TargetPathRules.DEFAULT_TARGET) }
    }
}
