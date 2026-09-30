@file:OptIn(ExperimentalLayoutApi::class)

package dev.clawdboard.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.clawdboard.core.Language
import dev.clawdboard.core.MODELS
import dev.clawdboard.core.NewsItem
import dev.clawdboard.core.ScopedLimit
import dev.clawdboard.core.Skin
import dev.clawdboard.core.Species
import dev.clawdboard.core.StatusSnapshot
import dev.clawdboard.core.Tint
import dev.clawdboard.core.UsageWindow
import dev.clawdboard.core.feelOf
import dev.clawdboard.core.settled
import dev.clawdboard.core.txt
import dev.clawdboard.ui.C
import dev.clawdboard.ui.LocalLook
import dev.clawdboard.ui.Look
import dev.clawdboard.ui.Mascot
import dev.clawdboard.ui.fmtAgo
import dev.clawdboard.ui.fmtAt
import dev.clawdboard.ui.fmtDateLong
import dev.clawdboard.ui.fmtLeft
import dev.clawdboard.ui.fmtPct
import dev.clawdboard.ui.fmtShortDate
import dev.clawdboard.ui.weekShort
import dev.clawdboard.ui.zoned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI

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
    val widget: Boolean,
    val setWidget: (Boolean) -> Unit,
)

private val LAV = Color(0xFFB9A6F2)

internal val newsFlow = MutableStateFlow<List<NewsItem>?>(null)

@Composable
internal fun Panel(port: Int, status: StatusSnapshot?, controls: WidgetControls) {
    val snap by usage.collectAsState()
    val at by pushedAt.collectAsState()
    val p by prefsFlow.collectAsState()
    val history by DeskHistory.samples.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val news by newsFlow.collectAsState()
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    LaunchedEffect(Unit) { while (true) { withContext(Dispatchers.IO) { DeskNews.fetch() }?.let { newsFlow.value = it }; delay(3_600_000L) } }
    val u = snap?.settled(now)
    val look = Look(skin = p.skin, tint = p.tint, animations = p.animations, species = p.mascot())
    val t = zoned(now)
    CompositionLocalProvider(LocalLook provides look) {
        Box(Modifier.fillMaxSize().background(C.bg).verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 1180.dp).padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = if (onMac) 40.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Mascot(Modifier.width(64.dp), reserveTop = false)
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text("Banditboard", color = C.text, fontSize = 30.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
                        Text(if (onMac) txt.macPanelSubtitle else txt.panelSubtitle, color = C.muted, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("%02d:%02d".format(t.hour, t.minute), color = C.text, fontSize = 36.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
                        Text(fmtDateLong(t), color = C.muted, fontSize = 14.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    UsageCard(Modifier.weight(1f), txt.session, txt.sessionWindow, u?.fiveHour, emptyList(), now)
                    UsageCard(Modifier.weight(1f), txt.week, txt.weekWindow, u?.sevenDay, u?.scoped.orEmpty(), now)
                }
                Card(Modifier.fillMaxWidth(), txt.panelModels) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        MODELS.forEachIndexed { i, m ->
                            val own = u?.scoped?.firstOrNull { it.label.contains(m, ignoreCase = true) }
                            val pct = own?.percent ?: u?.sevenDay?.percent
                            val down = status?.down?.contains(m) == true
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(fmtPct(pct), color = if (own != null && pct != null) C.level(pct) else C.muted, fontSize = 22.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
                                Bar(pct, if (own != null && pct != null) C.level(pct) else C.dim, Modifier.fillMaxWidth(0.8f), 7)
                                Mascot(Modifier.fillMaxWidth(0.55f), model = m, alive = !down, seed = i, feel = feelOf(u, m))
                                Text(m, color = if (down) C.bad else C.text, fontSize = 18.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Text(txt.panelModelsHint, color = C.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Card(Modifier.weight(1f), "Status", "status.claude.com") {
                        when {
                            status == null -> Text(txt.checkingStatus, color = C.muted, fontSize = 14.sp)
                            status.incidents.isEmpty() -> Text("● ${txt.allOperational}", color = C.ok, fontSize = 14.sp)
                            else -> status.incidents.take(4).forEach { Text("● ${it.name}", color = C.warn, fontSize = 14.sp, modifier = Modifier.clickable { open(it.url) }) }
                        }
                    }
                    Card(Modifier.weight(1f), txt.panelNews, "anthropic.com/news") {
                        val list = news
                        if (list == null) Text(txt.loadingNews, color = C.muted, fontSize = 14.sp)
                        else list.take(4).forEach { n ->
                            Row(Modifier.fillMaxWidth().clickable { open(n.link) }, verticalAlignment = Alignment.Top) {
                                Text(n.title, color = C.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text(fmtShortDate(n.date), color = C.dim, fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                    }
                }
                Card(Modifier.fillMaxWidth(), txt.last7Days) {
                    Chart(history.map { Triple(it.t, it.p5, it.p7) }, now)
                    val peak = history.mapNotNull { it.p5 }.maxOrNull()
                    Text(
                        if (history.isEmpty()) txt.noSamples else listOfNotNull(txt.samples(history.size), peak?.let { txt.peak5h(fmtPct(it)) }).joinToString(" · "),
                        color = C.dim, fontSize = 12.sp,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Card(Modifier.weight(0.8f), txt.panelClaude) {
                        if (u != null) Text(txt.updatedAgo(fmtAgo(now - at)), color = C.ok, fontSize = 14.sp)
                        Connect(port, hint = u == null)
                    }
                    Card(Modifier.weight(1.2f), txt.panelWidget) {
                        Label(txt.trayLayout)
                        Pills(listOfNotNull(Layout.FULL to txt.layoutFull, Layout.COMPACT to txt.trayCompact, if (onMac) null else Layout.MINI to txt.layoutMini), controls.layout, controls.setLayout)
                        if (onMac) Toggle(txt.macWidget, controls.widget, controls.setWidget)
                        Toggle(txt.trayOnTop, controls.onTop, controls.setOnTop)
                        if (!onMac) Toggle(txt.trayTaskbar, controls.taskbar, controls.setTaskbar)
                        controls.autostart?.let { Toggle(if (onMac) txt.macAutostart else txt.trayAutostart, it, controls.setAutostart) }
                        if (!onMac) Toggle(txt.trayMusic, controls.dance, controls.setDance)
                        Toggle(txt.alerts, p.alerts) { v -> savePrefs { it.copy(alerts = v) } }
                        Label(txt.mascot)
                        Pills(Species.entries.filter { it != Species.CLAWD || p.clawdUnlocked }.map { it to txt.label(it) }, p.mascot()) { s -> savePrefs { it.copy(species = s) } }
                        Label(txt.accessories)
                        Pills(Skin.entries.map { it to txt.label(it) }, p.skin) { s -> savePrefs { it.copy(skin = s) } }
                        Label(txt.color)
                        Pills(Tint.entries.map { it to txt.label(it) }, p.tint) { v -> savePrefs { it.copy(tint = v) } }
                        Label(txt.language)
                        Pills(Language.entries.map { it to txt.label(it) }, p.language) { v -> savePrefs { it.copy(language = v) } }
                    }
                }
                val sharing by Share.on.collectAsState()
                Card(Modifier.fillMaxWidth(), txt.shareTitle) {
                    Toggle(txt.shareToggle, sharing, Share::set)
                    if (sharing) Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
                        Qr(Modifier.size(170.dp), Share.pairUri())
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(txt.shareHint, color = C.muted, fontSize = 13.sp)
                            Share.addresses().forEach { Text("http://$it:${Share.port}", color = C.dim, fontSize = 12.sp) }
                        }
                    } else Text(txt.shareOff, color = C.dim, fontSize = 12.sp)
                }
                Card(Modifier.fillMaxWidth(), txt.credits) {
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
                        Qr(Modifier.size(128.dp).clickable { Promo.open() })
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Banditboard ${System.getProperty("jpackage.app-version").orEmpty()}".trim(), color = C.text, fontSize = 18.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
                            Text(txt.createdBy, color = C.clawd, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(txt.mascotRacco, color = C.muted, fontSize = 13.sp)
                            Text(txt.promoBody, color = C.muted, fontSize = 13.sp)
                            Text(txt.promoScan, color = C.dim, fontSize = 12.sp)
                            Text(txt.fanProject, color = C.dim, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun open(url: String?) {
    if (url.isNullOrBlank()) return
    runCatching { Desktop.getDesktop().browse(URI(url)) }
}

@Composable
private fun Card(modifier: Modifier, title: String, sub: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.clip(shape).background(C.card).border(1.dp, C.line, shape).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(title, color = C.clawd, fontSize = 19.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
            sub?.let { Text(it, color = C.dim, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp, bottom = 2.dp)) }
        }
        content()
    }
}

@Composable
private fun Bar(pct: Double?, color: Color, modifier: Modifier, height: Int) {
    Box(modifier.height(height.dp).clip(CircleShape).background(C.track)) {
        if (pct != null) Box(Modifier.fillMaxWidth((pct / 100).toFloat().coerceIn(0.02f, 1f)).height(height.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun UsageCard(modifier: Modifier, title: String, window: String, w: UsageWindow?, scoped: List<ScopedLimit>, now: Long) {
    val shape = RoundedCornerShape(18.dp)
    val pct = w?.percent
    Column(modifier.clip(shape).background(C.card).border(1.dp, C.line, shape).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(title, color = C.text, fontSize = 22.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
            Text(window, color = C.dim, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, bottom = 3.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(fmtPct(pct), color = pct?.let { C.level(it) } ?: C.dim, fontSize = 72.sp, fontFamily = Fredoka, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            w?.resetsAt?.let {
                Column(horizontalAlignment = Alignment.End) {
                    Text(txt.resetsIn, color = C.muted, fontSize = 13.sp)
                    Text(fmtLeft(it - now), color = C.text, fontSize = 30.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
                    Text(fmtAt(it, now), color = C.muted, fontSize = 13.sp)
                }
            }
        }
        Bar(pct, pct?.let { C.level(it) } ?: C.dim, Modifier.fillMaxWidth(), 14)
        scoped.forEach { s -> Text("● ${s.label} ${fmtPct(s.percent)}", color = C.level(s.percent), fontSize = 13.sp) }
    }
}

@Composable
private fun Chart(points: List<Triple<Long, Double?, Double?>>, now: Long) {
    val measurer = rememberTextMeasurer()
    val label = TextStyle(color = C.dim, fontSize = 11.sp)
    val grid = C.line
    val coral = C.clawd
    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        Text("— ${txt.session}", color = coral, fontSize = 12.sp)
        Spacer(Modifier.width(14.dp))
        Text("— ${txt.week}", color = LAV, fontSize = 12.sp)
    }
    Canvas(Modifier.fillMaxWidth().height(230.dp)) {
        val left = 44f
        val bottom = size.height - 22f
        val w = size.width - left
        val start = now - 7 * 86_400_000L
        fun x(t: Long) = left + (t - start).toFloat() / (7 * 86_400_000f) * w
        fun y(p: Double) = bottom - (p / 100f).toFloat() * bottom
        listOf(0, 25, 50, 75, 100).forEach { v ->
            drawLine(grid, Offset(left, y(v.toDouble())), Offset(size.width, y(v.toDouble())), 1f)
            drawText(measurer, "$v%", Offset(0f, y(v.toDouble()) - 8f), label)
        }
        var day = zoned(start).toLocalDate().plusDays(1)
        while (true) {
            val t = day.atStartOfDay(zoned(now).zone).toInstant().toEpochMilli()
            if (t > now) break
            drawLine(grid, Offset(x(t), 0f), Offset(x(t), bottom), 1f)
            drawText(measurer, "${weekShort(day.dayOfWeek)} ${day.dayOfMonth}", Offset(x(t) + 4f, bottom + 4f), label)
            day = day.plusDays(1)
        }
        fun series(pick: (Triple<Long, Double?, Double?>) -> Double?, color: Color) {
            var prev: Pair<Long, Double>? = null
            points.forEach { pt ->
                val v = pick(pt) ?: run { prev = null; return@forEach }
                val last = prev
                if (last != null && pt.first - last.first <= 2 * 3_600_000L) {
                    drawLine(color, Offset(x(last.first), y(last.second)), Offset(x(pt.first), y(v)), 4f, StrokeCap.Round)
                }
                prev = pt.first to v
            }
        }
        series({ it.third }, LAV)
        series({ it.second }, coral)
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
internal fun Toggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = C.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = C.clawd))
    }
}
