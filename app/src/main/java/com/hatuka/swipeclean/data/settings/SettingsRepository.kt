package com.hatuka.swipeclean.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.SortOrder
import com.hatuka.swipeclean.core.move.TargetPathRules
import com.hatuka.swipeclean.core.plan.CleanupPlan
import com.hatuka.swipeclean.core.plan.PlanCodec
import com.hatuka.swipeclean.core.plan.PlanSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** TRASH = recoverable for ~30 days in the system trash (default); PERMANENT = no trash. */
enum class DeleteMode { TRASH, PERMANENT }

/** Backed by the app's "settings" DataStore (provided in AppModule; tests pass their own). */
@Singleton
class SettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) {
    val deleteMode: Flow<DeleteMode> = store.data.map { it[DELETE_MODE].toEnum(DeleteMode.TRASH) }

    /** Folder for a plain swipe up; always valid for photos and videos alike. */
    val defaultTarget: Flow<String> = store.data.map { prefs ->
        prefs[DEFAULT_TARGET]?.takeIf(TargetPathRules::isValidForAll) ?: TargetPathRules.DEFAULT_TARGET
    }

    val filter: Flow<MediaFilter> = store.data.map { it[FILTER].toEnum(MediaFilter.BOTH) }

    val sortOrder: Flow<SortOrder> = store.data.map { it[SORT].toEnum(SortOrder.OLDEST_FIRST) }

    /** The daily cleanup plan (reminder times, quota, source). */
    val plan: Flow<CleanupPlan> = store.data.map { prefs ->
        val slots = prefs[PLAN_SLOTS]?.let(PlanCodec::decode)
        CleanupPlan(
            enabled = prefs[PLAN_ENABLED] ?: false,
            slots = if (slots.isNullOrEmpty()) listOf(CleanupPlan.DEFAULT_SLOT) else slots,
            quota = (prefs[PLAN_QUOTA] ?: CleanupPlan.DEFAULT_QUOTA).coerceIn(CleanupPlan.MIN_QUOTA, CleanupPlan.MAX_QUOTA),
            source = PlanSource(
                bucketId = prefs[PLAN_BUCKET]?.takeIf { it >= 0 },
                bucketName = prefs[PLAN_BUCKET_NAME],
                filter = prefs[PLAN_FILTER].toEnum(MediaFilter.BOTH),
            ),
        )
    }

    /** All-time reviewed count when the last ad was shown (see AdPolicy). */
    val adReviewedMark: Flow<Int> = store.data.map { it[AD_REVIEWED_MARK] ?: 0 }

    suspend fun setAdReviewedMark(totalReviewed: Int) = store.edit { it[AD_REVIEWED_MARK] = totalReviewed }

    suspend fun setPlan(plan: CleanupPlan) = store.edit {
        it[PLAN_ENABLED] = plan.enabled
        it[PLAN_SLOTS] = PlanCodec.encode(plan.slots)
        it[PLAN_QUOTA] = plan.quota.coerceIn(CleanupPlan.MIN_QUOTA, CleanupPlan.MAX_QUOTA)
        it[PLAN_BUCKET] = plan.source.bucketId ?: -1L
        val name = plan.source.bucketName
        if (name != null) it[PLAN_BUCKET_NAME] = name else it.remove(PLAN_BUCKET_NAME)
        it[PLAN_FILTER] = plan.source.filter.name
    }

    suspend fun setDeleteMode(mode: DeleteMode) = store.edit { it[DELETE_MODE] = mode.name }

    suspend fun setDefaultTarget(path: String) {
        val normalized = TargetPathRules.normalize(path) ?: return
        store.edit { it[DEFAULT_TARGET] = normalized }
    }

    suspend fun setFilter(filter: MediaFilter) = store.edit { it[FILTER] = filter.name }

    suspend fun setSortOrder(order: SortOrder) = store.edit { it[SORT] = order.name }

    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default

    private companion object {
        val DELETE_MODE = stringPreferencesKey("delete_mode")
        val DEFAULT_TARGET = stringPreferencesKey("default_target")
        val FILTER = stringPreferencesKey("filter")
        val SORT = stringPreferencesKey("sort")
        val PLAN_ENABLED = booleanPreferencesKey("plan_enabled")
        val PLAN_SLOTS = stringPreferencesKey("plan_slots")
        val PLAN_QUOTA = intPreferencesKey("plan_quota")
        val PLAN_BUCKET = longPreferencesKey("plan_bucket")
        val PLAN_BUCKET_NAME = stringPreferencesKey("plan_bucket_name")
        val PLAN_FILTER = stringPreferencesKey("plan_filter")
        val AD_REVIEWED_MARK = intPreferencesKey("ad_reviewed_mark")
    }
}
