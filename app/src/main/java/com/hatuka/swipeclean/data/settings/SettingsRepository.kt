package com.hatuka.swipeclean.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hatuka.swipeclean.core.media.MediaFilter
import com.hatuka.swipeclean.core.media.SortOrder
import com.hatuka.swipeclean.core.move.TargetPathRules
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
    }
}
