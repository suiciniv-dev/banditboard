package dev.clawdboard.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.clawdboard.core.I18n
import dev.clawdboard.core.Language
import dev.clawdboard.core.MODELS
import dev.clawdboard.core.ScopedLimit
import dev.clawdboard.core.UsageSnapshot
import dev.clawdboard.core.UsageWindow
import dev.clawdboard.core.feelOf
import dev.clawdboard.ui.C
import dev.clawdboard.ui.Mascot
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

private fun shot(file: File, size: DpSize, content: @Composable () -> Unit) {
    val scale = 2f
    val scene = ImageComposeScene((size.width.value * scale).toInt(), (size.height.value * scale).toInt(), Density(scale)) { content() }
    scene.render()
    val png = scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes
    scene.close()
    file.writeBytes(png)
}

@Composable
private fun Card(compact: Boolean) {
    Box(Modifier.fillMaxSize().padding(8.dp).shadow(10.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(C.bg)) {
        Dashboard(0, compact)
    }
}

fun main(args: Array<String>) {
    val out = File(args.first()).apply { mkdirs() }
    val now = System.currentTimeMillis()
    usage.value = UsageSnapshot(
        UsageWindow(42.0, now + 2 * 3_600_000L + 12 * 60_000L),
        UsageWindow(61.0, now + 3 * 86_400_000L),
        listOf(ScopedLimit("Fable", 22.0, now + 3 * 86_400_000L)),
        now,
    )
    pushedAt.value = now - 60_000L
    I18n.language = Language.EN
    shot(File(out, "windows-widget.png"), SIZE) { Card(false) }
    shot(File(out, "windows-compact.png"), COMPACT) { Card(true) }
    shot(File(out, "windows-mini.png"), MINI) { Mini {} }
    shot(File(out, "windows-promo.png"), DpSize(440.dp, 170.dp)) { PromoCard({}, {}) }
    shot(File(out, "racco.png"), DpSize(520.dp, 130.dp)) {
        Row(Modifier.fillMaxSize().background(C.bg).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MODELS.forEachIndexed { i, m -> Mascot(Modifier.width(110.dp), model = m, seed = i, feel = feelOf(usage.value, m)) }
        }
    }
}
