package dev.clawdboard.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AntigravityTest {
    private val now = Instant.parse("2026-10-02T10:50:00Z").toEpochMilli()
    private fun fixture(name: String) = javaClass.getResource("/antigravity/$name")!!.readText()

    @Test
    fun fullQuotaHasNoResetAndNoWindows() {
        val s = AgParse.parse(fixture("userstatus-full.json"), now)!!
        assertEquals(14, s.models.size)
        assertTrue(s.models.all { it.percent == 0.0 && it.resetsAt == null && !it.exhausted })
        assertEquals(listOf(AgPool.GEMINI, AgPool.OTHERS), s.groups.map { it.pool })
        assertTrue(s.groups.all { it.session == null && it.week == UsageWindow(0.0, null) })
        assertEquals("Google AI Plus", s.plan)
    }

    @Test
    fun usedGeminiPoolBecomesWeek() {
        val s = AgParse.parse(fixture("userstatus-used.json"), now)!!
        val reset = Instant.parse("2026-10-09T10:43:36Z").toEpochMilli()
        assertEquals(0.4, s.percent(AgFamily.FLASH)!!, 0.001)
        assertEquals(0.4, s.percent(AgFamily.PRO)!!, 0.001)
        assertEquals(0.0, s.percent(AgFamily.CLAUDE)!!, 0.001)
        assertEquals(0.0, s.percent(AgFamily.GPT)!!, 0.001)
        assertEquals(reset, s.group(AgPool.GEMINI)!!.week!!.resetsAt)
        assertNull(s.group(AgPool.GEMINI)!!.session)
    }

    @Test
    fun poolsMatchViewUsage() {
        val s = AgParse.parse(fixture("userstatus-used.json"), now)!!
        assertEquals(AlertKind.WEEK, s.group(AgPool.GEMINI)!!.worst()!!.first)
        assertEquals(0.4, s.group(AgPool.GEMINI)!!.worst()!!.second.percent, 0.001)
        assertEquals(0.0, s.group(AgPool.OTHERS)!!.week!!.percent, 0.001)
        assertNull(s.group(AgPool.OTHERS)!!.week!!.resetsAt)
    }

    @Test
    fun quotaSummaryGivesTheWeeklyWindowOfEachGroup() {
        val s = AgParse.parse(fixture("userstatus-used.json"), now, quota = fixture("quota-weekly.json"))!!
        val gemini = s.group(AgPool.GEMINI)!!
        val others = s.group(AgPool.OTHERS)!!
        assertNull(gemini.session)
        assertEquals(UsageWindow(2.2, Instant.parse("2026-10-09T10:43:36Z").toEpochMilli()), gemini.week)
        assertNull(others.session)
        assertEquals(UsageWindow(0.1, Instant.parse("2026-10-09T16:23:27Z").toEpochMilli()), others.week)
        assertEquals(14, s.models.size)
    }

    @Test
    fun proPlanShowsFiveHoursAndWeekLikeViewUsage() {
        val s = AgParse.parse(fixture("userstatus-used.json"), now, quota = fixture("quota-pro.json"))!!
        val gemini = s.group(AgPool.GEMINI)!!
        assertEquals(UsageWindow(42.0, Instant.parse("2026-10-02T11:21:00Z").toEpochMilli()), gemini.session)
        assertEquals(UsageWindow(21.0, Instant.parse("2026-10-02T13:31:00Z").toEpochMilli()), gemini.week)
        assertEquals(AlertKind.SESSION, gemini.worst()!!.first)
        assertEquals(42.0, s.percent(AgFamily.FLASH)!!, 0.0)
        assertEquals(42.0, s.percent(AgFamily.PRO)!!, 0.0)
        val others = s.group(AgPool.OTHERS)!!
        assertEquals(UsageWindow(0.0, null), others.session)
        assertEquals(UsageWindow(0.0, null), others.week)
        assertEquals(42.0, s.worst()!!, 0.0)
    }

    @Test
    fun bucketKindFallsBackToTheResetDistance() {
        fun quota(id: String, reset: String) =
            """{"response":{"groups":[{"displayName":"Gemini Models","buckets":[{"bucketId":"$id","remainingFraction":0.5,"resetTime":"$reset"}]}]}}"""
        val soon = AgParse.groups(quota("gemini-novo", "2026-10-02T13:00:00Z"), now)!!.single()
        assertEquals(50.0, soon.session!!.percent, 0.0)
        assertNull(soon.week)
        val later = AgParse.groups(quota("gemini-novo", "2026-10-06T13:00:00Z"), now)!!.single()
        assertEquals(50.0, later.week!!.percent, 0.0)
        assertNull(later.session)
    }

    @Test
    fun exhaustedDisabledAndMissingBuckets() {
        val body = """{"response":{"groups":[
            {"displayName":"Claude and GPT models","buckets":[
                {"bucketId":"3p-weekly","window":"weekly","remainingFraction":0,"resetTime":"2026-10-05T10:00:00Z"},
                {"bucketId":"3p-five-hour","displayName":"Five Hour Limit Remaining","disabled":true,"remainingFraction":0.2,"resetTime":"2026-10-02T12:00:00Z"}]},
            {"displayName":"Gemini Models","buckets":[{"bucketId":"gemini-weekly","window":"weekly"}]},
            {"displayName":"Imagens","buckets":[{"bucketId":"img-weekly","window":"weekly","remainingFraction":0.5}]}]}}"""
        val groups = AgParse.groups(body, now)!!
        assertEquals(1, groups.size)
        val others = groups.single()
        assertEquals(AgPool.OTHERS, others.pool)
        assertEquals(UsageWindow(100.0, Instant.parse("2026-10-05T10:00:00Z").toEpochMilli()), others.week)
        assertNull(others.session)
    }

    @Test
    fun unknownQuotaFallsBackToUserStatus() {
        val viaStatus = AgParse.parse(fixture("userstatus-used.json"), now)!!
        val broken = AgParse.parse(fixture("userstatus-used.json"), now, quota = "{\"response\":{}}")!!
        val garbage = AgParse.parse(fixture("userstatus-used.json"), now, quota = "not json")!!
        assertEquals(viaStatus.groups, broken.groups)
        assertEquals(viaStatus.groups, garbage.groups)
    }

    @Test
    fun oldSavedSnapshotStillLoads() {
        val old = """{"plan":"Pro","fetchedAt":5,"session":null,"week":{"percent":30,"resetsAt":99},"models":[
            {"label":"Gemini 3.1 Pro (High)","percent":30,"resetsAt":99,"exhausted":false,"kind":"WEEK"},
            {"label":"Claude Opus 4.6 (Thinking)","percent":0,"resetsAt":null,"exhausted":false,"kind":null}]}"""
        val s = AgSnapshot.fromJson(JSONObject(old))!!
        assertEquals(UsageWindow(30.0, 99L), s.group(AgPool.GEMINI)!!.week)
        assertEquals(UsageWindow(0.0, null), s.group(AgPool.OTHERS)!!.week)
        assertEquals(listOf(AgFamily.PRO, AgFamily.CLAUDE), s.families())
    }

    @Test
    fun windowIsDecidedWhenTheResetIsFirstSeen() {
        val reset = now + 3 * 3_600_000L
        val body = model("Gemini 3.1 Pro (High)", 0.5, Instant.ofEpochMilli(reset).toString())
        assertEquals(AlertKind.SESSION, AgParse.parse(body, now)!!.models[0].kind)
        val seen = mutableMapOf(reset to now - 6 * 86_400_000L)
        assertEquals(AlertKind.WEEK, AgParse.parse(body, now, seen)!!.models[0].kind)
    }

    @Test
    fun missingFractionWithResetMeansExhausted() {
        val body = """{"userStatus":{"cascadeModelConfigData":{"clientModelConfigs":[{"label":"Claude Opus 4.6 (Thinking)","quotaInfo":{"resetTime":"2026-10-05T10:00:00Z"}}]}}}"""
        val m = AgParse.parse(body, now)!!.models.single()
        assertTrue(m.exhausted)
        assertEquals(100.0, m.percent, 0.0)
        assertEquals(AgFamily.CLAUDE, m.family)
    }

    @Test
    fun brokenOrUnknownShapesDoNotThrow() {
        assertNull(AgParse.parse("not json", now))
        assertNull(AgParse.parse("{}", now))
        val s = AgParse.parse("""{"cascadeModelConfigData":{"clientModelConfigs":[null,{"label":"X"},{"quotaInfo":{}},{"label":"Novo","quotaInfo":{"remainingFraction":"abc","resetTime":"amanhã"}}]}}""", now)
        assertNotNull(s)
        assertTrue(s!!.models.isEmpty())
    }

    @Test
    fun snapshotJsonHasNoPersonalData() {
        val s = AgParse.parse(fixture("userstatus-used.json"), now, quota = fixture("quota-pro.json"))!!
        val json = s.toJson().toString()
        assertFalse("example.com" in json)
        assertFalse("Fulano" in json)
        assertFalse("base64" in json)
        assertEquals(s, AgSnapshot.fromJson(JSONObject(json)))
    }

    @Test
    fun familiesFollowTheLabel() {
        assertEquals(AgFamily.FLASH, agFamilyOf("Gemini 3.8 Flash (High)"))
        assertEquals(AgFamily.PRO, agFamilyOf("Gemini 3.1 Pro (Low)"))
        assertEquals(AgFamily.CLAUDE, agFamilyOf("Claude Sonnet 4.6 (Thinking)"))
        assertEquals(AgFamily.GPT, agFamilyOf("GPT-OSS 120B (Medium)"))
        assertNull(agFamilyOf("Outro modelo"))
    }

    @Test
    fun oldPhonesReadTheEnvelopeAsBefore() {
        val claude = """{"five_hour":{"used_percentage":42,"resets_at":1791000000},"seven_day":{"used_percentage":61,"resets_at":1791500000}}"""
        val withAg = JSONObject(claude).put("antigravity", JSONObject().put("week", JSONObject().put("percent", 99.0)).put("models", org.json.JSONArray()))
        assertEquals(parsePush(JSONObject(claude), 1L), parsePush(withAg, 1L))
        assertEquals(Seal.level(JSONObject(claude)), Seal.level(withAg))
        assertEquals("0,0", Seal.level(JSONObject(claude)))
    }

    private fun phoneEnvelope(): JSONObject {
        val s = AgParse.parse(fixture("userstatus-used.json"), now, quota = fixture("quota-pro.json"))!!
        fun w(x: UsageWindow?): Any = x?.let { JSONObject().put("percent", it.percent).put("resetsAt", it.resetsAt ?: JSONObject.NULL) } ?: JSONObject.NULL
        val pools = org.json.JSONArray()
        s.groups.forEach { g -> pools.put(JSONObject().put("pool", g.pool.name).put("session", w(g.session)).put("week", w(g.week))) }
        val models = org.json.JSONArray()
        s.families().forEach { f -> models.put(JSONObject().put("label", f.label).put("percent", s.percent(f))) }
        return JSONObject().put("open", true).put("pools", pools).put("plan", s.plan).put("usage_at", now).put("models", models)
    }

    @Test
    fun levelCarriesAntigravityForThePush() {
        val claude = JSONObject("""{"five_hour":{"used_percentage":85},"seven_day":{"used_percentage":10}}""")
        assertEquals("80,0,0000", Seal.level(JSONObject(claude.toString()).put("antigravity", phoneEnvelope())))
        val hot = phoneEnvelope()
        hot.getJSONArray("pools").getJSONObject(0).put("session", JSONObject().put("percent", 91.0)).put("week", JSONObject().put("percent", 100.0))
        assertEquals("80,0,9100", Seal.level(JSONObject(claude.toString()).put("antigravity", hot)))
        assertEquals("80,0", Seal.level(JSONObject(claude.toString()).put("antigravity", JSONObject().put("off", true))))
        assertTrue(Seal.level(JSONObject(claude.toString()).put("antigravity", hot)).matches(Regex("^[0-9,]{0,16}$")))
    }

    @Test
    fun phoneReadsTheDesktopEnvelope() {
        val r = AgPush.parse(phoneEnvelope())!!
        assertTrue(r.open)
        assertEquals(42.0, r.snap.group(AgPool.GEMINI)!!.session!!.percent, 0.0)
        assertEquals(21.0, r.snap.group(AgPool.GEMINI)!!.week!!.percent, 0.0)
        assertEquals(listOf(AgFamily.PRO, AgFamily.FLASH, AgFamily.CLAUDE, AgFamily.GPT), r.snap.families())
        assertEquals(42.0, r.snap.percent(AgFamily.FLASH)!!, 0.0)
        assertEquals(AgPool.GEMINI, r.snap.worst(AlertKind.SESSION)!!.first)
        assertNull(AgPush.parse(JSONObject().put("off", true)))
        assertTrue(AgPush.off(JSONObject().put("off", true)))
        assertNull(AgPush.parse(null))
    }

    @Test
    fun phoneReadsTheOldDesktopEnvelope() {
        val old = JSONObject("""{"open":true,"plan":"Pro","usage_at":7,"pools":[
            {"pool":"GEMINI","percent":42,"resetsAt":99,"kind":"SESSION"},
            {"pool":"OTHERS","percent":5,"resetsAt":null,"kind":"WEEK"}],
            "models":[{"label":"Gemini Pro","percent":42},{"label":"Claude","percent":5}]}""")
        val r = AgPush.parse(old)!!
        assertEquals(UsageWindow(42.0, 99L), r.snap.group(AgPool.GEMINI)!!.session)
        assertNull(r.snap.group(AgPool.GEMINI)!!.week)
        assertEquals(UsageWindow(5.0, null), r.snap.group(AgPool.OTHERS)!!.week)
        assertEquals(7L, r.snap.fetchedAt)
    }

    @Test
    fun passedResetsSettleToZero() {
        val s = AgPush.parse(phoneEnvelope())!!.snap
        val later = s.settled(Instant.parse("2026-10-02T12:00:00Z").toEpochMilli())
        assertEquals(UsageWindow(0.0, null), later.group(AgPool.GEMINI)!!.session)
        assertEquals(21.0, later.group(AgPool.GEMINI)!!.week!!.percent, 0.0)
        assertTrue(s.settled(now) === s)
    }

    private fun model(label: String, remaining: Double, reset: String) =
        """{"userStatus":{"cascadeModelConfigData":{"clientModelConfigs":[{"label":"$label","quotaInfo":{"remainingFraction":$remaining,"resetTime":"$reset"}}]}}}"""
}
