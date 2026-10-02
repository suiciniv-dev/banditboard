package dev.clawdboard.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.MenuScope
import dev.clawdboard.core.Tool
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.CheckboxMenuItem
import java.awt.Menu as AwtMenu
import java.awt.MenuItem
import java.awt.MouseInfo
import java.awt.PopupMenu
import java.awt.Window
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

internal sealed interface Entry {
    class Action(val label: String, val run: () -> Unit) : Entry
    class Check(val label: String, val on: Boolean, val set: (Boolean) -> Unit) : Entry
    class Sub(val label: String, val items: List<Entry>) : Entry
    data object Line : Entry
}

@Composable
internal fun MenuScope.render(items: List<Entry>) {
    items.forEach { e ->
        when (e) {
            is Entry.Action -> Item(e.label, onClick = e.run)
            is Entry.Check -> CheckboxItem(e.label, e.on, onCheckedChange = e.set)
            is Entry.Sub -> Menu(e.label) { render(e.items) }
            Entry.Line -> Separator()
        }
    }
}

internal object WidgetMenu {
    @Volatile var items: List<Entry> = emptyList()
    private var popup: PopupMenu? = null
    private var owner: Window? = null

    fun show(window: Window) {
        val at = MouseInfo.getPointerInfo()?.location ?: return
        val menu = popup?.takeIf { owner === window } ?: PopupMenu().also {
            popup?.let { old -> owner?.remove(old) }
            window.add(it)
            popup = it
            owner = window
        }
        menu.removeAll()
        fill(menu, items)
        val origin = window.locationOnScreen
        menu.show(window, at.x - origin.x, at.y - origin.y)
    }

    private fun fill(menu: AwtMenu, items: List<Entry>) {
        items.forEach { e ->
            when (e) {
                is Entry.Action -> menu.add(MenuItem(e.label).apply { addActionListener { e.run() } })
                is Entry.Check -> menu.add(CheckboxMenuItem(e.label, e.on).apply { addItemListener { e.set(state) } })
                is Entry.Sub -> menu.add(AwtMenu(e.label).also { fill(it, e.items) })
                Entry.Line -> menu.addSeparator()
            }
        }
    }
}

internal object Refresh {
    val claude = MutableStateFlow(0)
    val ag = MutableStateFlow(0)
    private val busy = AtomicBoolean(false)

    fun shown(): List<Tool> = Tools.shown(Tools.claude.value, Antigravity.enabled.value)

    fun all() = shown().forEach(::now)

    fun now(tool: Tool) {
        if (tool == Tool.ANTIGRAVITY) {
            ag.value++
            Antigravity.pull()
            return
        }
        claude.value++
        if (!Hook.connected() || !busy.compareAndSet(false, true)) return
        thread(isDaemon = true, name = "banditboard-refresh") {
            try {
                Hook.refresh()
            } finally {
                busy.set(false)
            }
        }
    }
}
