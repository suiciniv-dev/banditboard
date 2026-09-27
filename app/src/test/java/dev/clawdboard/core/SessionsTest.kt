package dev.clawdboard.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionsTest {
    private fun push(sessions: String?) = parsePush(
        JSONObject("""{"five_hour":{"used_percentage":40}${sessions?.let { ",\"sessions\":$it" } ?: ""}}"""), 1_000L,
    )!!

    @Test
    fun oneSessionPicksItsModel() {
        assertEquals("Opus", push("""["opus"]""").soloModel(1_000L))
        assertEquals("Fable", push("""["fable"]""").soloModel(1_000L))
    }

    @Test
    fun severalSessionsOrUnknownModelShowTheRegularRacco() {
        assertNull(push("""["opus","sonnet"]""").soloModel(1_000L))
        assertNull(push("""[""]""").soloModel(1_000L))
        assertNull(push(null).soloModel(1_000L))
    }

    @Test
    fun anOldPushNoLongerCounts() {
        assertNull(push("""["opus"]""").soloModel(1_000L + ACTIVE_MS + 1))
    }
}
