package dev.clawdboard.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.clawdboard.core.txt
import dev.clawdboard.ui.C
import io.nayuki.qrcodegen.QrCode
import org.json.JSONArray
import java.awt.Desktop
import java.io.File
import java.net.URI

object Promo {
    const val URL = "https://github.com/suiciniv-dev/banditboard/releases/latest"
    private const val FIRST_DELAY = 2 * 3_600_000L
    private const val EVERY = 7 * 86_400_000L

    private fun phoneConnected() = runCatching {
        val a = JSONArray(File(System.getProperty("user.home"), ".claude/clawdboard-targets.json").readText())
        (0 until a.length()).any { a.optJSONObject(it)?.optString("id") == "phone" }
    }.getOrDefault(false)

    fun due(now: Long): Boolean {
        if (Store.get("promo") == "never" || phoneConnected()) return false
        val first = Store.get("firstRun")?.toLongOrNull() ?: now.also { Store.put("firstRun", it.toString()) }
        val last = Store.get("promoAt")?.toLongOrNull() ?: 0L
        return now - first >= FIRST_DELAY && now - last >= EVERY
    }

    fun shown(now: Long) = Store.put("promoAt", now.toString())

    fun never() = Store.put("promo", "never")

    fun open() {
        runCatching { Desktop.getDesktop().browse(URI(URL)) }
    }
}

private val code: QrCode by lazy { QrCode.encodeText(Promo.URL, QrCode.Ecc.MEDIUM) }

@Composable
internal fun Qr(modifier: Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(10.dp)).background(Color.White).padding(6.dp)) {
        val n = code.size
        val u = size.width / n
        for (y in 0 until n) for (x in 0 until n) {
            if (code.getModule(x, y)) drawRect(Color.Black, Offset(x * u, y * u), Size(u + 0.5f, u + 0.5f))
        }
    }
}

@Composable
internal fun PromoCard(onLater: () -> Unit, onNever: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(8.dp).shadow(10.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(C.bg)) {
        Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Qr(Modifier.size(118.dp).clickable { Promo.open() })
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(txt.promoTitle, color = C.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(txt.promoBody, color = C.muted, fontSize = 12.sp, lineHeight = 16.sp)
                Text(txt.promoScan, color = C.dim, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(txt.promoOpen, color = C.clawd, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { Promo.open(); onLater() })
                    Text(txt.promoLater, color = C.muted, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onLater))
                    Text(txt.promoNever, color = C.dim, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onNever))
                }
            }
        }
    }
}
