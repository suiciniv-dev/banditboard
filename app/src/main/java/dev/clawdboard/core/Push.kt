package dev.clawdboard.core

import org.json.JSONObject

fun parsePush(o: JSONObject, now: Long): UsageSnapshot? {
    fun window(name: String): UsageWindow? {
        val w = o.optJSONObject(name) ?: return null
        val pct = w.num("used_percentage") ?: return null
        val reset = w.optLong("resets_at", 0L).takeIf { it > 0 }?.times(1000)
        return UsageWindow(pct.coerceIn(0.0, 100.0), reset)
    }
    val five = window("five_hour")
    val seven = window("seven_day")
    if (five == null && seven == null) return null
    val scoped = o.optJSONArray("scoped")?.let { arr ->
        (0 until arr.length()).mapNotNull { i ->
            val s = arr.optJSONObject(i) ?: return@mapNotNull null
            val label = s.str("label") ?: return@mapNotNull null
            val pct = s.num("used_percentage") ?: return@mapNotNull null
            ScopedLimit(label, pct.coerceIn(0.0, 100.0), s.optLong("resets_at", 0L).takeIf { it > 0 }?.times(1000))
        }
    }.orEmpty()
    return UsageSnapshot(five, seven, scoped, now)
}

fun UsageSnapshot.settled(now: Long): UsageSnapshot {
    fun settle(w: UsageWindow?) = if (w?.resetsAt != null && w.resetsAt <= now) UsageWindow(0.0, null) else w
    val five = settle(fiveHour)
    val seven = settle(sevenDay)
    val alive = scoped.filter { it.resetsAt == null || it.resetsAt > now }
    return if (five == fiveHour && seven == sevenDay && alive.size == scoped.size) this else copy(fiveHour = five, sevenDay = seven, scoped = alive)
}
