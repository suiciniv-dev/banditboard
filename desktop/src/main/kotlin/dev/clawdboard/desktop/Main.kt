
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
            alert?.let { tray.sendNotification(notification(it)) }
        }
    }
    return true
}

private fun notification(a: Alert): Notification {
    val body = if (a.level == 0) txt.alertFree else a.resetsAt?.let { txt.alertResets(fmtAt(it, System.currentTimeMillis())) }.orEmpty()
    val type = if (a.level >= 90) Notification.Type.Warning else Notification.Type.Info
    return Notification(txt.alertTitle(a.kind == AlertKind.WEEK, a.level), body, type)
}

internal val SIZE = DpSize(480.dp, 190.dp)
internal val COMPACT = DpSize(300.dp, 130.dp)
internal val MINI = DpSize(96.dp, 112.dp)

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
    val lock = runCatching {
        FileChannel.open(File(Store.dir, "lock").toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE).tryLock()
    }.getOrNull() ?: return
    Store.key
    if ("--clawd" in args) savePrefs { it.copy(clawdUnlocked = true) }
    WinMusic.start()
    Store.get("last")?.let { raw ->
        val at = Store.get("lastAt")?.toLongOrNull() ?: 0L
        usage.value = runCatching { parsePush(JSONObject(raw), at) }.getOrNull()
        pushedAt.value = at
        usage.value?.let { DeskHistory.record(it) }
    }
    application {
        val tray = rememberTrayState()
        val server = remember { Server { receive(it, tray) }.also { it.start() } }
        var visible by remember { mutableStateOf(true) }
        var onTop by remember { mutableStateOf(Store.get("onTop") != "false") }
        var inTaskbar by remember { mutableStateOf(Store.get("taskbar") == "true") }
        var layout by remember { mutableStateOf(runCatching { Layout.valueOf(Store.get("layout") ?: "") }.getOrDefault(Layout.FULL)) }
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
            if (hooked != 0 && hooked != server.port) withContext(Dispatchers.IO) { Hook.install(server.port) }
        }
        val trayScope = rememberCoroutineScope()
        Tray(
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
        key(inTaskbar, shown) { Window(
            visible = visible,
            create = {
                ComposeWindow().apply {
                    type = if (inTaskbar) java.awt.Window.Type.NORMAL else java.awt.Window.Type.UTILITY
                    isUndecorated = true
                    isTransparent = true
                    isResizable = false
                    title = "Banditboard"
                    iconImage = RaccoonIcon.toAwtImage(Density(1f), LayoutDirection.Ltr, Size(64f, 64f))
                    val s = when (shown) { Layout.FULL -> SIZE; Layout.COMPACT -> COMPACT; Layout.MINI -> MINI }
                    setSize(s.width.value.toInt(), s.height.value.toInt())
                    defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
                    addWindowListener(object : WindowAdapter() {
                        override fun windowClosing(e: WindowEvent) { visible = false }
                    })
                }
            },
            dispose = ComposeWindow::dispose,
            update = { it.isAlwaysOnTop = onTop },
        ) {
            LaunchedEffect(Unit) { WidgetSpot.place(shown, window) }
            CompositionLocalProvider(LocalDance provides dance) {
                val drag = Modifier.dragWindow(window, onDoubleClick = { panel = true }, onMoved = { WidgetSpot.save(shown, window) })
                if (shown == Layout.MINI) Box(Modifier.fillMaxSize().then(drag)) { Mini() }
                else Box(
                    Modifier.fillMaxSize().padding(8.dp).shadow(10.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(C.bg).then(drag),
                ) {
                    Dashboard(server.port, shown == Layout.COMPACT)
                    Text(
                        "×", color = C.dim, fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 4.dp).clickable { visible = false },
                    )
                }
            }
        } }
        if (panel) Window(
            onCloseRequest = { panel = false },
            title = "Banditboard",
            icon = RaccoonIcon,
            state = rememberWindowState(size = DpSize(1200.dp, 820.dp), position = WindowPosition(Alignment.Center)),
        ) {
            LaunchedEffect(Unit) { DarkTitle.apply(window, C.bg.toArgb(), C.text.toArgb()) }
            val controls = WidgetControls(
                layout, { layout = it; Store.put("layout", it.name) },
                onTop, { onTop = it; Store.put("onTop", it.toString()) },
                inTaskbar, { inTaskbar = it; Store.put("taskbar", it.toString()) },
                if (Autostart.available) autostart else null, { Autostart.set(it); autostart = Autostart.enabled() },
                danceOn, { danceOn = it; Store.put("dance", it.toString()) },
            )
            CompositionLocalProvider(LocalDance provides dance) { Panel(server.port, status, controls) }
        }
    }
}

@Composable
internal fun Mini() {
    val snap by usage.collectAsState()
    val p by prefsFlow.collectAsState()
    val u = snap?.settled(System.currentTimeMillis())
    val look = Look(skin = p.skin, tint = p.tint, animations = p.animations, species = p.mascot())
    val pct = u?.fiveHour?.percent
    CompositionLocalProvider(LocalLook provides look) {
        Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val solo = u?.soloModel(System.currentTimeMillis())
            Mascot(Modifier.fillMaxWidth(), model = solo, feel = feelOf(u, solo ?: "Opus"), reserveTop = solo != null)
            Text(
                fmtPct(pct), color = pct?.let { C.level(it) } ?: C.dim, fontSize = 13.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.background(C.bg.copy(alpha = 0.75f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp),
            )
        }
    }
}

@Composable
internal fun Dashboard(port: Int, compact: Boolean) {
    val snap by usage.collectAsState()
    val at by pushedAt.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val p by prefsFlow.collectAsState()
    val look = Look(skin = p.skin, tint = p.tint, animations = p.animations, species = p.mascot())
    val u = snap?.settled(now)
    CompositionLocalProvider(LocalLook provides look) {
        if (compact) Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Meter(txt.session, u?.fiveHour, now, small = true)
                Meter(txt.week, u?.sevenDay, now, small = true)
            }
            val solo = u?.soloModel(now)
            Mascot(Modifier.width(64.dp), model = solo, feel = feelOf(u, solo ?: "Opus"))
        } else Row(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Meter(txt.session, u?.fiveHour, now)
                if (u == null) Connect(port)
                else {
                    Meter(txt.week, u.sevenDay, now)
                    Text(txt.updatedAgo(fmtAgo(now - at)), color = C.dim, fontSize = 11.sp)
                }
            }
            Row(Modifier.width(200.dp).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                MODELS.forEachIndexed { i, m ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Mascot(Modifier.fillMaxWidth(), model = m, seed = i, feel = feelOf(u, m))
                        Text(m, color = C.muted, fontSize = 12.sp, fontFamily = Fredoka)
                    }
                }
            }
        }
    }
}

@Composable
internal fun Meter(label: String, w: UsageWindow?, now: Long, small: Boolean = false) {
    val pct = w?.percent
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, color = C.text, fontSize = if (small) 12.sp else 15.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            w?.resetsAt?.let { Text(if (small) fmtLeft(it - now) else "${txt.resetsIn} ${fmtLeft(it - now)}", color = C.muted, fontSize = if (small) 10.sp else 12.sp) }
        }
        Text(fmtPct(pct), color = pct?.let { C.level(it) } ?: C.dim, fontSize = if (small) 18.sp else 26.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(C.track)) {
            if (pct != null) Box(Modifier.fillMaxWidth((pct / 100).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(C.level(pct)))
        }
    }
}

@Composable
internal fun Connect(port: Int, hint: Boolean = true) {
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(Hook.connected()) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun refresh() {
        status = txt.desktopRefreshing
        val ok = Hook.refresh()
        status = if (ok) null else txt.desktopRefreshFailed
    }
    fun connect() {
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { Hook.install(port) }
            connected = connected || ok
            if (ok) withContext(Dispatchers.IO) { refresh() } else status = txt.desktopConnectFailed
            busy = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val shown = status ?: if (hint) txt.desktopWaiting else null
        if (shown != null) Text(shown, color = C.muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    if (!connected) connect()
                    else { busy = true; scope.launch { withContext(Dispatchers.IO) { refresh() }; busy = false } }
                },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = C.clawd, contentColor = Color.Black),
            ) { Text(if (connected) txt.desktopRefresh else txt.desktopConnect, fontFamily = Fredoka) }
            if (connected) Text(
                txt.desktopReconnect, color = C.dim, fontSize = 12.sp,
                modifier = Modifier.clickable(enabled = !busy) { connect() },
            )
        }
    }
}
