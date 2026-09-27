package com.hatuka.swipeclean

import android.graphics.Bitmap
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.hatuka.swipeclean.core.access.MediaAccess
import com.hatuka.swipeclean.core.access.MediaAccessResolver
import java.io.File

/** Screenshots land in the app's external files dir; the CI script pulls them with adb. */
fun takeScreenshot(name: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()
    val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
    val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "shots").apply { mkdirs() }
    File(dir, "${name}_api${Build.VERSION.SDK_INT}.png").outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
}

fun currentAccess(): MediaAccess {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return MediaAccessResolver.resolve(Build.VERSION.SDK_INT) {
        context.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
