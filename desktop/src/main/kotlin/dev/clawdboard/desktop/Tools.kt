package dev.clawdboard.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.clawdboard.core.Tool
import dev.clawdboard.core.ToolTheme
import dev.clawdboard.ui.C
import dev.clawdboard.ui.LocalPalette
import dev.clawdboard.ui.Palette
import kotlinx.coroutines.flow.MutableStateFlow

internal val P: Palette
    @Composable @ReadOnlyComposable get() = LocalPalette.current

object Tools {
    val claude = MutableStateFlow(Store.get("tool_claude") != "false")
    val theme = MutableStateFlow(runCatching { ToolTheme.valueOf(Store.get("theme") ?: "") }.getOrDefault(ToolTheme.FOLLOW))

    fun setClaude(on: Boolean) {
        Store.put("tool_claude", on.toString())
        claude.value = on
    }

    fun setTheme(t: ToolTheme) {
        Store.put("theme", t.name)
        theme.value = t
    }

    fun shown(claudeOn: Boolean, agOn: Boolean): List<Tool> =
        listOfNotNull(if (claudeOn || !agOn) Tool.CLAUDE else null, if (agOn) Tool.ANTIGRAVITY else null)

    fun palette(tool: Tool): Palette = C.palette(tool, theme.value)
}

@Composable
internal fun Themed(tool: Tool, content: @Composable () -> Unit) {
    val t by Tools.theme.collectAsState()
    CompositionLocalProvider(LocalPalette provides C.palette(tool, t), content = content)
}

@Composable
internal fun shownTools(): List<Tool> {
    val c by Tools.claude.collectAsState()
    val a by Antigravity.enabled.collectAsState()
    return Tools.shown(c, a)
}
