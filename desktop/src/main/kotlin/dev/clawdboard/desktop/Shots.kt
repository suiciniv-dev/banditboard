package dev.clawdboard.desktop

import androidx.compose.ui.unit.sp
import dev.clawdboard.core.txt
import dev.clawdboard.core.React
import dev.clawdboard.core.ClaudeSession
import dev.clawdboard.core.Doing
import dev.clawdboard.core.Act
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
import dev.clawdboard.core.AgFamily
import dev.clawdboard.core.AgGroup
import dev.clawdboard.core.AgModel
import dev.clawdboard.core.AgPool
import dev.clawdboard.core.AgSnapshot
import dev.clawdboard.core.AlertKind
import dev.clawdboard.core.I18n
import dev.clawdboard.core.Tool
import dev.clawdboard.core.ToolTheme
import dev.clawdboard.core.agFeel
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
    Box(Modifier.fillMaxSize().padding(8.dp).shadow(10.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(P.bg)) {
        Dashboard(0, compact)
    }
}

fun main(args: Array<String>) {
    val out = File(args.first()).apply { mkdirs() }
    val now = System.currentTimeMillis()
    Antigravity.enabled.value = false
    Tools.claude.value = true
    Tools.theme.value = ToolTheme.FOLLOW
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
    Claude.watching.value = true
    Claude.showFile.value = true
    fun claude(model: String, act: Act, doing: Doing?, file: String? = null, seen: Long = now) =
        ClaudeSession(model, "Simix.Ponto.Cloud", "feature/facial", model, act, doing, file, now - 40_000L, seen)
    Claude.sessions.value = listOf(claude("opus", Act.WORKING, Doing.EDITING, "FacialService.cs"))
    shot(File(out, "windows-activity.png"), SIZE) { Card(false) }
    Claude.sessions.value = listOf(claude("opus", Act.WORKING, Doing.EDITING, "FacialService.cs", now - 5_000L), claude("sonnet", Act.PERMISSION, Doing.COMMAND))
    shot(File(out, "windows-activity-permission.png"), SIZE) { Card(false) }
    Claude.sessions.value = listOf(claude("opus", Act.RUNNING, Doing.TESTS))
    shot(File(out, "windows-activity-compact.png"), COMPACT) { Card(true) }
    val reactions = listOf(
        React.WORK to txt.activityState(Act.WORKING, Doing.THINKING, null),
        React.RUN to txt.activityState(Act.RUNNING, Doing.TESTS, null),
        React.ALERT to txt.activityState(Act.PERMISSION, null, null),
        React.OOPS to txt.activityState(Act.ERROR, null, null),
        React.SLEEP to txt.activityState(Act.IDLE, null, null),
    )
    shot(File(out, "racco-activity.png"), DpSize(760.dp, 150.dp)) {
        Row(Modifier.fillMaxSize().background(P.bg).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            reactions.forEachIndexed { i, (react, label) ->
                androidx.compose.foundation.layout.Column(Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Mascot(Modifier.width(110.dp), model = "Opus", seed = i, feel = feelOf(usage.value, "Opus", react))
                    androidx.compose.material3.Text(label, color = P.muted, fontSize = 13.sp, fontFamily = Fredoka, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
    Claude.watching.value = false
    shot(File(out, "racco.png"), DpSize(520.dp, 130.dp)) {
        Row(Modifier.fillMaxSize().background(P.bg).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MODELS.forEachIndexed { i, m -> Mascot(Modifier.width(110.dp), model = m, seed = i, feel = feelOf(usage.value, m)) }
        }
    }
    Antigravity.enabled.value = true
    Antigravity.open.value = true
    Antigravity.state.value = Antigravity.State.OK
    Antigravity.lastAt.value = now - 30_000L
    val session = now + 2 * 3_600_000L + 40 * 60_000L
    val week = now + 4 * 86_400_000L + 5 * 3_600_000L
    Antigravity.snapshot.value = AgSnapshot(
        listOf(
            AgModel("Gemini 3.1 Pro (High)", 38.0, session, false, AlertKind.SESSION),
            AgModel("Gemini 3.8 Flash (High)", 38.0, session, false, AlertKind.SESSION),
            AgModel("Claude Sonnet 4.6 (Thinking)", 88.0, week, false, AlertKind.WEEK),
            AgModel("Claude Opus 4.6 (Thinking)", 88.0, week, false, AlertKind.WEEK),
            AgModel("GPT-OSS 120B (Medium)", 88.0, week, false, AlertKind.WEEK),
        ),
        listOf(
            AgGroup(AgPool.GEMINI, UsageWindow(38.0, session), UsageWindow(21.0, week)),
            AgGroup(AgPool.OTHERS, UsageWindow(12.0, now + 3_600_000L), UsageWindow(88.0, week)),
        ),
        "Google AI Pro", now,
    )
    AgHistory.samples.value = (0 until 336).map { k -> start + k * 1_800_000L }.filter { zoned(it).hour in 10..20 }.map { t ->
        val h = zoned(t).hour + zoned(t).minute / 60.0 - 10
        Sample(t, (h % 5) / 5 * 38, 20.0 + (t - start) / (7 * 86_400_000.0) * 68)
    }
    val both = listOf(Tool.CLAUDE, Tool.ANTIGRAVITY)
    shot(File(out, "windows-widget-antigravity.png"), sizeOf(Layout.FULL, 2)) { Widget(both, 0, false) }
    shot(File(out, "windows-compact-antigravity.png"), sizeOf(Layout.COMPACT, 2)) { Widget(both, 0, true) }
    shot(File(out, "windows-mini-antigravity.png"), MINI) { Mini() }
    shot(File(out, "windows-dashboard-antigravity.png"), DpSize(1200.dp, 3400.dp)) {
        Themed(Tool.CLAUDE) { Panel(0, StatusSnapshot(emptySet(), emptyList(), now), controls) }
    }
    shot(File(out, "racco-antigravity.png"), DpSize(520.dp, 150.dp)) {
        Themed(Tool.ANTIGRAVITY) {
            Row(Modifier.fillMaxSize().background(P.bg).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AgFamily.entries.forEachIndexed { i, f ->
                    androidx.compose.foundation.layout.Column(Modifier.width(110.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Mascot(Modifier.width(110.dp), seed = i, wear = f.accessory, feel = agFeel(Antigravity.snapshot.value?.percent(f)))
                        androidx.compose.material3.Text(f.label, color = P.muted, fontSize = 13.sp, fontFamily = Fredoka)
                    }
                }
            }
        }
    }
    Tools.theme.value = ToolTheme.BLACK
    shot(File(out, "windows-widget-antigravity-black.png"), sizeOf(Layout.FULL, 2)) { Widget(both, 0, false) }
}
