@file:OptIn(ExperimentalLayoutApi::class)

package dev.clawdboard.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.clawdboard.core.Language
import dev.clawdboard.core.MODELS
import dev.clawdboard.core.Skin
import dev.clawdboard.core.Species
import dev.clawdboard.core.StatusSnapshot
import dev.clawdboard.core.Tint
import dev.clawdboard.core.feelOf
import dev.clawdboard.core.settled
import dev.clawdboard.core.txt
import dev.clawdboard.ui.C
import dev.clawdboard.ui.LocalLook
import dev.clawdboard.ui.Look
import dev.clawdboard.ui.Mascot
import dev.clawdboard.ui.fmtAgo
import dev.clawdboard.ui.fmtDateLong
import dev.clawdboard.ui.fmtPct
import dev.clawdboard.ui.zoned
import kotlinx.coroutines.delay

class WidgetControls(
    val layout: Layout,
    val setLayout: (Layout) -> Unit,
    val onTop: Boolean,
    val setOnTop: (Boolean) -> Unit,
    val taskbar: Boolean,
    val setTaskbar: (Boolean) -> Unit,
    val autostart: Boolean?,
    val setAutostart: (Boolean) -> Unit,
    val dance: Boolean,
    val setDance: (Boolean) -> Unit,
)

@Composable
internal fun Panel(port: Int, status: StatusSnapshot?, controls: WidgetControls) {
    val snap by usage.collectAsState()
    val at by pushedAt.collectAsState()
    val p by prefsFlow.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val u = snap?.settled(now)
    val look = Look(skin = p.skin, tint = p.tint, animations = p.animations, species = p.mascot())
    val t = zoned(now)
    CompositionLocalProvider(LocalLook provides look) {
        Row(Modifier.fillMaxSize().background(C.bg).padding(28.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("%02d:%02d".format(t.hour, t.minute), color = C.text, fontSize = 52.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
                    Text(fmtDateLong(t), color = C.muted, fontSize = 15.sp, modifier = Modifier.padding(bottom = 10.dp))
                }
                Meter(txt.session, u?.fiveHour, now)
                Meter(txt.week, u?.sevenDay, now)
                u?.scoped?.forEach { s -> Text("${s.label} ${fmtPct(s.percent)}", color = C.level(s.percent), fontSize = 14.sp) }
                if (u == null) Connect(port) else Text(txt.updatedAgo(fmtAgo(now - at)), color = C.dim, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MODELS.forEachIndexed { i, m ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            val down = status?.down?.contains(m) == true
                            Mascot(Modifier.fillMaxWidth(), model = m, alive = !down, seed = i, feel = feelOf(u, m))
                            Text(m, color = if (down) C.bad else C.muted, fontSize = 13.sp, fontFamily = Fredoka)
                        }
                    }
                }
                status?.incidents?.take(3)?.forEach { Text("• ${it.name}", color = C.warn, fontSize = 13.sp) }
            }
            Column(
                Modifier.weight(0.9f).fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(C.card).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(txt.panelWidget, color = C.text, fontSize = 18.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
                Label(txt.trayLayout)
                Pills(listOf(Layout.FULL to txt.layoutFull, Layout.COMPACT to txt.trayCompact, Layout.MINI to txt.layoutMini), controls.layout, controls.setLayout)
                Toggle(txt.trayOnTop, controls.onTop, controls.setOnTop)
                Toggle(txt.trayTaskbar, controls.taskbar, controls.setTaskbar)
                controls.autostart?.let { Toggle(txt.trayAutostart, it, controls.setAutostart) }
                Toggle(txt.trayMusic, controls.dance, controls.setDance)
                Toggle(txt.alerts, p.alerts) { v -> savePrefs { it.copy(alerts = v) } }
                Label(txt.mascot)
                Pills(Species.entries.filter { it != Species.CLAWD || p.clawdUnlocked }.map { it to txt.label(it) }, p.mascot()) { s -> savePrefs { it.copy(species = s) } }
                Label(txt.accessories)
                Pills(Skin.entries.map { it to txt.label(it) }, p.skin) { s -> savePrefs { it.copy(skin = s) } }
                Label(txt.color)
                Pills(Tint.entries.map { it to txt.label(it) }, p.tint) { v -> savePrefs { it.copy(tint = v) } }
                Label(txt.language)
                Pills(Language.entries.map { it to txt.label(it) }, p.language) { v -> savePrefs { it.copy(language = v) } }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = C.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun <T> Pills(options: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, name) ->
            val on = value == selected
            Text(
                name, color = if (on) C.bg else C.muted, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) C.text else C.card2).clickable { onPick(value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun Toggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = C.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = C.clawd))
    }
}
