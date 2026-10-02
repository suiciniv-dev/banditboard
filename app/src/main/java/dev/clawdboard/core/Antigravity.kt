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

data class AgSnapshot(
    val models: List<AgModel>,
    val session: UsageWindow?,
    val week: UsageWindow?,
    val plan: String?,
    val fetchedAt: Long,
) {
    fun family(f: AgFamily): AgModel? = models.filter { it.family == f }.maxByOrNull { it.percent }

    fun pool(p: AgPool): AgModel? = models.filter { it.family?.pool == p }.maxByOrNull { it.percent }

    fun worst(): Double? = listOfNotNull(session?.percent, week?.percent).maxOrNull()

    fun toJson(): JSONObject {
        fun w(x: UsageWindow?): Any = x?.let { JSONObject().put("percent", it.percent).put("resetsAt", it.resetsAt ?: JSONObject.NULL) } ?: JSONObject.NULL
        val arr = JSONArray()
        models.forEach {
            arr.put(
                JSONObject().put("label", it.label).put("percent", it.percent).put("resetsAt", it.resetsAt ?: JSONObject.NULL)
                    .put("exhausted", it.exhausted).put("kind", it.kind?.name ?: JSONObject.NULL)
            )
        }
        return JSONObject().put("plan", plan ?: JSONObject.NULL).put("fetchedAt", fetchedAt)
            .put("session", w(session)).put("week", w(week)).put("models", arr)
    }

    companion object {
        fun fromJson(o: JSONObject): AgSnapshot? = runCatching {
            fun w(name: String) = o.optJSONObject(name)?.let { x -> x.num("percent")?.let { UsageWindow(it, x.num("resetsAt")?.toLong()) } }
            val arr = o.optJSONArray("models") ?: JSONArray()
            val models = (0 until arr.length()).mapNotNull { i ->
                val m = arr.optJSONObject(i) ?: return@mapNotNull null
                val label = m.str("label") ?: return@mapNotNull null
                AgModel(
                    label, m.num("percent") ?: 0.0, m.num("resetsAt")?.toLong(), m.optBoolean("exhausted"),
                    m.str("kind")?.let { k -> AlertKind.entries.firstOrNull { it.name == k } },
                )
            }
            AgSnapshot(models, w("session"), w("week"), o.str("plan"), o.optLong("fetchedAt"))
        }.getOrNull()
    }
}

object AgParse {
    const val SESSION_MAX_MS = 6 * 3_600_000L

    fun parse(body: String, now: Long, firstSeen: MutableMap<Long, Long> = mutableMapOf()): AgSnapshot? =
        runCatching { parse(JSONObject(body), now, firstSeen) }.getOrNull()

    fun parse(root: JSONObject, now: Long, firstSeen: MutableMap<Long, Long> = mutableMapOf()): AgSnapshot? {
        val u = root.optJSONObject("userStatus") ?: root
        val configs = u.optJSONObject("cascadeModelConfigData")?.optJSONArray("clientModelConfigs") ?: return null
        val models = (0 until configs.length()).mapNotNull { i -> runCatching { model(configs.optJSONObject(i), now, firstSeen) }.getOrNull() }
            .distinctBy { it.label }
        val plan = u.optJSONObject("userTier")?.str("name")
            ?: u.optJSONObject("planStatus")?.optJSONObject("planInfo")?.str("planName")
        firstSeen.keys.removeAll { it < now }
        fun worst(kind: AlertKind) = models.filter { it.kind == kind }.maxByOrNull { it.percent }?.let { UsageWindow(it.percent, it.resetsAt) }
        return AgSnapshot(models, worst(AlertKind.SESSION), worst(AlertKind.WEEK), plan, now)
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
