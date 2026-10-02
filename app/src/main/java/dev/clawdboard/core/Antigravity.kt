package dev.clawdboard.core

import org.json.JSONArray
import org.json.JSONObject

enum class Tool { CLAUDE, ANTIGRAVITY }

enum class ToolTheme { FOLLOW, CLAUDE, BLACK }

enum class AgPool { GEMINI, OTHERS }

enum class AgFamily(val label: String, val short: String, val accessory: Accessory, val pool: AgPool) {
    PRO("Gemini Pro", "Pro", Accessory.STAR, AgPool.GEMINI),
    FLASH("Gemini Flash", "Flash", Accessory.BOLT, AgPool.GEMINI),
    CLAUDE("Claude", "Claude", Accessory.GLASSES, AgPool.OTHERS),
    GPT("GPT-OSS", "GPT", Accessory.BEANIE, AgPool.OTHERS),
}

fun agFamilyOf(label: String): AgFamily? {
    val l = label.lowercase()
    return when {
        "gemini" in l && "flash" in l -> AgFamily.FLASH
        "gemini" in l -> AgFamily.PRO
        "claude" in l -> AgFamily.CLAUDE
        "gpt" in l -> AgFamily.GPT
        else -> null
    }
}

data class AgModel(val label: String, val percent: Double, val resetsAt: Long?, val exhausted: Boolean, val kind: AlertKind?) {
    val family: AgFamily? get() = agFamilyOf(label)
}

data class AgGroup(val pool: AgPool, val session: UsageWindow?, val week: UsageWindow?) {
    fun windows(): List<Pair<AlertKind, UsageWindow>> =
        listOfNotNull(session?.let { AlertKind.SESSION to it }, week?.let { AlertKind.WEEK to it })

    fun worst(): Pair<AlertKind, UsageWindow>? = windows().maxByOrNull { it.second.percent }
}

data class AgSnapshot(
    val models: List<AgModel>,
    val groups: List<AgGroup>,
    val plan: String?,
    val fetchedAt: Long,
) {
    fun group(p: AgPool): AgGroup? = groups.firstOrNull { it.pool == p }

    fun percent(f: AgFamily): Double? = group(f.pool)?.worst()?.second?.percent

    fun families(): List<AgFamily> =
        if (models.any { it.family != null }) AgFamily.entries.filter { f -> models.any { it.family == f } }
        else AgFamily.entries.filter { group(it.pool) != null }

    fun worst(): Double? = groups.mapNotNull { it.worst()?.second?.percent }.maxOrNull()

    fun sameQuota(other: AgSnapshot?): Boolean = other != null && groups == other.groups && plan == other.plan

    fun toJson(): JSONObject {
        fun w(x: UsageWindow?): Any = x?.let { JSONObject().put("percent", it.percent).put("resetsAt", it.resetsAt ?: JSONObject.NULL) } ?: JSONObject.NULL
        val arr = JSONArray()
        models.forEach {
            arr.put(
                JSONObject().put("label", it.label).put("percent", it.percent).put("resetsAt", it.resetsAt ?: JSONObject.NULL)
                    .put("exhausted", it.exhausted).put("kind", it.kind?.name ?: JSONObject.NULL)
            )
        }
        val gs = JSONArray()
        groups.forEach { gs.put(JSONObject().put("pool", it.pool.name).put("session", w(it.session)).put("week", w(it.week))) }
        return JSONObject().put("plan", plan ?: JSONObject.NULL).put("fetchedAt", fetchedAt).put("groups", gs).put("models", arr)
    }

    companion object {
        fun fromJson(o: JSONObject): AgSnapshot? = runCatching {
            fun w(x: JSONObject, name: String) = x.optJSONObject(name)?.let { y -> y.num("percent")?.let { UsageWindow(it, y.num("resetsAt")?.toLong()) } }
            val arr = o.optJSONArray("models") ?: JSONArray()
            val models = (0 until arr.length()).mapNotNull { i ->
                val m = arr.optJSONObject(i) ?: return@mapNotNull null
                val label = m.str("label") ?: return@mapNotNull null
                AgModel(
                    label, m.num("percent") ?: 0.0, m.num("resetsAt")?.toLong(), m.optBoolean("exhausted"),
                    m.str("kind")?.let { k -> AlertKind.entries.firstOrNull { it.name == k } },
                )
            }
            val gs = o.optJSONArray("groups")
            val groups = if (gs == null) AgParse.derive(models) else (0 until gs.length()).mapNotNull { i ->
                val g = gs.optJSONObject(i) ?: return@mapNotNull null
                val pool = AgPool.entries.firstOrNull { it.name == g.str("pool") } ?: return@mapNotNull null
                AgGroup(pool, w(g, "session"), w(g, "week"))
            }
            AgSnapshot(models, groups, o.str("plan"), o.optLong("fetchedAt"))
        }.getOrNull()
    }
}

fun AgSnapshot.settled(now: Long): AgSnapshot {
    fun s(w: UsageWindow?) = w?.let { x -> x.resetsAt?.takeIf { it <= now }?.let { UsageWindow(0.0, null) } ?: x }
    val next = groups.map { AgGroup(it.pool, s(it.session), s(it.week)) }
    return if (next == groups) this else copy(groups = next)
}

fun AgSnapshot.worst(kind: AlertKind): Pair<AgPool, UsageWindow>? =
    groups.mapNotNull { g -> (if (kind == AlertKind.SESSION) g.session else g.week)?.let { g.pool to it } }.maxByOrNull { it.second.percent }

data class AgRemote(val snap: AgSnapshot, val open: Boolean)

object AgPush {
    fun off(o: JSONObject?): Boolean = o?.optBoolean("off") == true

    fun parse(o: JSONObject?): AgRemote? = runCatching {
        if (o == null || off(o)) return null
        fun w(x: JSONObject, name: String) = x.optJSONObject(name)?.let { y -> y.num("percent")?.let { UsageWindow(it, y.num("resetsAt")?.toLong()) } }
        val pools = o.optJSONArray("pools") ?: return null
        val groups = (0 until pools.length()).mapNotNull { i ->
            val g = pools.optJSONObject(i) ?: return@mapNotNull null
            val pool = AgPool.entries.firstOrNull { it.name == g.str("pool") } ?: return@mapNotNull null
            if (!g.has("session") && !g.has("week")) {
                val flat = g.num("percent")?.let { UsageWindow(it, g.num("resetsAt")?.toLong()) } ?: return@mapNotNull null
                return@mapNotNull if (g.str("kind") == AlertKind.SESSION.name) AgGroup(pool, flat, null) else AgGroup(pool, null, flat)
            }
            AgGroup(pool, w(g, "session"), w(g, "week")).takeIf { it.session != null || it.week != null }
        }.sortedBy { it.pool.ordinal }
        if (groups.isEmpty()) return null
        val arr = o.optJSONArray("models") ?: JSONArray()
        val models = (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { m -> m.str("label")?.let { AgModel(it, m.num("percent") ?: 0.0, null, false, null) } }
        }
        AgRemote(AgSnapshot(models, groups, o.str("plan"), o.optLong("usage_at")), o.optBoolean("open"))
    }.getOrNull()
}

object AgParse {
    const val SESSION_MAX_MS = 6 * 3_600_000L

    fun parse(body: String, now: Long, firstSeen: MutableMap<Long, Long> = mutableMapOf(), quota: String? = null): AgSnapshot? =
        runCatching { parse(JSONObject(body), now, firstSeen, quota?.let { q -> runCatching { JSONObject(q) }.getOrNull() }) }.getOrNull()

    fun parse(root: JSONObject, now: Long, firstSeen: MutableMap<Long, Long> = mutableMapOf(), quota: JSONObject? = null): AgSnapshot? {
        val u = root.optJSONObject("userStatus") ?: root
        val configs = u.optJSONObject("cascadeModelConfigData")?.optJSONArray("clientModelConfigs") ?: return null
        val models = (0 until configs.length()).mapNotNull { i -> runCatching { model(configs.optJSONObject(i), now, firstSeen) }.getOrNull() }
            .distinctBy { it.label }
        val plan = u.optJSONObject("userTier")?.str("name")
            ?: u.optJSONObject("planStatus")?.optJSONObject("planInfo")?.str("planName")
        firstSeen.keys.removeAll { it < now }
        val groups = quota?.let { groups(it, now) }?.takeIf { it.isNotEmpty() } ?: derive(models)
        return AgSnapshot(models, groups, plan, now)
    }

    fun groups(body: String, now: Long): List<AgGroup>? = runCatching { groups(JSONObject(body), now) }.getOrNull()

    fun groups(root: JSONObject, now: Long): List<AgGroup>? {
        val list = (root.optJSONObject("response") ?: root).optJSONArray("groups") ?: return null
        return (0 until list.length()).mapNotNull { i -> runCatching { group(list.optJSONObject(i), now) }.getOrNull() }
            .groupBy { it.pool }
            .map { (pool, gs) -> AgGroup(pool, gs.mapNotNull { it.session }.maxByOrNull { it.percent }, gs.mapNotNull { it.week }.maxByOrNull { it.percent }) }
            .sortedBy { it.pool.ordinal }
    }

    fun derive(models: List<AgModel>): List<AgGroup> = AgPool.entries.mapNotNull { p ->
        val m = models.filter { it.family?.pool == p }.maxByOrNull { it.percent } ?: return@mapNotNull null
        val w = UsageWindow(m.percent, m.resetsAt)
        if (m.kind == AlertKind.SESSION) AgGroup(p, w, null) else AgGroup(p, null, w)
    }

    fun poolOf(text: String): AgPool? {
        val t = text.lowercase()
        return when {
            "claude" in t || "gpt" in t -> AgPool.OTHERS
            "gemini" in t -> AgPool.GEMINI
            "3p" in t -> AgPool.OTHERS
            else -> null
        }
    }

    private fun group(g: JSONObject?, now: Long): AgGroup? {
        if (g == null) return null
        val buckets = g.optJSONArray("buckets") ?: return null
        val all = (0 until buckets.length()).mapNotNull { buckets.optJSONObject(it) }
        val pool = poolOf((listOfNotNull(g.str("displayName")) + all.mapNotNull { it.str("bucketId") }).joinToString(" ")) ?: return null
        var session: UsageWindow? = null
        var week: UsageWindow? = null
        for (b in all) {
            if (b.optBoolean("disabled")) continue
            val remaining = b.num("remainingFraction")?.coerceIn(0.0, 1.0) ?: continue
            val percent = if (remaining <= 0.0) 100.0 else Math.round((1 - remaining) * 1000) / 10.0
            val reset = parseIsoMillis(b.str("resetTime"))
            val w = UsageWindow(percent, reset?.takeIf { it > now && percent > 0 })
            val text = listOfNotNull(b.str("window"), b.str("bucketId"), b.str("displayName")).joinToString(" ").lowercase()
            val weekly = when {
                "week" in text -> true
                "hour" in text || "5h" in text || "session" in text -> false
                else -> reset == null || reset - now > SESSION_MAX_MS
            }
            if (weekly) week = listOfNotNull(week, w).maxByOrNull { it.percent }
            else session = listOfNotNull(session, w).maxByOrNull { it.percent }
        }
        if (session == null && week == null) return null
        return AgGroup(pool, session, week)
    }

    private fun model(c: JSONObject?, now: Long, firstSeen: MutableMap<Long, Long>): AgModel? {
        if (c == null) return null
        val label = c.str("label") ?: return null
        val q = c.optJSONObject("quotaInfo") ?: return null
        if (q.num("remainingFraction") == null && parseIsoMillis(q.str("resetTime")) == null) return null
        val remaining = (q.num("remainingFraction") ?: 0.0).coerceIn(0.0, 1.0)
        val percent = Math.round((1 - remaining) * 1000) / 10.0
        val reset = parseIsoMillis(q.str("resetTime"))?.takeIf { it > now && percent > 0 }
        val kind = reset?.let {
            val seen = firstSeen.getOrPut(it) { now }
            if (it - seen > SESSION_MAX_MS) AlertKind.WEEK else AlertKind.SESSION
        }
        val exhausted = c.optBoolean("isExhausted") || q.optBoolean("isExhausted") || remaining <= 0.0
        return AgModel(label, if (exhausted) 100.0 else percent, reset, exhausted, kind ?: if (exhausted) AlertKind.WEEK else null)
    }
}

fun agFeel(pct: Double?): Feel {
    val heat = if (pct == null) 0f else ((pct - 90) / 10).toFloat().coerceIn(0f, 1f)
    val mood = when {
        pct != null && pct >= 99.5 -> Mood.EXHAUSTED
        pct != null && pct >= 85 -> Mood.SWEATY
        pct == null || pct < 0.5 -> Mood.SLEEPY
        else -> Mood.NORMAL
    }
    return Feel(mood, heat)
}
