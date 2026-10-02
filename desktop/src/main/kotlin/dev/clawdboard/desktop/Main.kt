
package dev.clawdboard.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.key
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.WindowConstants
import dev.clawdboard.core.Alert
import dev.clawdboard.core.AgFamily
import dev.clawdboard.core.AgPool
import dev.clawdboard.core.AgSnapshot
import dev.clawdboard.core.Tool
import dev.clawdboard.core.ToolTheme
import dev.clawdboard.core.agFeel
import dev.clawdboard.core.AlertKind
import dev.clawdboard.core.BANDIT
import dev.clawdboard.core.FUR
import dev.clawdboard.core.MASK
import dev.clawdboard.core.MODELS
import dev.clawdboard.core.NOSE
import dev.clawdboard.core.Prefs
import dev.clawdboard.core.UsageSnapshot
import dev.clawdboard.core.UsageWindow
import dev.clawdboard.core.feelOf
import dev.clawdboard.core.nextAlert
import dev.clawdboard.core.parsePush
import dev.clawdboard.core.settled
import dev.clawdboard.core.soloModel
import dev.clawdboard.core.txt
import dev.clawdboard.ui.C
import dev.clawdboard.ui.LocalDance
import dev.clawdboard.ui.LocalLook
import dev.clawdboard.core.StatusApi
import dev.clawdboard.core.StatusSnapshot
import androidx.compose.ui.window.WindowPosition
import dev.clawdboard.ui.Look
import dev.clawdboard.ui.Mascot
import dev.clawdboard.ui.fmtAgo
import dev.clawdboard.ui.fmtAt
import dev.clawdboard.ui.fmtLeft
import dev.clawdboard.ui.fmtPct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import dev.clawdboard.core.I18n
import dev.clawdboard.core.Species
import dev.clawdboard.core.Tint
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.awt.Toolkit

internal val Fredoka = FontFamily(
    Font("fredoka.ttf", FontWeight.Normal),
    Font("fredoka.ttf", FontWeight.Medium),
    Font("fredoka.ttf", FontWeight.SemiBold),
)

private object RaccoonIcon : Painter() {
    override val intrinsicSize = Size(16f, 16f)
    override fun DrawScope.onDraw() {
        val u = size.width / 16f
        val top = (16 - (BANDIT.size - 3)) / 2f
        BANDIT.drop(3).forEachIndexed { r, row ->
            row.forEachIndexed { c, ch ->
                val color = when (ch) {
                    'B' -> Color(0xFFA39B90)
                    'M' -> Color(MASK)
                    'L' -> Color(FUR)
                    'N' -> Color(NOSE)
                    else -> null
                } ?: return@forEachIndexed
                drawRect(color, Offset(c * u, (r + top) * u), Size(u + 0.5f, u + 0.5f))
            }
        }
    }
}

internal val usage = MutableStateFlow<UsageSnapshot?>(null)
internal val pushedAt = MutableStateFlow(0L)

private fun receive(body: JSONObject, tray: TrayState): Boolean {
    val now = System.currentTimeMillis()
    val snap = parsePush(body, now) ?: return false
    Store.put("last", body.toString())
    Store.put("lastAt", now.toString())
    usage.value = snap
    pushedAt.value = now
    DeskHistory.record(snap)
    if (prefsFlow.value.alerts) {
        listOf(AlertKind.SESSION to snap.fiveHour, AlertKind.WEEK to snap.sevenDay).forEach { (kind, w) ->
            val (alert, level) = nextAlert(kind, w, Store.int("mark_${kind.name}"))
            Store.put("mark_${kind.name}", level)
            alert?.let { a -> notification(a, Tool.CLAUDE).let { if (onMac) MenuBar.notify(it) else tray.sendNotification(it) } }
        }
    }
    return true
}

private fun receiveAg(snap: AgSnapshot, tray: TrayState) {
    AgHistory.record(snap.fetchedAt, snap.pool(AgPool.GEMINI)?.percent, snap.pool(AgPool.OTHERS)?.percent)
    Forward.changed()
    if (!prefsFlow.value.alerts) return
    AgPool.entries.forEach { pool ->
        val m = snap.pool(pool) ?: return@forEach
        val kind = m.kind ?: AlertKind.WEEK
        val (alert, level) = nextAlert(kind, UsageWindow(m.percent, m.resetsAt), Store.int("ag_mark_${pool.name}"))
        Store.put("ag_mark_${pool.name}", level)
        alert?.let { a -> notification(a, Tool.ANTIGRAVITY, txt.label(pool)).let { if (onMac) MenuBar.notify(it) else tray.sendNotification(it) } }
    }
}

private fun notification(a: Alert, tool: Tool, scope: String? = null): Notification {
    val ag = tool == Tool.ANTIGRAVITY
    val free = if (ag) txt.agAlertFree else txt.alertFree
    val body = if (a.level == 0) free else a.resetsAt?.let { txt.alertResets(fmtAt(it, System.currentTimeMillis())) }.orEmpty()
    val type = if (a.level >= 90) Notification.Type.Warning else Notification.Type.Info
    val title = txt.alertTitle(a.kind == AlertKind.WEEK, a.level)
    val name = when {
        ag -> txt.toolAntigravity
        Antigravity.enabled.value -> txt.toolClaude
        else -> null
    }
    return Notification(listOfNotNull(name, scope, title).joinToString(" · "), body, type)
}

internal val SIZE = DpSize(480.dp, 190.dp)
internal val COMPACT = DpSize(300.dp, 130.dp)
internal val MINI = DpSize(96.dp, 112.dp)

internal fun sizeOf(layout: Layout, tools: Int): DpSize = when (layout) {
    Layout.FULL -> DpSize(SIZE.width, SIZE.height * tools - 8.dp * (tools - 1))
    Layout.COMPACT -> DpSize(COMPACT.width, COMPACT.height * tools - 8.dp * (tools - 1))
    Layout.MINI -> MINI
}

enum class Layout { FULL, COMPACT, MINI }

private fun prefs(): Prefs = Store.get("prefs")?.let { runCatching { Prefs().merge(JSONObject(it)) }.getOrNull() } ?: Prefs()

internal val prefsFlow = MutableStateFlow(prefs().also { I18n.language = it.language })

internal fun savePrefs(f: (Prefs) -> Prefs) {
    val p = f(prefsFlow.value).sanitized()
    Store.put("prefs", p.toJson().toString())
    I18n.language = p.language
    prefsFlow.value = p
}

fun main(args: Array<String>) {
    if (!onMac && System.getenv("SKIKO_RENDER_API") == null) System.setProperty("skiko.renderApi", "SOFTWARE")
    val lock = runCatching {
        FileChannel.open(File(Store.dir, "lock").toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE).tryLock()
    }.getOrNull() ?: return
    Store.key
    Autostart.repair()
    Share.restore()
    if ("--clawd" in args) savePrefs { it.copy(clawdUnlocked = true) }
    MenuBar.openAt = args.firstOrNull { it.startsWith("--popover=") }?.substringAfter('=')?.toIntOrNull()
    if (!onMac) WinMusic.start()
    Store.get("last")?.let { raw ->
        val at = Store.get("lastAt")?.toLongOrNull() ?: 0L
        usage.value = runCatching { parsePush(JSONObject(raw), at) }.getOrNull()
        pushedAt.value = at
        usage.value?.let { DeskHistory.record(it) }
    }
    application {
        val tray = rememberTrayState()
        val server = remember { Server(onHook = Claude::onHook) { receive(it, tray) }.also { it.start() } }
        LaunchedEffect(Unit) { while (true) { delay(2_000); Claude.tick() } }
        LaunchedEffect(Unit) {
            while (true) {
                runCatching { withContext(Dispatchers.IO) { Antigravity.tick(System.currentTimeMillis()) }?.let { receiveAg(it, tray) } }
                delay(Antigravity.CHECK_MS)
            }
        }
        Claude.notify = { title, body ->
            val n = Notification(title, body, Notification.Type.Warning)
            if (onMac) MenuBar.notify(n) else tray.sendNotification(n)
        }
        var visible by remember { mutableStateOf(!onMac || Store.get("widget") == "true") }
        val showWidget: (Boolean) -> Unit = { visible = it; if (onMac) Store.put("widget", it.toString()) }
        var onTop by remember { mutableStateOf(Store.get("onTop") != "false") }
        var inTaskbar by remember { mutableStateOf(Store.get("taskbar") == "true") }
        var layout by remember {
            mutableStateOf(
                runCatching { Layout.valueOf(Store.get("layout") ?: "") }.getOrDefault(Layout.FULL)
                    .let { if (onMac && it == Layout.MINI) Layout.FULL else it },
            )
        }
        var panel by remember { mutableStateOf("--panel" in args) }
        var danceOn by remember { mutableStateOf(Store.get("dance") != "false") }
        val playing by WinMusic.playing.collectAsState()
        val dance = danceOn && playing
        var status by remember { mutableStateOf<StatusSnapshot?>(null) }
        LaunchedEffect(Unit) {
            while (true) {
                withContext(Dispatchers.IO) { StatusApi.fetch() }?.let { status = it }
                delay(300_000L)
            }
        }
        var promo by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(if ("--promo" in args) 3_000L else 60_000L)
            while (true) {
                val now = System.currentTimeMillis()
                if ("--promo" in args || (usage.value != null && Promo.due(now))) {
                    Promo.shown(now)
                    promo = true
                    break
                }
                delay(3_600_000L)
            }
        }
        if (promo) Window(
            onCloseRequest = { promo = false },
            title = "Banditboard",
            icon = RaccoonIcon,
            undecorated = true,
            transparent = true,
            resizable = false,
            alwaysOnTop = true,
            state = rememberWindowState(size = DpSize(440.dp, 170.dp)),
        ) {
            LaunchedEffect(Unit) {
                val gc = window.graphicsConfiguration
                val b = gc.bounds
                val ins = Toolkit.getDefaultToolkit().getScreenInsets(gc)
                window.setLocation(b.x + b.width - ins.right - window.width - 12, b.y + b.height - ins.bottom - window.height - 210)
            }
            PromoCard(onLater = { promo = false }, onNever = { Promo.never(); promo = false })
        }
        val shown = layout
        var autostart by remember { mutableStateOf(Autostart.enabled()) }
        val p by prefsFlow.collectAsState()
        LaunchedEffect(server.port) {
            val hooked = Store.int("hookPort")
            if (hooked != 0 && (hooked != server.port || Hook.stale())) withContext(Dispatchers.IO) { Hook.install(server.port) }
        }
        val trayScope = rememberCoroutineScope()
        val claudeOn by Tools.claude.collectAsState()
        val agOn by Antigravity.enabled.collectAsState()
        val theme by Tools.theme.collectAsState()
        val tools = Tools.shown(claudeOn, agOn)
        if (onMac) MacBar(server.port, visible, showWidget, { panel = true }, { lock.release(); exitApplication() })
        else Tray(
            icon = RaccoonIcon,
            state = tray,
            tooltip = "Banditboard",
            onAction = { visible = true },
            menu = {
                Item(txt.trayPanel) { panel = true }
                Item(txt.trayShow) { visible = true }
                Item(txt.desktopRefresh) { trayScope.launch(Dispatchers.IO) { Hook.refresh() } }
                Menu(txt.trayLayout) {
                    listOf(Layout.FULL to txt.layoutFull, Layout.COMPACT to txt.trayCompact, Layout.MINI to txt.layoutMini).forEach { (l, name) ->
                        CheckboxItem(name, layout == l) { _ -> layout = l; Store.put("layout", l.name) }
                    }
                }
                CheckboxItem(txt.trayOnTop, onTop) { onTop = it; Store.put("onTop", it.toString()) }
                CheckboxItem(txt.trayTaskbar, inTaskbar) { inTaskbar = it; Store.put("taskbar", it.toString()) }
                if (Autostart.available) CheckboxItem(txt.trayAutostart, autostart) { Autostart.set(it); autostart = Autostart.enabled() }
                CheckboxItem(txt.trayMusic, danceOn) { danceOn = it; Store.put("dance", it.toString()) }
                CheckboxItem(txt.alerts, p.alerts) { v -> savePrefs { it.copy(alerts = v) } }
                Menu(txt.tools) {
                    CheckboxItem(txt.toolClaude, claudeOn) { Tools.setClaude(it) }
                    CheckboxItem(txt.toolAntigravity, agOn) { Antigravity.setEnabled(it) }
                    Separator()
                    Item(txt.agExport) {
                        trayScope.launch(Dispatchers.IO) {
                            val n = runCatching { Antigravity.export(System.currentTimeMillis()) }
                                .map { Notification("Banditboard", txt.agExported(it.path), Notification.Type.Info) }.getOrNull()
                            n?.let { tray.sendNotification(it) }
                        }
                    }
                }
                Menu(txt.theme) {
                    ToolTheme.entries.forEach { t -> CheckboxItem(txt.label(t), theme == t) { _ -> Tools.setTheme(t) } }
                }
                Menu(txt.mascot) {
                    Species.entries.filter { it != Species.CLAWD || p.clawdUnlocked }.forEach { s ->
                        CheckboxItem(txt.label(s), p.mascot() == s) { _ -> savePrefs { it.copy(species = s) } }
                    }
                }
                Menu(txt.color) {
                    Tint.entries.forEach { t -> CheckboxItem(txt.label(t), p.tint == t) { _ -> savePrefs { it.copy(tint = t) } } }
                }
                Separator()
                Item(txt.trayQuit) { lock.release(); exitApplication() }
            },
        )
        key(inTaskbar, shown, tools.size) { Window(
            visible = visible,
            create = {
                ComposeWindow().apply {
                    type = if (inTaskbar) java.awt.Window.Type.NORMAL else java.awt.Window.Type.UTILITY
                    isUndecorated = true
                    isTransparent = true
                    isResizable = false
                    title = "Banditboard"
                    iconImage = RaccoonIcon.toAwtImage(Density(1f), LayoutDirection.Ltr, Size(64f, 64f))
                    val s = sizeOf(shown, tools.size)
                    setSize(s.width.value.toInt(), s.height.value.toInt())
                    defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
                    addWindowListener(object : WindowAdapter() {
                        override fun windowClosing(e: WindowEvent) = showWidget(false)
                    })
                }
            },
            dispose = ComposeWindow::dispose,
            update = { it.isAlwaysOnTop = onTop },
        ) {
            LaunchedEffect(Unit) { WidgetSpot.place(shown, window) }
            if (onTop && !onMac) LaunchedEffect(Unit) { while (true) { WinTop.raise(window); delay(2_000) } }
            CompositionLocalProvider(LocalDance provides dance) {
                val drag = Modifier.dragWindow(window, onDoubleClick = { panel = true }, onMoved = { WidgetSpot.save(shown, window) })
                if (shown == Layout.MINI) Box(Modifier.fillMaxSize().then(drag)) { Mini() }
                else Widget(tools, server.port, shown == Layout.COMPACT, drag, onHelp = { panel = true }, onClose = { showWidget(false) })
            }
        } }
        if (panel) Window(
            onCloseRequest = { panel = false },
            title = "Banditboard",
            icon = RaccoonIcon,
            state = rememberWindowState(size = DpSize(1200.dp, 820.dp), position = WindowPosition(Alignment.Center)),
        ) {
            val title = Tools.palette(Tool.CLAUDE)
            LaunchedEffect(Unit) { DarkTitle.apply(window, title.bg.toArgb(), title.text.toArgb()) }
            val controls = WidgetControls(
                layout, { layout = it; Store.put("layout", it.name) },
                onTop, { onTop = it; Store.put("onTop", it.toString()) },
                inTaskbar, { inTaskbar = it; Store.put("taskbar", it.toString()) },
                if (Autostart.available) autostart else null, { Autostart.set(it); autostart = Autostart.enabled() },
                danceOn, { danceOn = it; Store.put("dance", it.toString()) },
                visible, showWidget,
            )
            CompositionLocalProvider(LocalDance provides dance) { Themed(Tool.CLAUDE) { Panel(server.port, status, controls) } }
        }
    }
}

@Composable
internal fun Mini() {
    val tools = shownTools()
    val snap by usage.collectAsState()
    val ag by Antigravity.snapshot.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val u = snap?.settled(now)
    val claudeWorst = listOfNotNull(u?.fiveHour?.percent, u?.sevenDay?.percent).maxOrNull()
    val agWorst = ag?.models?.maxOfOrNull { it.percent }
    val tool = when {
        Tool.ANTIGRAVITY !in tools -> Tool.CLAUDE
        Tool.CLAUDE !in tools -> Tool.ANTIGRAVITY
        (agWorst ?: -1.0) > (claudeWorst ?: -1.0) -> Tool.ANTIGRAVITY
        else -> Tool.CLAUDE
    }
    Themed(tool) { if (tool == Tool.CLAUDE) ClaudeMini() else AgMini() }
}

@Composable
private fun ClaudeMini() {
    val snap by usage.collectAsState()
    val p by prefsFlow.collectAsState()
    val list by Claude.sessions.collectAsState()
    val on by Claude.watching.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val u = snap?.settled(now)
    val look = Look(skin = p.skin, tint = p.tint, animations = p.animations, species = p.mascot())
    val pct = u?.fiveHour?.percent
    CompositionLocalProvider(LocalLook provides look) {
        Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val solo = (if (on) Claude.solo(list) else null) ?: u?.soloModel(now)
            val react = Claude.react(null, list, now, on)
            Mascot(Modifier.fillMaxWidth(), model = solo, feel = feelOf(u, solo ?: "Opus", react), reserveTop = solo != null || react.bubble())
            MiniPct(pct)
        }
    }
}

@Composable
private fun AgMini() {
    val snap by Antigravity.snapshot.collectAsState()
    val p by prefsFlow.collectAsState()
    val look = Look(skin = p.skin, tint = p.tint, animations = p.animations, species = p.mascot())
    val worst = agWorstFamily(snap)
    CompositionLocalProvider(LocalLook provides look) {
        Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Mascot(Modifier.fillMaxWidth(), wear = (worst?.first ?: AgFamily.PRO).accessory, feel = agFeel(worst?.second))
            MiniPct(worst?.second)
        }
    }
}

@Composable
private fun MiniPct(pct: Double?) {
    Text(
        fmtPct(pct), color = pct?.let { C.level(it) } ?: P.dim, fontSize = 13.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(P.bg.copy(alpha = 0.75f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp),
    )
}

internal fun agWorstFamily(s: AgSnapshot?): Pair<AgFamily, Double>? =
    AgFamily.entries.mapNotNull { f -> s?.family(f)?.let { f to it.percent } }.maxByOrNull { it.second }

@Composable
internal fun Widget(tools: List<Tool>, port: Int, compact: Boolean, drag: Modifier = Modifier, onHelp: (() -> Unit)? = null, onClose: (() -> Unit)? = null) {
    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tools.forEachIndexed { i, tool ->
            Themed(tool) {
                Box(Modifier.weight(1f).fillMaxWidth().shadow(10.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(P.bg).then(drag)) {
                    if (tool == Tool.CLAUDE) Dashboard(port, compact, onHelp) else AgDashboard(compact)
                    if (i == 0 && onClose != null) Text(
                        "×", color = P.dim, fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 4.dp).clickable { onClose() },
                    )
                }
            }
        }
    }
}

@Composable
internal fun AgDashboard(compact: Boolean) {
    val snap by Antigravity.snapshot.collectAsState()
    val open by Antigravity.open.collectAsState()
    val state by Antigravity.state.collectAsState()
    val at by Antigravity.lastAt.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val p by prefsFlow.collectAsState()
    val look = Look(skin = p.skin, tint = p.tint, animations = p.animations, species = p.mascot())
    val s = snap
    val pools = AgPool.entries.filter { s == null || s.pool(it) != null }
    val status = agStatus(state, s != null, now - at)
    CompositionLocalProvider(LocalLook provides look) {
        if (compact) Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pools.forEach { PoolMeter(s, it, now, small = true) }
                if (pools.size < 2) Text(status, color = if (open) P.dim else P.muted, fontSize = 10.sp, maxLines = 1)
            }
            val worst = agWorstFamily(s)
            Mascot(Modifier.width(64.dp), wear = (worst?.first ?: AgFamily.PRO).accessory, feel = agFeel(worst?.second))
        } else Row(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                pools.forEach { PoolMeter(s, it, now) }
                Text(status, color = if (open) P.dim else P.muted, fontSize = 11.sp, maxLines = 1)
            }
            Row(Modifier.width(200.dp).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val families = AgFamily.entries.filter { s == null || s.family(it) != null }
                families.forEachIndexed { i, f ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Mascot(Modifier.fillMaxWidth(), seed = i, wear = f.accessory, feel = agFeel(s?.family(f)?.percent))
                        Text(f.short, color = P.muted, fontSize = 12.sp, fontFamily = Fredoka)
                    }
                }
            }
        }
    }
}

@Composable
internal fun Dashboard(port: Int, compact: Boolean, onHelp: (() -> Unit)? = null) {
    val snap by usage.collectAsState()
    val at by pushedAt.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val p by prefsFlow.collectAsState()
    val look = Look(skin = p.skin, tint = p.tint, animations = p.animations, species = p.mascot())
    val u = snap?.settled(now)
    val list by Claude.sessions.collectAsState()
    val on by Claude.watching.collectAsState()
    CompositionLocalProvider(LocalLook provides look) {
        if (compact) Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Meter(txt.session, u?.fiveHour, now, small = true)
                Meter(txt.week, u?.sevenDay, now, small = true)
            }
            val solo = (if (on) Claude.solo(list) else null) ?: u?.soloModel(now)
            Mascot(Modifier.width(64.dp), model = solo, feel = feelOf(u, solo ?: "Opus", Claude.react(null, list, now, on)))
        } else Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Meter(txt.session, u?.fiveHour, now)
                if (u == null) Connect(port, onHelp = onHelp)
                else {
                    Meter(txt.week, u.sevenDay, now)
                    if (!on) Text(txt.updatedAgo(fmtAgo(now - at)), color = P.dim, fontSize = 11.sp)
                }
            }
            Row(Modifier.width(200.dp).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                MODELS.forEachIndexed { i, m ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Mascot(Modifier.fillMaxWidth(), model = m, seed = i, feel = feelOf(u, m, Claude.react(m, list, now, on)))
                        Text(m, color = P.muted, fontSize = 12.sp, fontFamily = Fredoka)
                    }
                }
            }
          }
          if (on && u != null) ActivityLine()
        }
    }
}

@Composable
internal fun Meter(label: String, w: UsageWindow?, now: Long, small: Boolean = false, window: String? = null) {
    val pct = w?.percent
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, color = P.text, fontSize = if (small) 12.sp else 15.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            val left = w?.resetsAt?.let { if (small || window != null) fmtLeft(it - now) else "${txt.resetsIn} ${fmtLeft(it - now)}" }
            listOfNotNull(window, left).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { Text(it, color = P.muted, fontSize = if (small) 10.sp else 12.sp) }
        }
        Text(fmtPct(pct), color = pct?.let { C.level(it) } ?: P.dim, fontSize = if (small) 18.sp else 26.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(P.track)) {
            if (pct != null) Box(Modifier.fillMaxWidth((pct / 100).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(C.level(pct)))
        }
    }
}

@Composable
private fun Trouble(f: Failure, onHelp: (() -> Unit)?) {
    val title = when (f.cause) {
        Cause.POLICY -> txt.desktopTroublePolicy
        Cause.SETTINGS -> txt.desktopTroubleSettings
        Cause.DENIED -> if (onMac) txt.macTroubleDenied else txt.desktopTroubleDenied
        Cause.TIMEOUT -> if (onMac) txt.macTroubleTimeout else txt.desktopTroubleTimeout
        Cause.OTHER -> txt.desktopConnectFailed
    }
    if (onHelp != null) Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = C.warn)) { append(title) }
            append(" ")
            withStyle(SpanStyle(color = P.accent, fontWeight = FontWeight.SemiBold)) { append(txt.desktopWhatToDo) }
        },
        fontSize = 12.sp, modifier = Modifier.clickable { onHelp() },
    ) else {
        val help = when (f.cause) {
            Cause.POLICY -> txt.desktopHelpPolicy
            Cause.SETTINGS -> if (onMac) txt.macHelpSettings(Hook.settings.path) else txt.desktopHelpSettings(Hook.settings.path)
            Cause.DENIED -> if (onMac) txt.macHelpDenied(Hook.claudeDir.path) else txt.desktopHelpDenied(Hook.claudeDir.path)
            Cause.TIMEOUT -> if (onMac) txt.macHelpTimeout else txt.desktopHelpTimeout
            Cause.OTHER -> txt.desktopHelpOther
        }
        Text(title, color = C.warn, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(help, color = P.muted, fontSize = 12.sp)
        f.detail?.let { Text("${txt.desktopPowershellSaid} $it", color = P.dim, fontSize = 11.sp) }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (f.cause == Cause.SETTINGS) Text(
                txt.desktopOpenSettings, color = P.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { Hook.show(Hook.settings) },
            )
            if (Hook.log.exists()) Text(
                txt.desktopOpenLog, color = P.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { Hook.show(Hook.log) },
            )
        }
    }
}

@Composable
internal fun Connect(port: Int, hint: Boolean = true, onHelp: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(Hook.connected()) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val failure by Hook.failure.collectAsState()
    fun refresh() {
        status = txt.desktopRefreshing
        val ok = Hook.refresh()
        status = if (ok) null else txt.desktopRefreshFailed
    }
    fun connect() {
        busy = true
        status = null
        scope.launch {
            val ok = withContext(Dispatchers.IO) { Hook.install(port) }
            connected = connected || ok
            if (ok) withContext(Dispatchers.IO) { refresh() }
            busy = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val f = failure
        val shown = status ?: if (hint) txt.desktopWaiting else null
        if (f != null) Trouble(f, onHelp)
        else if (shown != null) Text(shown, color = P.muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    if (!connected) connect()
                    else { busy = true; scope.launch { withContext(Dispatchers.IO) { refresh() }; busy = false } }
                },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = P.accent, contentColor = Color.Black),
            ) { Text(if (connected) txt.desktopRefresh else txt.desktopConnect, fontFamily = Fredoka) }
            if (connected) Text(
                txt.desktopReconnect, color = P.dim, fontSize = 12.sp,
                modifier = Modifier.clickable(enabled = !busy) { connect() },
            )
        }
    }
}

@Composable
internal fun PoolMeter(s: AgSnapshot?, pool: AgPool, now: Long, small: Boolean = false) {
    val m = s?.pool(pool)
    val window = (if (m?.kind == AlertKind.SESSION) txt.session else txt.week).lowercase()
    Meter(txt.label(pool), m?.let { UsageWindow(it.percent, it.resetsAt) }, now, small, window)
}

internal fun agStatus(state: Antigravity.State, hasData: Boolean, ago: Long): String = when (state) {
    Antigravity.State.OK -> txt.agUpdatedAgo(fmtAgo(ago))
    Antigravity.State.STARTING -> txt.agStarting
    Antigravity.State.NO_ACCESS -> txt.agNoAccess
    Antigravity.State.NO_ANSWER -> txt.agNoAnswer
    Antigravity.State.UNSUPPORTED -> txt.agUnsupported
    Antigravity.State.OFF, Antigravity.State.CLOSED -> if (hasData) txt.agClosed else txt.agWaiting
}
