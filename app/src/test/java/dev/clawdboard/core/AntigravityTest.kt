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
        assertNull(s.session)
        assertNull(s.week)
        assertEquals("Google AI Plus", s.plan)
    }

    @Test
    fun usedGeminiPoolBecomesWeek() {
        val s = AgParse.parse(fixture("userstatus-used.json"), now)!!
        val reset = Instant.parse("2026-10-09T10:43:36Z").toEpochMilli()
        assertEquals(0.4, s.family(AgFamily.FLASH)!!.percent, 0.001)
        assertEquals(0.4, s.family(AgFamily.PRO)!!.percent, 0.001)
        assertEquals(0.0, s.family(AgFamily.CLAUDE)!!.percent, 0.001)
        assertEquals(0.0, s.family(AgFamily.GPT)!!.percent, 0.001)
        assertEquals(reset, s.week!!.resetsAt)
        assertNull(s.session)
    }

    @Test
    fun poolsMatchViewUsage() {
        val s = AgParse.parse(fixture("userstatus-used.json"), now)!!
        assertEquals(0.4, s.pool(AgPool.GEMINI)!!.percent, 0.001)
        assertEquals(AlertKind.WEEK, s.pool(AgPool.GEMINI)!!.kind)
        assertEquals(0.0, s.pool(AgPool.OTHERS)!!.percent, 0.001)
        assertNull(s.pool(AgPool.OTHERS)!!.resetsAt)
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
        val s = AgParse.parse(fixture("userstatus-used.json"), now)!!
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
    }

    private fun model(label: String, remaining: Double, reset: String) =
        """{"userStatus":{"cascadeModelConfigData":{"clientModelConfigs":[{"label":"$label","quotaInfo":{"remainingFraction":$remaining,"resetTime":"$reset"}}]}}}"""
}
