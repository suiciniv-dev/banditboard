package dev.clawdboard.desktop

import dev.clawdboard.core.Sample
import dev.clawdboard.core.UsageSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import java.io.File

object DeskHistory {
    private const val WEEK = 7 * 86_400_000L
    private const val STEP = 30 * 60_000L
    private val file = File(Store.dir, "history.json")
    val samples = MutableStateFlow(load())

    private fun load(): List<Sample> = runCatching {
        val a = JSONArray(file.readText())
        (0 until a.length()).map { i ->
            val s = a.getJSONArray(i)
            Sample(s.getLong(0), s.optDouble(1).takeUnless { it.isNaN() }, s.optDouble(2).takeUnless { it.isNaN() })
        }
    }.getOrDefault(emptyList())

    @Synchronized
    fun record(snap: UsageSnapshot) {
        val now = snap.fetchedAt
        val list = samples.value
        if (list.isNotEmpty() && now - list.last().t < STEP) return
        val next = (list + Sample(now, snap.fiveHour?.percent, snap.sevenDay?.percent)).filter { now - it.t <= WEEK }
        val a = JSONArray()
        next.forEach { a.put(JSONArray().put(it.t).put(it.p5 ?: Double.NaN).put(it.p7 ?: Double.NaN)) }
        runCatching { file.writeText(a.toString()) }
        samples.value = next
    }
}
