package com.hatuka.swipeclean.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.hatuka.swipeclean.core.access.MediaAccess
import com.hatuka.swipeclean.core.access.MediaAccessResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Current gallery access level. Permissions can change while the app is in the background
 * (e.g. the user picks more photos in system settings), so the activity calls [refresh] on
 * every resume and after each permission request.
 */
@Singleton
class MediaAccessMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _access = MutableStateFlow(compute())
    val access: StateFlow<MediaAccess> = _access.asStateFlow()

    val permissionsToRequest: Array<String>
        get() = MediaAccessResolver.permissionsToRequest(Build.VERSION.SDK_INT).toTypedArray()

    fun refresh() {
        _access.value = compute()
    }

    private fun compute(): MediaAccess = MediaAccessResolver.resolve(Build.VERSION.SDK_INT) {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
