package dev.clawdboard.desktop

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.clawdboard.core.Act
import dev.clawdboard.core.ActivityTracker
import dev.clawdboard.core.ClaudeSession
import dev.clawdboard.core.React
import dev.clawdboard.core.primary
import dev.clawdboard.core.reactFor
import dev.clawdboard.core.soloModel
import dev.clawdboard.core.describe
import dev.clawdboard.core.txt
import dev.clawdboard.ui.C
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import kotlin.concurrent.thread

const val NOTIFY_AFTER_MS = 6_000L

object Claude {
    private val tracker = ActivityTracker()
    val sessions = MutableStateFlow<List<ClaudeSession>>(emptyList())
    val watching = MutableStateFlow(Hook.watching)
    val showFile = MutableStateFlow(Store.get("activityFile") == "on")
    val notifying = MutableStateFlow(Store.get("activityNotify") != "off")
    var notify: ((String, String) -> Unit)? = null

    fun onHook(o: JSONObject) {
        if (!watching.value) return
        val now = System.currentTimeMillis()
        val before = sessions.value.filter { it.attention }.map { it.id to it.since }.toSet()
        if (!tracker.onHook(o, now)) return
        val list = tracker.sessions(now)
        publish(list)
        list.filter { it.attention && (it.id to it.since) !in before }.forEach { s -> later(s.id, s.since) }
    }

    private fun later(id: String, since: Long) = thread(isDaemon = true) {
        Thread.sleep(NOTIFY_AFTER_MS)
        val s = sessions.value.firstOrNull { it.id == id && it.since == since && it.attention } ?: return@thread
        if (watching.value && notifying.value) notify?.invoke(txt.activityNeedsYou, line(s, showFile.value))
    }

    fun setNotifying(on: Boolean) {
        Store.put("activityNotify", if (on) "on" else "off")
        notifying.value = on
    }

    @Volatile
    var changedAt = 0L
        private set

    private fun publish(list: List<ClaudeSession>) {
        if (list == sessions.value) return
        sessions.value = list
        changedAt = System.currentTimeMillis()
        Forward.changed()
    }

    fun tick() {
        if (watching.value) publish(tracker.sessions(System.currentTimeMillis()))
    }

    fun setWatching(on: Boolean, port: Int): Boolean {
        val ok = Hook.install(port, if (on) "on" else "off")
        if (ok) {
            Store.put("activity", if (on) "on" else "off")
            watching.value = on
            if (!on) sessions.value = emptyList()
            changedAt = System.currentTimeMillis()
            Forward.changed()
        }
        return ok
    }

    fun setShowFile(on: Boolean) {
        Store.put("activityFile", if (on) "on" else "off")
        showFile.value = on
    }

    fun react(model: String?, list: List<ClaudeSession>, now: Long, on: Boolean): React = reactFor(model, if (on) list else null, now)

    fun solo(list: List<ClaudeSession>): String? = list.soloModel()

    fun line(s: ClaudeSession, file: Boolean): String = s.describe(file)
}

fun React.bubble() = this == React.ALERT || this == React.RUN || this == React.OOPS

@Composable
fun ActivityLine(fontSize: TextUnit = 11.sp, modifier: Modifier = Modifier) {
    val on by Claude.watching.collectAsState()
    val list by Claude.sessions.collectAsState()
    val file by Claude.showFile.collectAsState()
    if (!on) return
    val s = list.primary()
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val dot = when {
            s == null || s.act == Act.IDLE -> P.dim
            s.attention -> C.warn
            s.act == Act.ERROR -> C.bad
            s.act == Act.FINISHED -> C.ok
            else -> P.accent
        }
        androidx.compose.foundation.layout.Box(Modifier.size(7.dp).background(dot, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            s?.let { Claude.line(it, file) } ?: txt.activityNone,
            color = if (s?.attention == true) C.warn else P.muted, fontSize = fontSize, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
