package dev.clawdboard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.clawdboard.core.Language
import dev.clawdboard.core.Nudge
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.clawdboard.BuildConfig
import dev.clawdboard.core.Accessory
import dev.clawdboard.core.Backdrop
import dev.clawdboard.core.ToolTheme
import dev.clawdboard.core.Brightness
import dev.clawdboard.core.Orientation
import dev.clawdboard.core.Prefs
import dev.clawdboard.core.Repository
import dev.clawdboard.core.ScreenMode
import dev.clawdboard.core.Skin
import dev.clawdboard.core.Species
import dev.clawdboard.core.Tint
import dev.clawdboard.core.txt
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(repo: Repository, st: Repository.State, prefs: Prefs, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val now = LocalNow.current

    var pairMsg by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var curPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var newPin2 by remember { mutableStateOf("") }
    var pinMsg by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(txt.settings, color = C.text, fontSize = 28.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClose) { Text(txt.close, color = C.clawd, fontSize = 16.sp) }
            }
            st.panelUrl?.let { Text(txt.webPanel(it), color = C.muted, fontSize = 14.sp) }

            Section(txt.claudeOnPc)
            val at = st.lastPushAt
            Text(
                if (at != null) txt.lastPush(fmtAgo(now - at)) else txt.noPushYet,
                color = if (at != null) C.ok else C.warn, fontSize = 15.sp,
            )
            st.remote?.let { Text(if (it.seal != null) txt.remoteBox else txt.remoteLan, color = C.muted, fontSize = 14.sp) }
            if (repo.vault.isProvisioned) Hint(txt.usageExplain(st.panelUrl))
            Hint(txt.scanQrHint)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ScanButton(repo) { ok, m -> pairMsg = ok to m }
                if (st.remote != null) OutlinedButton(onClick = { repo.unpairRemote(); if (!repo.vault.isProvisioned) onClose() }) {
                    Text(txt.remoteUnpair, color = C.text)
                }
                if (repo.vault.isProvisioned) OutlinedButton(onClick = {
                    scope.launch {
                        pairMsg = when (val r = repo.newPairKey()) {
                            Repository.Outcome.Ok -> true to txt.newKeyDone
                            is Repository.Outcome.Error -> false to r.message
                            else -> false to txt.newKeyFailed
                        }
                    }
                }) { Text(txt.newKey, color = C.text) }
            }
            pairMsg?.let { (ok, m) -> Text(m, color = if (ok) C.ok else C.bad, fontSize = 14.sp) }

            Section(txt.tools)
            val clipboard = LocalClipboardManager.current
            var copied by remember { mutableStateOf(false) }
            val command = st.panelUrl?.removePrefix("http://")?.let { repo.pairCommand(it) }
            ToolRow(
                txt.toolClaude, C.clawd, st.lastPushAt?.let { txt.toolConnected(fmtAgo(now - it)) } ?: txt.toolNoData,
                prefs.showClaude, enabled = prefs.showAg, onShow = { v -> repo.updateSettings { it.copy(showClaude = v) } },
                command = command?.let { c -> { clipboard.setText(AnnotatedString(c)); copied = true } },
            ) { Mascot(Modifier.width(52.dp), model = "Fable", reserveTop = false) }
            Spacer(Modifier.height(10.dp))
            val agPal = agPalette(prefs)
            ToolRow(
                txt.toolAntigravity, agPal.accent, st.ag?.let { txt.toolConnected(fmtAgo(now - it.snap.fetchedAt)) } ?: txt.toolNoData,
                prefs.showAg, enabled = prefs.showClaude, onShow = { v -> repo.updateSettings { it.copy(showAg = v) } },
            ) { Mascot(Modifier.width(52.dp), wear = Accessory.STAR, reserveTop = false) }
            if (copied) Text(txt.commandCopied, color = C.ok, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
            Hint(txt.agNeedsPc)
            Label(txt.theme)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolTheme.entries.forEach { t ->
                    Chip(txt.label(t), agTheme(prefs) == t) {
                        repo.updateSettings { it.copy(toolTheme = t, backdrop = if (t == ToolTheme.BLACK) Backdrop.BLACK else Backdrop.DEFAULT) }
                    }
                }
            }
            Hint(txt.themeHint)

            Section(txt.screen)
            Label(txt.language)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Language.entries.forEach { l -> Chip(txt.label(l), prefs.language == l) { repo.updateSettings { it.copy(language = l) } } }
            }
            Label(txt.mode)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScreenMode.entries.forEach { m -> Chip(txt.label(m), prefs.mode == m) { repo.updateSettings { it.copy(mode = m) } } }
            }
            if (prefs.mode == ScreenMode.CAROUSEL) {
                Label(txt.dwell)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Prefs.DWELL_OPTIONS.forEach { d -> Chip("${d}s", prefs.dwellSec == d) { repo.updateSettings { it.copy(dwellSec = d) } } }
                }
            }
            Label(txt.backdrop)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Backdrop.entries.forEach { b -> Chip(txt.label(b), prefs.backdrop == b) { repo.updateSettings { it.copy(backdrop = b) } } }
            }
            Label(txt.brightness)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Brightness.entries.forEach { b -> Chip(txt.label(b), prefs.brightness == b) { repo.updateSettings { it.copy(brightness = b) } } }
            }
            Label(txt.orientation)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Orientation.entries.forEach { o -> Chip(txt.label(o), prefs.orientation == o) { repo.updateSettings { it.copy(orientation = o) } } }
            }
            Label(txt.zoom)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Prefs.ZOOM_OPTIONS.forEach { z -> Chip("$z%", prefs.zoom == z) { repo.updateSettings { it.copy(zoom = z) } } }
            }
            Hint(txt.zoomHint)
            Toggle(txt.pixelShift, txt.pixelShiftHint, prefs.pixelShift) { v -> repo.updateSettings { it.copy(pixelShift = v) } }
            Toggle(txt.autostart, null, prefs.autostart) { v -> repo.updateSettings { it.copy(autostart = v) } }
            Toggle(txt.alerts, txt.alertsHint, prefs.alerts) { v -> repo.updateSettings { it.copy(alerts = v) } }
            Toggle(txt.panelToggle, st.panelUrl, prefs.panelEnabled) { v -> repo.updateSettings { it.copy(panelEnabled = v) } }

            Section(txt.mascots)
            MascotRow(st.status, Modifier.widthIn(max = 420.dp), usage = st.usage, mascotWidth = 72.dp)
            Label(txt.mascot)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Species.entries.filter { it != Species.CLAWD || prefs.clawdUnlocked }.forEach { m ->
                    Chip(txt.label(m), prefs.mascot() == m) { repo.updateSettings { it.copy(species = m) } }
                }
            }
            Label(txt.accessories)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Skin.entries.forEach { s -> Chip(txt.label(s), prefs.skin == s) { repo.updateSettings { it.copy(skin = s) } } }
            }
            if (prefs.skin == Skin.MODELS) Hint(txt.modelsHint)
            Label(txt.color)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Tint.entries.forEach { t -> Chip(txt.label(t), prefs.tint == t) { repo.updateSettings { it.copy(tint = t) } } }
            }
            Toggle(
                txt.animations,
                txt.animationsHint,
                prefs.animations,
            ) { v -> repo.updateSettings { it.copy(animations = v) } }

            Section(txt.music)
            Toggle(
                txt.musicScreen,
                txt.musicScreenHint,
                prefs.music,
            ) { v -> repo.updateSettings { it.copy(music = v) } }
            if (prefs.music) {
                val access by repo.music.access.collectAsStateWithLifecycle()
                if (access) {
                    Spacer(Modifier.height(8.dp))
                    Text(txt.playerAccessOk, color = C.ok, fontSize = 14.sp)
                    if (!prefs.animations) Hint(txt.noDanceWithoutAnimations)
                } else {
                    Hint(txt.accessWhy)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = C.clawd, contentColor = C.bg),
                        onClick = { repo.music.openAccess(context) },
                    ) { Text(txt.grantAccess) }
                    Hint(txt.restrictedHint)
                }
            }

            if (repo.vault.isProvisioned) Section(txt.changePin)
            if (repo.vault.isProvisioned) SecretField(curPin, { curPin = it.filter(Char::isDigit).take(8) }, txt.currentPin, numeric = true)
            if (repo.vault.isProvisioned) SecretField(newPin, { newPin = it.filter(Char::isDigit).take(8) }, txt.newPin, numeric = true)
            if (repo.vault.isProvisioned) SecretField(newPin2, { newPin2 = it.filter(Char::isDigit).take(8) }, txt.repeatNewPin, numeric = true)
            if (repo.vault.isProvisioned) Button(
                colors = ButtonDefaults.buttonColors(containerColor = C.card2, contentColor = C.text),
                onClick = {
                    if (newPin != newPin2) {
                        pinMsg = false to txt.newPinsDontMatch
                        return@Button
                    }
                    scope.launch {
                        pinMsg = when (val r = repo.changePin(curPin, newPin)) {
                            Repository.Outcome.Ok -> { curPin = ""; newPin = ""; newPin2 = ""; true to txt.pinChanged }
                            is Repository.Outcome.WrongPin -> false to txt.wrongCurrentPin(r.remaining)
                            is Repository.Outcome.Error -> false to r.message
                            Repository.Outcome.Wiped -> null
                        }
                    }
                },
            ) { Text(txt.changePin) }
            pinMsg?.let { (ok, m) -> Text(m, color = if (ok) C.ok else C.bad, fontSize = 14.sp) }

            Section(txt.security)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (repo.vault.isProvisioned) OutlinedButton(onClick = { repo.lock(); onClose() }) { Text(txt.lockNow, color = C.text) }
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = if (confirmReset) C.bad else C.card2, contentColor = C.text),
                    onClick = {
                        if (confirmReset) {
                            repo.factoryReset(); onClose()
                        } else {
                            confirmReset = true
                        }
                    },
                ) { Text(if (confirmReset) txt.tapAgainToErase else txt.eraseAll) }
            }
            Hint(txt.wipeHint)

            Section(txt.credits)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mascot(Modifier.width(72.dp), seed = 99, reserveTop = false)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Banditboard ${BuildConfig.VERSION_NAME}", color = C.text, fontSize = 18.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
                    Text(txt.createdBy, color = C.clawd, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Credit(
                txt.mascot,
                if (prefs.mascot() == Species.CLAWD) txt.mascotClawd else txt.mascotRacco,
            )
            if (prefs.mascot() != Species.CLAWD) Text(
                txt.whyRaccoon,
                color = C.text, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
            )
            Credit(txt.data, txt.dataSources)
            Credit(txt.feedback, Nudge.EMAIL, C.clawd) { sendFeedback(context) }
            Hint(txt.fanProject)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ToolRow(
    name: String,
    dot: Color,
    status: String,
    shown: Boolean,
    enabled: Boolean,
    onShow: (Boolean) -> Unit,
    command: (() -> Unit)? = null,
    mascot: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(C.card).border(1.dp, C.line, shape).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        mascot()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(8.dp))
                Text(name, color = C.text, fontSize = 17.sp, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold)
            }
            Text(status, color = C.muted, fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(enabled = enabled || !shown) { onShow(!shown) }) {
            Checkbox(shown, { onShow(it) }, enabled = enabled || !shown, colors = CheckboxDefaults.colors(checkedColor = C.clawd, checkmarkColor = C.bg))
            Text(txt.trayShow, color = C.text, fontSize = 14.sp)
        }
        if (command != null) {
            Spacer(Modifier.width(10.dp))
            OutlinedButton(onClick = command) { Text(txt.copyCommand, color = C.text, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(22.dp))
    Text(title, color = C.clawd, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Label(text: String) {
    Spacer(Modifier.height(10.dp))
    Text(text, color = C.muted, fontSize = 13.sp)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun Hint(text: String) {
    Spacer(Modifier.height(6.dp))
    Text(text, color = C.dim, fontSize = 13.sp)
}

@Composable
private fun Toggle(title: String, desc: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = C.text, fontSize = 15.sp)
            desc?.let { Text(it, color = C.dim, fontSize = 13.sp) }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = C.bg, checkedTrackColor = C.clawd),
        )
    }
}

@Composable
private fun Credit(title: String, text: String, color: Color = C.text, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 3.dp)) {
        Text(title, color = C.muted, fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Text(text, color = color, fontSize = 13.sp)
    }
}
