package dev.clawdboard.core

import android.content.Context
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Remote(app: Context) {
    data class Source(val urls: List<String>, val key: String, val seal: String?)

    private val sp = app.getSharedPreferences("remote", Context.MODE_PRIVATE)

    val source: Source?
        get() {
            val key = sp.getString("key", null) ?: return null
            val urls = sp.getString("urls", null)?.split(",")?.filter { it.isNotBlank() }.orEmpty()
            return if (urls.isEmpty()) null else Source(urls, key, sp.getString("seal", null))
        }

    val paired: Boolean get() = source != null

    var lastAt: Long
        get() = sp.getLong("at", 0L)
        set(v) {
            sp.edit().putLong("at", v).apply()
        }

    fun pair(link: String): Boolean {
        val s = parse(link) ?: return false
        sp.edit().clear().putString("urls", s.urls.joinToString(",")).putString("key", s.key).putString("seal", s.seal).commit()
        return true
    }

    fun clear() {
        sp.edit().clear().commit()
    }

    fun fetch(): Pair<JSONObject, Long>? {
        val s = source ?: return null
        if (s.seal != null) {
            val r = runCatching { httpRequest(s.urls.first(), headers = mapOf("X-Clawdboard-Key" to s.key), timeoutMs = 10_000) }.getOrNull()
            if (r?.code != 200) return null
            val o = runCatching { JSONObject(r.body) }.getOrNull() ?: return null
            val blob = o.optString("blob", "").takeIf { it.isNotEmpty() && it != "null" } ?: return null
            val plain = open(blob, s.seal) ?: return null
            return runCatching { JSONObject(plain) }.getOrNull()?.let { it to o.optLong("at", 0L) }
        }
        s.urls.forEachIndexed { i, base ->
            val r = runCatching { httpRequest("$base/api/usage", headers = mapOf("X-Banditboard-Key" to s.key), timeoutMs = 5_000) }.getOrNull()
            if (r?.code != 200) return@forEachIndexed
            val o = runCatching { JSONObject(r.body) }.getOrNull() ?: return@forEachIndexed
            val usage = o.optJSONObject("usage") ?: return null
            if (i > 0) sp.edit().putString("urls", (listOf(base) + s.urls.filter { it != base }).joinToString(",")).apply()
            return usage to o.optLong("at", 0L)
        }
        return null
    }

    companion object {
        fun parse(link: String): Source? {
            val uri = runCatching { URI(link.trim()) }.getOrNull() ?: return null
            if (uri.scheme != "banditboard") return null
            val query = (uri.rawQuery ?: return null).split("&").mapNotNull { part ->
                val eq = part.indexOf('=')
                if (eq <= 0) null else part.substring(0, eq) to URLDecoder.decode(part.substring(eq + 1), "UTF-8")
            }.toMap()
            fun q(name: String) = query[name]?.takeIf { it.isNotBlank() }
            return when (uri.host) {
                "pair" -> {
                    val key = q("k") ?: return null
                    val urls = q("u")?.split(",")?.map { it.trim() }?.filter { it.startsWith("http") }.orEmpty()
                    if (urls.isEmpty()) null else Source(urls, key, null)
                }
                "box" -> {
                    val worker = q("w")?.takeIf { it.startsWith("https://") } ?: return null
                    val box = q("b") ?: return null
                    val read = q("r") ?: return null
                    val master = q("e")?.takeIf { it.length == 64 } ?: return null
                    Source(listOf("$worker/v1/box/$box"), read, master)
                }
                else -> null
            }
        }

        fun open(blob: String, master: String): String? = runCatching {
            val all = Base64.getDecoder().decode(blob)
            if (all.size <= 48) return null
            val root = ByteArray(master.length / 2) { i -> master.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
            fun hmac(key: ByteArray, data: ByteArray): ByteArray = Mac.getInstance("HmacSHA256").run {
                init(SecretKeySpec(key, "HmacSHA256"))
                doFinal(data)
            }
            val enc = hmac(root, "banditboard-enc".toByteArray())
            val mac = hmac(root, "banditboard-mac".toByteArray())
            val body = all.copyOfRange(0, all.size - 32)
            val tag = all.copyOfRange(all.size - 32, all.size)
            if (!MessageDigest.isEqual(hmac(mac, body), tag)) return null
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(enc, "AES"), IvParameterSpec(body, 0, 16))
            String(cipher.doFinal(body, 16, body.size - 16), Charsets.UTF_8)
        }.getOrNull()
    }
}
