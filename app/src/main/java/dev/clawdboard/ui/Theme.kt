package dev.clawdboard.ui

import dev.clawdboard.core.ClaudeSession
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.clawdboard.R

@OptIn(ExperimentalTextApi::class)
private fun fredoka(w: FontWeight) =
    Font(R.font.fredoka, weight = w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))

val Fredoka = FontFamily(
    fredoka(FontWeight.Normal),
    fredoka(FontWeight.Medium),
    fredoka(FontWeight.SemiBold),
    fredoka(FontWeight.Bold),
)

val LocalNow = compositionLocalOf { System.currentTimeMillis() }

val LocalClaude = compositionLocalOf<List<ClaudeSession>?> { null }

@Composable
fun ClawdTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.clawd,
            onPrimary = Color.Black,
            secondary = C.lav,
            background = C.bg,
            onBackground = C.text,
            surface = C.card,
            onSurface = C.text,
            surfaceVariant = C.card2,
            onSurfaceVariant = C.muted,
            outline = C.line,
            error = C.bad,
        ),
        content = content,
    )
}
