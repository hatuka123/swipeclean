package com.hatuka.swipeclean.reminders

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hatuka.swipeclean.core.plan.PlanSchedule
import com.hatuka.swipeclean.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Schedules the next reminder as one delayed WorkManager job. WorkManager keeps it across
 * reboots and app updates and respects Doze, so the reminder may arrive a little late but no
 * exact-alarm permission is needed. Called on app start, after plan changes, after each
 * reminder and when the time or time zone changes.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    private val workManager: Provider<WorkManager>,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    /** Returns when the next reminder is due, or null if the plan is off (or has no days). */
    suspend fun reschedule(): ZonedDateTime? {
        val plan = settings.plan.first()
        val now = ZonedDateTime.now(clock.withZone(ZoneId.systemDefault()))
        val next = if (plan.enabled) PlanSchedule.nextTrigger(now, plan.slots) else null
        if (next == null) {
            workManager.get().cancelUniqueWork(WORK_NAME)
            return null
        }
        val delay = Duration.between(now, next).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()
        workManager.get().enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        return next
    }

    companion object {
        const val WORK_NAME = "daily_cleanup_reminder"
    }
}
