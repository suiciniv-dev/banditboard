package dev.clawdboard.desktop

import dev.clawdboard.core.Sample
import dev.clawdboard.core.UsageSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SampleLog(name: String) {
    private val file = File(Store.dir, name)
    val samples = MutableStateFlow(load())

    private fun load(): List<Sample> = runCatching {
        val a = JSONArray(file.readText())
        (0 until a.length()).map { i ->
            val s = a.getJSONArray(i)
            Sample(s.getLong(0), s.optDouble(1).takeUnless { it.isNaN() }, s.optDouble(2).takeUnless { it.isNaN() })
        }
    }.getOrDefault(emptyList())

    fun record(snap: UsageSnapshot) = record(snap.fetchedAt, snap.fiveHour?.percent, snap.sevenDay?.percent)

    @Synchronized
    fun record(now: Long, p5: Double?, p7: Double?) {
        val list = samples.value
        if (list.isNotEmpty() && now - list.last().t < STEP) return
        val next = (list + Sample(now, p5, p7)).filter { now - it.t <= WEEK }
        val a = JSONArray()
        next.forEach { a.put(JSONArray().put(it.t).put(it.p5 ?: JSONObject.NULL).put(it.p7 ?: JSONObject.NULL)) }
        runCatching { file.writeText(a.toString()) }
        samples.value = next
    }

    private companion object {
        const val WEEK = 7 * 86_400_000L
        const val STEP = 30 * 60_000L
    }
}

val DeskHistory = SampleLog("history.json")
val AgHistory = SampleLog("history-ag.json")
