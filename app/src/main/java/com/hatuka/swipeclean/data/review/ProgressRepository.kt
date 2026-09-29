package com.hatuka.swipeclean.data.review

import com.hatuka.swipeclean.core.plan.DayStats
import com.hatuka.swipeclean.core.plan.Progress
import com.hatuka.swipeclean.core.plan.ProgressCalculator
import com.hatuka.swipeclean.data.db.AppDatabase
import com.hatuka.swipeclean.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Daily progress toward the plan's quota, the streak and all-time totals. */
@Singleton
class ProgressRepository @Inject constructor(
    db: AppDatabase,
    settings: SettingsRepository,
    private val clock: Clock,
) {
    val progress: Flow<Progress> = combine(db.stats().observeAll(), settings.plan) { stats, plan ->
        ProgressCalculator.progress(
            stats = stats.map { DayStats(it.epochDay, it.reviewed, it.deleted, it.bytesFreed) },
            plannedDays = if (plan.enabled) plan.plannedDays else emptySet(),
            quota = plan.quota,
            today = today(),
        )
    }

    // The device time zone may change while the app runs; read it each time.
    private fun today(): LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

    fun todayEpochDay(): Long = today().toEpochDay()

    suspend fun current(): Progress = progress.first()
}
