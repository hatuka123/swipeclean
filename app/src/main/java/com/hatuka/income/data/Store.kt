package com.hatuka.income.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** Everything the app knows, kept as JSON in SharedPreferences. Nothing leaves the phone. */
object Store {
    private lateinit var prefs: SharedPreferences

    val jobs = mutableStateListOf<Job>()
    val entries = mutableStateListOf<WorkEntry>()
    val oneTimes = mutableStateListOf<OneTime>()
    var settings by mutableStateOf(Settings())
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("income", Context.MODE_PRIVATE)
        jobs.clear(); entries.clear(); oneTimes.clear()
        runCatching { jobs += readList(prefs.getString("jobs", null)) { readJob(it) } }
        runCatching { entries += readList(prefs.getString("entries", null)) { readEntry(it) } }
        runCatching { oneTimes += readList(prefs.getString("oneTimes", null)) { readOneTime(it) } }
        runCatching {
            prefs.getString("settings", null)?.let {
                val o = JSONObject(it)
                settings = Settings(o.optDouble("points", 2.75), o.optDouble("pension", 6.0))
            }
        }
    }

    fun job(id: Long?): Job? = jobs.firstOrNull { it.id == id }

    private fun nextId(ids: List<Long>) = (ids.maxOrNull() ?: 0L) + 1

    // ---- jobs ----
    fun saveJob(j: Job) {
        val i = jobs.indexOfFirst { it.id == j.id }
        if (i >= 0) jobs[i] = j else jobs += j.copy(id = nextId(jobs.map { it.id }))
        persistJobs()
    }

    fun deleteJob(id: Long) {
        jobs.removeAll { it.id == id }
        entries.removeAll { it.jobId == id }
        oneTimes.replaceAll { if (it.jobId == id) it.copy(jobId = null) else it }
        persistJobs(); persistEntries(); persistOneTimes()
    }

    // ---- work entries ----
    fun saveEntry(e: WorkEntry) {
        val i = entries.indexOfFirst { it.id == e.id }
        if (i >= 0) entries[i] = e else entries += e.copy(id = nextId(entries.map { it.id }))
        persistEntries()
    }

    fun deleteEntry(id: Long) {
        entries.removeAll { it.id == id }
        persistEntries()
    }

    // ---- one-off incomes ----
    fun saveOneTime(o: OneTime) {
        val i = oneTimes.indexOfFirst { it.id == o.id }
        if (i >= 0) oneTimes[i] = o else oneTimes += o.copy(id = nextId(oneTimes.map { it.id }))
        persistOneTimes()
    }

    fun deleteOneTime(id: Long) {
        oneTimes.removeAll { it.id == id }
        persistOneTimes()
    }

    fun updateSettings(s: Settings) {
        settings = s
        prefs.edit().putString("settings", JSONObject().put("points", s.creditPoints).put("pension", s.pensionPct).toString()).apply()
    }

    // ---- JSON ----
    private fun persistJobs() = put("jobs", jobs.map {
        JSONObject().put("id", it.id).put("name", it.name).put("kind", it.kind.name).put("url", it.payrollUrl).put("color", it.color)
    })

    private fun persistEntries() = put("entries", entries.map {
        JSONObject().put("id", it.id).put("job", it.jobId).put("from", it.fromDay).put("to", it.toDay)
            .put("days", it.days).put("amount", it.amount).put("note", it.note)
    })

    private fun persistOneTimes() = put("oneTimes", oneTimes.map {
        JSONObject().put("id", it.id).put("day", it.day).put("amount", it.amount).put("title", it.title)
            .apply { it.jobId?.let { j -> put("job", j) } }
    })

    private fun put(key: String, items: List<JSONObject>) {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun <T> readList(s: String?, f: (JSONObject) -> T): List<T> {
        if (s == null) return emptyList()
        val arr = JSONArray(s)
        return (0 until arr.length()).mapNotNull { runCatching { f(arr.getJSONObject(it)) }.getOrNull() }
    }

    private fun readJob(o: JSONObject) = Job(
        o.getLong("id"), o.getString("name"),
        runCatching { JobKind.valueOf(o.getString("kind")) }.getOrDefault(JobKind.EMPLOYEE),
        o.optString("url", ""), o.optInt("color", 0),
    )

    private fun readEntry(o: JSONObject) = WorkEntry(
        o.getLong("id"), o.getLong("job"), o.getLong("from"), o.getLong("to"),
        o.optInt("days", 0), o.getDouble("amount"), o.optString("note", ""),
    )

    private fun readOneTime(o: JSONObject) = OneTime(
        o.getLong("id"), o.getLong("day"), o.getDouble("amount"), o.optString("title", ""),
        if (o.has("job")) o.getLong("job") else null,
    )
}
