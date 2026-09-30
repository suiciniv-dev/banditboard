package dev.clawdboard.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import dev.clawdboard.core.BANDIT
import dev.clawdboard.core.FUR
import dev.clawdboard.core.MASK
import dev.clawdboard.core.MODELS
import dev.clawdboard.core.NOSE
import dev.clawdboard.core.feelOf
import dev.clawdboard.core.settled
import dev.clawdboard.core.txt
import dev.clawdboard.ui.C
import dev.clawdboard.ui.LocalLook
import dev.clawdboard.ui.Look
import dev.clawdboard.ui.Mascot
import dev.clawdboard.ui.fmtAgo
import dev.clawdboard.ui.fmtPct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Color
import java.awt.Desktop
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage

object MenuBar {
    internal var openAt: Int? = null
    private const val HEIGHT = 44
    private const val CELL = 3
    internal val dark = MutableStateFlow(false)
    internal var icon: TrayIcon? = null

    private val font: Font by lazy {
        runCatching {
            MenuBar::class.java.classLoader.getResourceAsStream("fredoka.ttf")!!.use { Font.createFont(Font.TRUETYPE_FONT, it) }
        }.getOrElse { Font(Font.SANS_SERIF, Font.PLAIN, 1) }.deriveFont(Font.BOLD, 25f)
    }

    internal fun readDark() = runCatching {
        val p = ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle").redirectErrorStream(true).start()
        p.inputStream.bufferedReader().readText().trim().equals("Dark", ignoreCase = true)
    }.getOrDefault(false)

    fun image(text: String?, dark: Boolean): BufferedImage {
        val rows = BANDIT.drop(3)
        val art = rows.maxOf { it.length } * CELL
        val probe = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
        val metrics = probe.getFontMetrics(font)
        probe.dispose()
        val gap = if (text == null) 0 else 8
        val width = art + gap + (text?.let { metrics.stringWidth(it) + 2 } ?: 0)
        val img = BufferedImage(width, HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        val top = (HEIGHT - rows.size * CELL) / 2
        rows.forEachIndexed { r, row ->
            row.forEachIndexed { c, ch ->
                val color = when (ch) {
                    'B' -> Color(0xA3, 0x9B, 0x90)
                    'M' -> Color(MASK.toInt(), true)
                    'L' -> Color(FUR.toInt(), true)
                    'N' -> Color(NOSE.toInt(), true)
                    else -> null
                } ?: return@forEachIndexed
                g.color = color
                g.fillRect(c * CELL, top + r * CELL, CELL, CELL)
            }
        }
        if (text != null) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            g.font = font
            g.color = if (dark) Color(255, 255, 255, 235) else Color(0, 0, 0, 215)
            val baseline = (HEIGHT - metrics.ascent - metrics.descent) / 2 + metrics.ascent
            g.drawString(text, art + gap, baseline)
        }
        g.dispose()
        return img
    }

    fun notify(n: Notification) {
        val type = when (n.type) {
            Notification.Type.Warning -> TrayIcon.MessageType.WARNING
            Notification.Type.Error -> TrayIcon.MessageType.ERROR
            else -> TrayIcon.MessageType.INFO
        }
        icon?.displayMessage(n.title, n.message, type)
    }
}

@Composable
fun ApplicationScope.MacBar(port: Int, widget: Boolean, onWidget: (Boolean) -> Unit, onPanel: () -> Unit, onQuit: () -> Unit) {
    if (!SystemTray.isSupported()) return
    var anchor by remember { mutableStateOf<Point?>(null) }
    var closedAt by remember { mutableLongStateOf(0L) }
    val tray = remember { TrayIcon(MenuBar.image(null, MenuBar.dark.value)).apply { isImageAutoSize = false; toolTip = "Banditboard" } }
    DisposableEffect(Unit) {
        val listener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                when {
                    anchor != null -> { anchor = null; closedAt = System.currentTimeMillis() }
                    System.currentTimeMillis() - closedAt > 300 -> anchor = e.locationOnScreen
                }
            }
        }
        tray.addMouseListener(listener)
        runCatching { SystemTray.getSystemTray().add(tray) }
        MenuBar.icon = tray
        onDispose {
            MenuBar.icon = null
            tray.removeMouseListener(listener)
            runCatching { SystemTray.getSystemTray().remove(tray) }
        }
    }
    LaunchedEffect(Unit) {
        MenuBar.openAt?.let {
            delay(2_000L)
            anchor = Point(it, 0)
        }
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isDark by MenuBar.dark.collectAsState()
    LaunchedEffect(Unit) {
        while (true) {
            MenuBar.dark.value = withContext(Dispatchers.IO) { MenuBar.readDark() }
            now = System.currentTimeMillis()
            delay(30_000L)
        }
    }
    val snap by usage.collectAsState()
    val text = snap?.settled(now)?.fiveHour?.percent?.let { fmtPct(it) }
    val img = remember(text, isDark) { MenuBar.image(text, isDark) }
    SideEffect { if (tray.image !== img) tray.image = img }
    anchor?.let { at ->
        Popover(at, port, widget, onWidget, onPanel, onQuit) {
            anchor = null
            closedAt = System.currentTimeMillis()
        }
    }
}

@Composable
private fun ApplicationScope.Popover(
    at: Point, port: Int, widget: Boolean, onWidget: (Boolean) -> Unit, onPanel: () -> Unit, onQuit: () -> Unit, onClose: () -> Unit,
) {
    val width = 340
    val height = if (usage.value == null) 392 else 348
    val spot = remember(at) {
        val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.map { it.defaultConfiguration }
            .firstOrNull { it.bounds.contains(at) } ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
        val b = screen.bounds
        val top = Toolkit.getDefaultToolkit().getScreenInsets(screen).top
        Point((at.x - width / 2).coerceIn(b.x + 6, b.x + b.width - width - 6), b.y + top + 2)
    }
    val close by rememberUpdatedState(onClose)
    Window(
        onCloseRequest = onClose,
        title = "Banditboard",
        undecorated = true,
        transparent = true,
        resizable = false,
        alwaysOnTop = true,
        state = rememberWindowState(size = androidx.compose.ui.unit.DpSize(width.dp, height.dp), position = WindowPosition(spot.x.dp, spot.y.dp)),
        onKeyEvent = { if (it.key == Key.Escape) { close(); true } else false },
    ) {
        LaunchedEffect(Unit) {
            window.addWindowFocusListener(object : WindowAdapter() {
                override fun windowLostFocus(e: WindowEvent) = close()
            })
            runCatching { Desktop.getDesktop().requestForeground(true) }
            window.toFront()
            window.requestFocus()
        }
        Box(Modifier.fillMaxSize().padding(8.dp).shadow(12.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(C.bg)) {
            Card(port, widget, onWidget, { close(); onPanel() }, onQuit)
        }
    }
}

@Composable
private fun Card(port: Int, widget: Boolean, onWidget: (Boolean) -> Unit, onPanel: () -> Unit, onQuit: () -> Unit) {
    val snap by usage.collectAsState()
    val at by pushedAt.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val p by prefsFlow.collectAsState()
    val look = Look(skin = p.skin, tint = p.tint, animations = p.animations, species = p.mascot())
    val u = snap?.settled(now)
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLook provides look) {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Banditboard", color = C.text, fontSize = 16.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (u != null) Text(txt.updatedAgo(fmtAgo(now - at)), color = C.dim, fontSize = 11.sp)
            }
            Meter(txt.session, u?.fiveHour, now, small = true)
            Meter(txt.week, u?.sevenDay, now, small = true)
            if (u == null) Connect(port, onHelp = onPanel)
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MODELS.forEachIndexed { i, m ->
                    val own = u.scoped.firstOrNull { it.label.contains(m, ignoreCase = true) }
                    val pct = own?.percent ?: u.sevenDay?.percent
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Mascot(Modifier.fillMaxWidth(0.8f), model = m, seed = i, feel = feelOf(u, m))
                        Text(m, color = C.muted, fontSize = 11.sp, fontFamily = Fredoka)
                        Text(
                            fmtPct(pct), color = if (own != null && pct != null) C.level(pct) else C.dim,
                            fontSize = 12.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Toggle(txt.macWidget, widget, onWidget)
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Link(txt.trayPanel, onPanel)
                Link(if (busy) txt.desktopRefreshing else txt.desktopRefresh) {
                    if (!busy) {
                        busy = true
                        scope.launch { withContext(Dispatchers.IO) { Hook.refresh() }; busy = false }
                    }
                }
                Link(txt.trayQuit, onQuit)
            }
        }
    }
}

@Composable
private fun Link(text: String, onClick: () -> Unit) {
    Text(text, color = C.clawd, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onClick))
}
