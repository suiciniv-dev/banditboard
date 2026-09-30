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
import dev.clawdboard.core.NewsItem
import dev.clawdboard.core.Sample
import dev.clawdboard.core.StatusSnapshot
import dev.clawdboard.core.ScopedLimit
import dev.clawdboard.core.UsageSnapshot
import dev.clawdboard.core.UsageWindow
import dev.clawdboard.core.feelOf
import dev.clawdboard.ui.C
import dev.clawdboard.ui.Mascot
import dev.clawdboard.ui.zoned
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
    I18n.language = if (args.getOrNull(1) == "pt") Language.PT else Language.EN
    shot(File(out, "windows-widget.png"), SIZE) { Card(false) }
    shot(File(out, "windows-compact.png"), COMPACT) { Card(true) }
    shot(File(out, "windows-mini.png"), MINI) { Mini() }
    shot(File(out, "windows-promo.png"), DpSize(440.dp, 170.dp)) { PromoCard({}, {}) }
    val start = now - 7 * 86_400_000L
    DeskHistory.samples.value = (0 until 336).map { k -> start + k * 1_800_000L }.filter { zoned(it).hour in 9..21 }.map { t ->
        val h = zoned(t).hour + zoned(t).minute / 60.0 - 9
        Sample(t, (h % 5) / 5 * 45, 18.0 + (t - start) / (7 * 86_400_000.0) * 43)
    }
    newsFlow.value = listOf(
        NewsItem("Introducing the next Claude models", "", now - 2 * 86_400_000L, null),
        NewsItem("Claude Code gets background agents", "", now - 5 * 86_400_000L, null),
        NewsItem("Building safer AI systems together", "", now - 9 * 86_400_000L, null),
    )
    val controls = WidgetControls(Layout.MINI, {}, true, {}, false, {}, false, {}, true, {}, true, {})
    shot(File(out, "windows-dashboard.png"), DpSize(1200.dp, 2180.dp)) {
        Panel(0, StatusSnapshot(emptySet(), emptyList(), now), controls)
    }
    shot(File(out, "racco.png"), DpSize(520.dp, 130.dp)) {
        Row(Modifier.fillMaxSize().background(C.bg).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MODELS.forEachIndexed { i, m -> Mascot(Modifier.width(110.dp), model = m, seed = i, feel = feelOf(usage.value, m)) }
        }
    }
}
