package com.hatuka.swipeclean.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hatuka.swipeclean.MainActivity
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.core.plan.CleanupPlan
import com.hatuka.swipeclean.data.review.ProgressRepository
import com.hatuka.swipeclean.data.settings.SettingsRepository
import com.hatuka.swipeclean.ui.nav.Routes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Posts "Time to clean: N items, ~M min", opening the swipe screen for the plan's source. */
@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val progress: ProgressRepository,
) {
    fun canNotify(): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Shows the reminder. Skipped when the plan is off or today's quota is already met, unless
     * [force] (the "send a test reminder" button). Returns whether a notification was posted.
     */
    suspend fun notifyIfDue(force: Boolean = false): Boolean {
        val plan = settings.plan.first()
        val today = progress.current()
        if (!force && (!plan.enabled || today.quotaMet)) return false
        val remaining = (plan.quota - today.todayReviewed).coerceIn(1, plan.quota)
        return post(plan, remaining)
    }

    private suspend fun post(plan: CleanupPlan, items: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        ensureChannel()
        val minutes = CleanupPlan(quota = items).estimatedMinutes
        val text = context.resources.getQuantityString(R.plurals.reminder_text, items, items, minutes)
        val sort = settings.sortOrder.first()
        val deepLink = Uri.parse(Routes.swipeDeepLink(plan.source.bucketId, plan.source.filter, sort))
        val open = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(Intent.ACTION_VIEW, deepLink, context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing", e)
            false
        }
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.reminder_channel_description)
            },
        )
    }

    companion object {
        const val CHANNEL_ID = "daily_cleanup"
        const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE = 1001
        private const val TAG = "ReminderNotifier"
    }
}
