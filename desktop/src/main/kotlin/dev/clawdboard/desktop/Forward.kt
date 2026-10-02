package dev.clawdboard.desktop

import dev.clawdboard.core.Seal
import dev.clawdboard.core.activityJson
import dev.clawdboard.core.httpRequest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread

object Forward {
    private const val GAP_MS = 3_000L
    private const val MAX_BLOB = 11_000
    private val targets = File(Hook.claudeDir, "clawdboard-targets.json")
    private val lock = Object()
    private var dirty = false
    private var running = false
    private var lastSent = 0L

    fun envelope(now: Long): JSONObject {
        val o = Store.get("last")?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject()
        Store.get("lastAt")?.toLongOrNull()?.let { o.put("usage_at", it) }
        val activity = if (Claude.watching.value) activityJson(Claude.sessions.value, now, Claude.showFile.value) else JSONObject().put("off", true)
        o.put("activity", activity)
        Antigravity.phoneJson()?.let { o.put("antigravity", it) }
        return o
    }

    fun changed() {
        synchronized(lock) {
            dirty = true
            if (running) return
            running = true
        }
        thread(isDaemon = true, name = "banditboard-forward") {
            while (true) {
                val wait = GAP_MS - (System.currentTimeMillis() - lastSent)
                if (wait > 0) Thread.sleep(wait)
                synchronized(lock) {
                    if (!dirty) {
                        running = false
                        return@thread
                    }
                    dirty = false
                }
                lastSent = System.currentTimeMillis()
                runCatching { send(lastSent) }
            }
        }
    }

    private fun send(now: Long) {
        val list = runCatching { JSONArray(targets.readText()) }.getOrNull() ?: return
        val body = envelope(now)
        Hook.shareAgLevel(Seal.agLevel(body.optJSONObject("antigravity")))
        val plain = body.toString()
        for (i in 0 until list.length()) {
            val t = list.optJSONObject(i) ?: continue
            val url = t.optString("url").trimEnd('/')
            val key = t.optString("key")
            if (url.isEmpty() || key.isEmpty() || "127.0.0.1" in url || "localhost" in url) continue
            val enc = t.optString("enc").takeIf { it.length == 64 }
            val headers = mapOf("Content-Type" to "application/json", "X-Clawdboard" to "1", "X-Clawdboard-Key" to key)
            runCatching {
                if (enc != null) {
                    val blob = Seal.close(plain, enc).let { b ->
                        if (b.length <= MAX_BLOB || !body.has("antigravity")) b
                        else Seal.close(JSONObject(plain).apply { remove("antigravity") }.toString(), enc)
                    }
                    val sealed = JSONObject().put("blob", blob).put("lvl", Seal.level(body)).toString()
                    httpRequest("$url/push", "POST", headers, sealed, 8_000)
                } else {
                    httpRequest("$url/api/push", "POST", headers, plain, 4_000)
                }
            }
        }
    }
}
