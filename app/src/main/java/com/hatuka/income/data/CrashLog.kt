package com.hatuka.income.data

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the last crash (or serious error) on the phone so the owner, who has no logcat, can copy it
 * from the home screen and send it. Nothing leaves the phone by itself.
 */
object CrashLog {
    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching { write(app, "CRASH (thread ${thread.name})", e) }
            previous?.uncaughtException(thread, e)
        }
    }

    /** Records a caught error that still deserves a report. */
    fun record(context: Context, where: String, e: Throwable) {
        runCatching { write(context.applicationContext, where, e) }
    }

    fun read(context: Context): String? = file(context).takeIf { it.isFile }?.readText()

    fun clear(context: Context) {
        file(context).delete()
    }

    private fun file(context: Context) = File(context.applicationContext.filesDir, FILE)

    private fun write(context: Context, where: String, e: Throwable) {
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
        val trace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString()
        val text = buildString {
            appendLine("MyIncome $version · ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()) + " · " + where)
            append(trace.lines().take(60).joinToString("\n"))
        }
        file(context).writeText(text)
    }
}
