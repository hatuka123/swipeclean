package com.hatuka.swipeclean.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** TRASH = recoverable for ~30 days in the system trash (default); PERMANENT = no trash. */
enum class DeleteMode { TRASH, PERMANENT }

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.settingsStore

    val deleteMode: Flow<DeleteMode> = store.data.map { prefs ->
        prefs[DELETE_MODE]?.let { runCatching { DeleteMode.valueOf(it) }.getOrNull() } ?: DeleteMode.TRASH
    }

    suspend fun setDeleteMode(mode: DeleteMode) {
        store.edit { it[DELETE_MODE] = mode.name }
    }

    private companion object {
        val DELETE_MODE = stringPreferencesKey("delete_mode")
    }
}
