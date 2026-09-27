package com.hatuka.swipeclean.reminders

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.hatuka.swipeclean.core.plan.CleanupPlan
import com.hatuka.swipeclean.core.plan.PlanSlot
import com.hatuka.swipeclean.data.db.AppDatabase
import com.hatuka.swipeclean.data.review.ProgressRepository
import com.hatuka.swipeclean.data.settings.SettingsRepository
import com.hatuka.swipeclean.testing.inMemoryDb
import com.hatuka.swipeclean.testing.testSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ReminderTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()
    // Sunday 2026-09-27, 10:00 UTC.
    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var scheduler: ReminderScheduler
    private lateinit var notifier: ReminderNotifier
    private lateinit var previousZone: TimeZone
    private lateinit var factory: WorkerFactory

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        db = inMemoryDb()
        settings = testSettings()
        val progress = ProgressRepository(db, settings, clock)
        notifier = ReminderNotifier(app, settings, progress)
        scheduler = ReminderScheduler({ WorkManager.getInstance(app) }, settings, clock)
        factory = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                ReminderWorker(appContext, workerParameters, notifier, scheduler)
        }
        WorkManagerTestInitHelper.initializeTestWorkManager(
            app,
            Configuration.Builder().setExecutor(SynchronousExecutor()).setWorkerFactory(factory).build(),
        )
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun tearDown() {
        db.close()
        TimeZone.setDefault(previousZone)
    }

    private fun work(): WorkInfo? =
        WorkManager.getInstance(app).getWorkInfosForUniqueWork(ReminderScheduler.WORK_NAME).get().firstOrNull { !it.state.isFinished }

    private suspend fun enablePlan(quota: Int = 50, time: LocalTime = LocalTime.of(20, 0)) =
        settings.setPlan(CleanupPlan(enabled = true, slots = listOf(PlanSlot(DayOfWeek.entries.toSet(), time)), quota = quota))

    @Test
    fun `plan off means nothing is scheduled`() = runTest {
        assertNull(scheduler.reschedule())
        assertNull(work())
    }

    @Test
    fun `schedules the next slot with the right delay and cancels when turned off`() = runTest {
        enablePlan()
        val next = scheduler.reschedule()!!
        assertEquals(LocalTime.of(20, 0), next.toLocalTime())
        val info = work()!!
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(TimeUnit.HOURS.toMillis(10), info.initialDelayMillis)

        settings.setPlan(CleanupPlan(enabled = false))
        assertNull(scheduler.reschedule())
        assertNull(work())
    }

    @Test
    fun `the reminder shows a notification and schedules the next one`() = runBlocking {
        enablePlan(quota = 40)
        val worker = TestListenableWorkerBuilder<ReminderWorker>(app).setWorkerFactory(factory).build()
        assertEquals(ListenableWorker.Result.success(), worker.doWork())

        val manager = app.getSystemService(NotificationManager::class.java)
        val posted = shadowOf(manager).allNotifications
        assertEquals(1, posted.size)
        val text = posted.single().extras.getCharSequence("android.text").toString()
        assertTrue(text, text.contains("40"))
        // The worker scheduled the following reminder.
        assertEquals(WorkInfo.State.ENQUEUED, work()?.state)
    }

    @Test
    fun `no reminder once today's goal is met, but the test button still works`() = runTest {
        enablePlan(quota = 10)
        db.stats().add(LocalDate.now(clock).toEpochDay(), reviewed = 12)
        assertFalse(notifier.notifyIfDue())
        assertTrue(notifier.notifyIfDue(force = true))
    }
}
