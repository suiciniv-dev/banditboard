package dev.clawdboard.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.clawdboard.core.Accessory
import dev.clawdboard.core.AgFamily
import dev.clawdboard.core.AlertKind
import dev.clawdboard.core.Backdrop
import dev.clawdboard.core.MODELS
import dev.clawdboard.core.Prefs
import dev.clawdboard.core.Repository
import dev.clawdboard.core.Tool
import dev.clawdboard.core.ToolTheme
import dev.clawdboard.core.UsageWindow
import dev.clawdboard.core.agFeel
import dev.clawdboard.core.feelOf
import dev.clawdboard.core.reactFor
import dev.clawdboard.core.settled
import dev.clawdboard.core.txt

fun agTheme(prefs: Prefs): ToolTheme = if (prefs.backdrop == Backdrop.BLACK) ToolTheme.BLACK else prefs.toolTheme

fun agPalette(prefs: Prefs): Palette = C.palette(Tool.ANTIGRAVITY, agTheme(prefs))

fun claudePalette(): Palette = Palette(C.bg, C.card, C.card2, C.line, C.track, C.text, C.muted, C.dim, C.glow, C.clawd)

@Composable
private fun PBar(percent: Double?, pal: Palette, modifier: Modifier = Modifier, height: Dp = 14.dp) {
    val target = ((percent ?: 0.0) / 100.0).coerceIn(0.0, 1.0).toFloat()
    val anim by animateFloatAsState(target, tween(900), label = "bar")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(pal.track)) {
        if (anim > 0f) Box(Modifier.fillMaxHeight().fillMaxWidth(anim).clip(RoundedCornerShape(50)).background(percent?.let { C.level(it) } ?: pal.dim))
    }
}

private fun agStatusLine(st: Repository.State, now: Long): String {
    val ag = st.ag ?: return txt.agWaitingPhone
    val ago = fmtAgo(now - ag.snap.fetchedAt)
    return if (ag.open) listOfNotNull(ag.snap.plan?.let { txt.agPlan(it) }, ago).joinToString(" · ") else "${txt.agClosed} · $ago"
}

@Composable
fun AgPage(st: Repository.State, pal: Palette, landscape: Boolean, compact: Boolean) {
    val now = LocalNow.current
    val s = st.ag?.snap?.settled(now)
    if (s == null) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Mascot(Modifier.width(120.dp), wear = Accessory.STAR, reserveTop = false, feel = agFeel(null))
            Spacer(Modifier.height(16.dp))
            Text(txt.toolAntigravity, color = pal.text, fontSize = 24.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
            Text(txt.agWaitingPhone, color = pal.muted, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
        return
    }
    val rows = agRows(s).map { (kind, title, w) -> Triple(title, if (kind == AlertKind.SESSION) txt.hours5 else txt.days7, w) }
    val many = rows.size > 2
    val status = agStatusLine(st, now)
    if (landscape) {
        Column(Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, top = if (compact) 10.dp else 16.dp, bottom = 28.dp)) {
            Header(txt.toolAntigravity, pal.accent, status, pal)
            Column(
                (if (compact) Modifier.padding(vertical = 6.dp) else Modifier.weight(if (many) 1.3f else 1f)).fillMaxWidth(),
                verticalArrangement = if (compact) Arrangement.spacedBy(4.dp) else if (many) Arrangement.spacedBy(6.dp, Alignment.CenterVertically) else Arrangement.SpaceEvenly,
            ) {
                rows.forEach { (title, window, w) -> AgWideRow(title, window, w, pal, compact || many, many) }
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(pal.line))
            Row(
                Modifier.weight(if (compact) 1f else 1.15f).fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                s.families().forEachIndexed { i, f -> AgMascot(f, i, s.percent(f), pal, Modifier.weight(1f), 132.dp, compact) }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 32.dp)) {
            Header(txt.toolAntigravity, pal.accent, status, pal)
            val big = if (compact || many) 48.sp else 64.sp
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.SpaceEvenly) {
                rows.forEach { (title, window, w) -> AgBlock(title, window, w, pal, big, compact) }
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(pal.line))
            Column(Modifier.weight(1.1f).fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                s.families().chunked(2).forEachIndexed { r, pair ->
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        pair.forEachIndexed { c, f -> AgMascot(f, r * 2 + c, s.percent(f), pal, Modifier.weight(1f), 128.dp, compact) }
                    }
                }
            }
        }
    }
}

private fun agRows(s: dev.clawdboard.core.AgSnapshot): List<Triple<AlertKind, String, UsageWindow>> = s.groups.flatMap { g ->
    g.windows().map { (kind, w) -> Triple(kind, "${if (kind == AlertKind.SESSION) txt.session else txt.week} (${txt.label(g.pool)})", w) }
}

@Composable
private fun Header(name: String, dot: Color, right: String, pal: Palette) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(8.dp))
        Text(name, color = pal.text, fontSize = 18.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text(right, color = pal.dim, fontSize = 12.sp, maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun AgWideRow(title: String, window: String, w: UsageWindow, pal: Palette, compact: Boolean, many: Boolean) {
    val now = LocalNow.current
    if (compact) {
        val pct = if (many) 26.sp else 36.sp
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = pal.text, fontSize = if (many) 16.sp else 17.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.width(228.dp))
            Text(fmtPct(w.percent), color = C.level(w.percent), fontSize = pct, fontFamily = Fredoka, fontWeight = FontWeight.Bold, lineHeight = pct, maxLines = 1, modifier = Modifier.width(96.dp))
            PBar(w.percent, pal, Modifier.weight(1f), 14.dp)
            Text(
                w.resetsAt?.let { fmtLeft(it - now) } ?: "--", color = pal.text, fontSize = 20.sp, fontFamily = Fredoka,
                fontWeight = FontWeight.Medium, maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.width(110.dp),
            )
        }
        return
    }
    val pct = if (many) 40.sp else 58.sp
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(260.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(title, color = pal.text, fontSize = 20.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(Modifier.width(6.dp))
                Text(window, color = pal.dim, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(bottom = 2.dp))
            }
            Text(fmtPct(w.percent), color = C.level(w.percent), fontSize = pct, fontFamily = Fredoka, fontWeight = FontWeight.Bold, lineHeight = pct)
        }
        PBar(w.percent, pal, Modifier.weight(1f), 18.dp)
        Spacer(Modifier.width(22.dp))
        Column(Modifier.width(150.dp), horizontalAlignment = Alignment.End) {
            val reset = w.resetsAt
            if (reset != null) {
                Text(txt.resetsIn, color = pal.muted, fontSize = 12.sp)
                Text(fmtLeft(reset - now), color = pal.text, fontSize = 24.sp, fontFamily = Fredoka, fontWeight = FontWeight.Medium)
                Text(fmtAt(reset, now), color = pal.muted, fontSize = 12.sp)
            } else {
                Text(txt.noReset, color = pal.dim, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AgBlock(title: String, window: String, w: UsageWindow, pal: Palette, big: androidx.compose.ui.unit.TextUnit, compact: Boolean) {
    val now = LocalNow.current
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(title, color = pal.text, fontSize = 20.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text(window, color = pal.dim, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(bottom = 2.dp))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(fmtPct(w.percent), color = C.level(w.percent), fontSize = big, fontFamily = Fredoka, fontWeight = FontWeight.Bold, lineHeight = big)
            Spacer(Modifier.weight(1f))
            val reset = w.resetsAt
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 8.dp)) {
                if (reset != null) {
                    if (!compact) Text(txt.resetsIn, color = pal.muted, fontSize = 12.sp)
                    Text(fmtLeft(reset - now), color = pal.text, fontSize = 24.sp, fontFamily = Fredoka, fontWeight = FontWeight.Medium)
                    Text(fmtAt(reset, now), color = pal.muted, fontSize = 12.sp)
                } else {
                    Text(txt.noResetScheduled, color = pal.dim, fontSize = 13.sp)
                }
            }
        }
        PBar(w.percent, pal)
    }
}

@Composable
private fun AgMascot(f: AgFamily, index: Int, pct: Double?, pal: Palette, modifier: Modifier, maxMascot: Dp, compact: Boolean) {
    val out = pct != null && pct >= 99.5
    BoxWithConstraints(modifier.fillMaxHeight()) {
        val barW = minOf(maxMascot * 0.8f, maxWidth * 0.7f)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = if (compact) Arrangement.Center else Arrangement.Bottom) {
            Text(fmtPct(pct), color = pct?.let { C.level(it) } ?: pal.dim, fontSize = 18.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            PBar(pct, pal, Modifier.width(barW), 8.dp)
            Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
            BoxWithConstraints(Modifier.weight(1f, fill = false)) {
                Mascot(Modifier.width(minOf(maxMascot, maxWidth * 0.85f, maxHeight * MASCOT_ASPECT)), seed = index + 31, wear = f.accessory, feel = agFeel(pct))
            }
            Spacer(Modifier.height(if (compact) 6.dp else 8.dp))
            Text(if (compact) f.short else f.label, color = if (out) C.bad else pal.text, fontSize = 17.sp, fontFamily = Fredoka, fontWeight = FontWeight.Medium, maxLines = 1)
            if (!compact) Text(
                when {
                    out -> txt.exhausted
                    pct != null && pct >= 85 -> txt.agNearLimit
                    else -> " "
                },
                color = if (out) C.bad else pal.muted, fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun BothPage(st: Repository.State, pal: Palette, landscape: Boolean) {
    val now = LocalNow.current
    val claude = claudePalette()
    val u = st.usage
    val s = st.ag?.snap?.settled(now)
    val claudeAge = st.lastPushAt?.let { fmtAgo(now - it) } ?: txt.toolNoData
    val agAge = st.ag?.let { if (it.open) fmtAgo(now - it.snap.fetchedAt) else txt.agClosed } ?: txt.toolNoData
    val list = LocalClaude.current
    val claudeRaccos: @Composable RowScope.() -> Unit = {
        MODELS.forEachIndexed { i, m ->
            val own = u?.scoped?.firstOrNull { it.label.contains(m, ignoreCase = true) }
            val pct = own?.percent ?: u?.sevenDay?.percent
            SmallRacco(m, if (own != null && pct != null) C.level(pct) else claude.muted, pct, claude, Modifier.weight(1f)) {
                Mascot(Modifier.fillMaxWidth(0.8f), model = m, seed = i + 41, feel = feelOf(u, m, reactFor(m, list, now)))
            }
        }
    }
    val agRaccos: @Composable RowScope.() -> Unit = {
        (s?.families() ?: AgFamily.entries).forEachIndexed { i, f ->
            val pct = s?.percent(f)
            SmallRacco(if (f == AgFamily.FLASH) f.short else f.label, pct?.let { C.level(it) } ?: pal.muted, pct, pal, Modifier.weight(1f)) {
                Mascot(Modifier.fillMaxWidth(0.8f), seed = i + 51, wear = f.accessory, feel = agFeel(pct))
            }
        }
    }
    val halves: @Composable (Modifier, Modifier) -> Unit = { a, b ->
        Half(a, txt.toolClaude, claude, claudeAge, listOf(txt.session to u?.fiveHour, txt.week to u?.sevenDay), claudeRaccos)
        val agLines: List<Pair<String, UsageWindow?>> = s?.let { snap -> agRows(snap).map { (_, title, w) -> title to w } } ?: listOf(txt.session to null, txt.week to null)
        Half(b, txt.toolAntigravity, pal, agAge, agLines, agRaccos)
    }
    if (landscape) Row(Modifier.fillMaxSize()) { halves(Modifier.weight(1f), Modifier.weight(1f)) }
    else Column(Modifier.fillMaxSize()) { halves(Modifier.weight(1f), Modifier.weight(1f)) }
}

@Composable
private fun Half(modifier: Modifier, name: String, pal: Palette, age: String, rows: List<Pair<String, UsageWindow?>>, raccos: @Composable RowScope.() -> Unit) {
    val now = LocalNow.current
    Column(modifier.fillMaxSize().background(pal.bg).padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 30.dp)) {
        Header(name, pal.accent, age, pal)
        Spacer(Modifier.height(14.dp))
        val many = rows.size > 2
        val pct = if (many) 22.sp else 30.sp
        rows.forEach { (label, w) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(label, color = pal.text, fontSize = if (many) 13.sp else 15.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(bottom = 4.dp))
                Spacer(Modifier.width(8.dp))
                Text(fmtPct(w?.percent), color = w?.percent?.let { C.level(it) } ?: pal.dim, fontSize = pct, fontFamily = Fredoka, fontWeight = FontWeight.Bold, lineHeight = pct)
                Spacer(Modifier.weight(1f))
                Text(w?.resetsAt?.let { "${txt.resetsIn} ${fmtLeft(it - now)}" } ?: " ", color = pal.muted, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
            }
            Spacer(Modifier.height(4.dp))
            PBar(w?.percent, pal, height = if (many) 7.dp else 10.dp)
            Spacer(Modifier.height(if (many) 7.dp else 12.dp))
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, content = raccos)
    }
}

@Composable
private fun SmallRacco(name: String, color: Color, pct: Double?, pal: Palette, modifier: Modifier, mascot: @Composable () -> Unit) {
    Column(modifier.widthIn(max = 96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) { mascot() }
        Text(name, color = pal.text, fontSize = 12.sp, fontFamily = Fredoka, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(fmtPct(pct), color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ToolPicker(repo: Repository, prefs: Prefs) {
    var claude by remember { mutableStateOf(prefs.showClaude) }
    var ag by remember { mutableStateOf(prefs.showAg) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > 560.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(txt.pickTitle, color = C.text, fontSize = 26.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(txt.pickSubtitle, color = C.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            val cards: @Composable (Modifier) -> Unit = { m ->
                PickCard(m, txt.toolClaude, txt.pickClaudeHint, claudePalette(), claude, { claude = it }) {
                    Mascot(Modifier.width(96.dp), model = "Fable", reserveTop = true)
                }
                PickCard(m, txt.toolAntigravity, txt.pickAgHint, C.palette(Tool.ANTIGRAVITY, agTheme(prefs)), ag, { ag = it }) {
                    Mascot(Modifier.width(96.dp), wear = Accessory.STAR, reserveTop = true)
                }
            }
            if (wide) Row(Modifier.widthIn(max = 760.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) { cards(Modifier.weight(1f)) }
            else Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) { cards(Modifier.fillMaxWidth()) }
            Spacer(Modifier.height(28.dp))
            Button(
                enabled = claude || ag,
                colors = ButtonDefaults.buttonColors(containerColor = C.clawd, contentColor = C.bg),
                onClick = { repo.updateSettings { it.copy(showClaude = claude, showAg = ag, toolsChosen = true) } },
            ) { Text(txt.pickContinue, fontSize = 16.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) }
        }
    }
}

@Composable
private fun PickCard(modifier: Modifier, name: String, hint: String, pal: Palette, on: Boolean, set: (Boolean) -> Unit, mascot: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.clip(shape).background(pal.bg).border(2.dp, if (on) pal.accent else pal.line, shape).clickable { set(!on) }.padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(on, set, colors = CheckboxDefaults.colors(checkedColor = pal.accent, uncheckedColor = pal.muted, checkmarkColor = pal.bg))
            Text(name, color = pal.text, fontSize = 20.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
        }
        mascot()
        Spacer(Modifier.height(8.dp))
        Text(hint, color = pal.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

fun agPageNeeded(prefs: Prefs, st: Repository.State): Boolean = prefs.showAg && (st.ag != null || !prefs.showClaude)
