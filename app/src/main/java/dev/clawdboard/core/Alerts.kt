package dev.clawdboard.core

enum class AlertKind { SESSION, WEEK }

data class Alert(val kind: AlertKind, val level: Int, val resetsAt: Long?)

val ALERT_LEVELS = intArrayOf(80, 90, 100)

fun nextAlert(kind: AlertKind, w: UsageWindow?, sent: Int): Pair<Alert?, Int> {
    if (w == null) return null to sent
    val level = ALERT_LEVELS.lastOrNull { w.percent >= it - 0.5 } ?: 0
    return when {
        level > sent -> Alert(kind, level, w.resetsAt) to level
        sent > 0 && w.percent < sent - 20 -> (if (sent >= 90) Alert(kind, 0, w.resetsAt) else null) to level
        else -> null to sent
    }
}
