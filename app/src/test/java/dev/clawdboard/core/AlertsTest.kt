package dev.clawdboard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertsTest {
    private fun run(vararg percents: Double): List<Int?> {
        var sent = 0
        return percents.map { p ->
            val (a, s) = nextAlert(AlertKind.SESSION, UsageWindow(p, 1L), sent)
            sent = s
            a?.level
        }
    }

    @Test
    fun warnsOncePerLevelAndWhenItFrees() {
        assertEquals(listOf(null, 80, null, 90, null, 100, null, 0, null), run(50.0, 81.0, 85.0, 90.0, 88.0, 99.6, 100.0, 3.0, 10.0))
    }

    @Test
    fun jumpingStraightToTheTopWarnsOnlyTheTop() {
        assertEquals(listOf(100), run(100.0))
    }

    @Test
    fun smallDropsDoNotWarnAgain() {
        assertEquals(listOf(90, null, null), run(91.0, 84.0, 91.0))
    }

    @Test
    fun belowNinetyFreesSilently() {
        assertEquals(listOf(80, null, 80), run(82.0, 40.0, 80.0))
    }

    @Test
    fun missingWindowKeepsTheMark() {
        val (a, s) = nextAlert(AlertKind.WEEK, null, 90)
        assertNull(a)
        assertEquals(90, s)
    }
}
