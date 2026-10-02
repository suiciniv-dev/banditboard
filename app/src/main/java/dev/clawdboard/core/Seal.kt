package dev.clawdboard.core

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Seal {
    private fun hmac(key: ByteArray, data: ByteArray): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(data)
    }

    private fun keys(master: String): Pair<ByteArray, ByteArray> {
        val root = ByteArray(master.length / 2) { i -> master.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
        return hmac(root, "banditboard-enc".toByteArray()) to hmac(root, "banditboard-mac".toByteArray())
    }

    fun close(plain: String, master: String): String {
        val (enc, mac) = keys(master)
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(enc, "AES"), IvParameterSpec(iv))
        val body = iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(body + hmac(mac, body))
    }

    fun open(blob: String, master: String): String? = runCatching {
        val all = Base64.getDecoder().decode(blob)
        if (all.size <= 48) return null
        val (enc, mac) = keys(master)
        val body = all.copyOfRange(0, all.size - 32)
        if (!MessageDigest.isEqual(hmac(mac, body), all.copyOfRange(all.size - 32, all.size))) return null
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(enc, "AES"), IvParameterSpec(body, 0, 16))
        String(cipher.doFinal(body, 16, body.size - 16), Charsets.UTF_8)
    }.getOrNull()

    fun level(o: org.json.JSONObject): String {
        fun of(name: String): Int {
            val pct = o.optJSONObject(name)?.num("used_percentage") ?: return 0
            return listOf(80, 90, 100).lastOrNull { pct >= it - 0.5 } ?: 0
        }
        val base = "${of("five_hour")},${of("seven_day")}"
        return agLevel(o.optJSONObject("antigravity"))?.let { "$base,$it" } ?: base
    }

    fun agLevel(ag: org.json.JSONObject?): String? {
        val pools = ag?.optJSONArray("pools")?.takeIf { it.length() > 0 } ?: return null
        fun code(w: org.json.JSONObject?): Char {
            val pct = w?.num("percent") ?: return '0'
            return when {
                pct >= 99.5 -> '1'
                pct >= 89.5 -> '9'
                pct >= 79.5 -> '8'
                else -> '0'
            }
        }
        val byPool = (0 until pools.length()).mapNotNull { pools.optJSONObject(it) }.associateBy { it.str("pool") }
        return AgPool.entries.joinToString("") { p ->
            val g = byPool[p.name]
            "${code(g?.optJSONObject("session"))}${code(g?.optJSONObject("week"))}"
        }
    }
}
