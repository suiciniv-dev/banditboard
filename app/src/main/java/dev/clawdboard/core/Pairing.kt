package dev.clawdboard.core

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

class Pairing(private val app: Context) {
    private val sp = app.getSharedPreferences("pairing", Context.MODE_PRIVATE)

    val lastPushAt: Long? get() = sp.getLong("lastPushAt", 0L).takeIf { it > 0 }

    fun remember(key: String) {
        sp.edit().putString("hash", hash(key)).commit()
    }

    fun matches(key: String?): Boolean {
        val stored = sp.getString("hash", null) ?: return false
        if (key.isNullOrBlank()) return false
        return MessageDigest.isEqual(stored.toByteArray(), hash(key.trim()).toByteArray())
    }

    fun save(body: JSONObject, at: Long) {
        sp.edit().putString("last", body.toString()).putLong("lastPushAt", at).apply()
    }

    fun restore(now: Long): UsageSnapshot? {
        val raw = sp.getString("last", null) ?: return null
        val at = lastPushAt ?: return null
        return runCatching { parsePush(JSONObject(raw), at)?.settled(now) }.getOrNull()
    }

    fun clear() {
        sp.edit().clear().commit()
    }

    fun installer(url: String, key: String, id: String = "phone"): String {
        return asset("pc/install.ps1").replace("__USAGE__", asset("pc/usage.ps1").trimEnd())
            .replace("__T_CONNECTED__", txt.installConnected).replace("__T_BACKUP__", txt.installBackup(""))
            .replace("__T_EVERY__", txt.installEvery(url)).replace("__URL__", url).replace("__KEY__", key).replace("__ENC__", "").replace("__ACTIVITY__", "").replace("__ID__", id)
    }

    private fun asset(name: String) = app.assets.open(name).use { it.readBytes().toString(Charsets.UTF_8) }

    companion object {
        fun newKey(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }

        fun hash(key: String): String =
            MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        fun command(url: String, key: String) = "irm $url/pc/install.ps1?k=$key | iex"
    }
}
