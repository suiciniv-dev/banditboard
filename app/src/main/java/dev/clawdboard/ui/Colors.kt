package dev.clawdboard.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import dev.clawdboard.core.Backdrop
import dev.clawdboard.core.Tool
import dev.clawdboard.core.ToolTheme

data class Palette(
    val bg: Color,
    val card: Color,
    val card2: Color,
    val line: Color,
    val track: Color,
    val text: Color,
    val muted: Color,
    val dim: Color,
    val glow: Color,
    val accent: Color,
)

val LocalPalette = compositionLocalOf { C.palette(Tool.CLAUDE, ToolTheme.FOLLOW) }

object C {
    var bg by mutableStateOf(Color(0xFF16130F))
    var card by mutableStateOf(Color(0xFF1F1A14))
    var card2 by mutableStateOf(Color(0xFF262019))
    var line by mutableStateOf(Color(0xFF383024))
    var track by mutableStateOf(Color(0xFF2B2419))
    var text by mutableStateOf(Color(0xFFECE3D6))
    var muted by mutableStateOf(Color(0xFFA89A86))
    var dim by mutableStateOf(Color(0xFF7D715F))
    var ok by mutableStateOf(Color(0xFF8FB573))
    var warn by mutableStateOf(Color(0xFFF0A83C))
    var glow by mutableStateOf(Color(0x1FD97757))
    val clawd = Color(0xFFD77757)
    val deadBody = Color(0xFF5A5247)
    val eye = Color(0xFF0C0A08)
    val bad = Color(0xFFE5604D)
    val lav = Color(0xFFB9A6F2)
    val clockDigits = listOf(Color(0xFFF6A5C0), Color(0xFFF9C3D6), Color(0xFFF5D66A), Color(0xFFC8B4F5))

    fun apply(b: Backdrop) {
        when (b) {
            Backdrop.DEFAULT -> {
                bg = Color(0xFF16130F); card = Color(0xFF1F1A14); card2 = Color(0xFF262019)
                line = Color(0xFF383024); track = Color(0xFF2B2419)
                text = Color(0xFFECE3D6); muted = Color(0xFFA89A86); dim = Color(0xFF7D715F)
                ok = Color(0xFF8FB573); warn = Color(0xFFF0A83C); glow = Color(0x1FD97757)
            }
            Backdrop.BLACK -> {
                bg = Color(0xFF000000); card = Color(0xFF0E0E10); card2 = Color(0xFF18181B)
                line = Color(0xFF26262B); track = Color(0xFF1E1E22)
                text = Color(0xFFF3EFEA); muted = Color(0xFF9A958F); dim = Color(0xFF5E5A55)
                ok = Color(0xFF7CC68A); warn = Color(0xFFE9B44C); glow = Color(0x00000000)
            }
        }
    }

    val agAccent = Color(0xFF7AA2F7)

    fun palette(tool: Tool, theme: ToolTheme): Palette {
        val accent = if (tool == Tool.ANTIGRAVITY && theme != ToolTheme.CLAUDE) agAccent else clawd
        return when {
            theme == ToolTheme.BLACK -> Palette(
                Color(0xFF000000), Color(0xFF0E0E10), Color(0xFF18181B), Color(0xFF26262B), Color(0xFF1E1E22),
                Color(0xFFF3EFEA), Color(0xFF9A958F), Color(0xFF5E5A55), Color(0x00000000), accent,
            )
            tool == Tool.ANTIGRAVITY && theme == ToolTheme.FOLLOW -> Palette(
                Color(0xFF0F1218), Color(0xFF151A23), Color(0xFF1A202B), Color(0xFF283042), Color(0xFF1C2230),
                Color(0xFFE4E8F0), Color(0xFF98A2B3), Color(0xFF6B7487), Color(0x297AA2F7), accent,
            )
            else -> Palette(
                Color(0xFF16130F), Color(0xFF1F1A14), Color(0xFF262019), Color(0xFF383024), Color(0xFF2B2419),
                Color(0xFFECE3D6), Color(0xFFA89A86), Color(0xFF7D715F), Color(0x1FD97757), clawd,
            )
        }
    }

    fun level(p: Double): Color = when {
        p >= 85 -> bad
        p >= 60 -> warn
        else -> ok
    }
}
