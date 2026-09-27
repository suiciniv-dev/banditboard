package dev.clawdboard.desktop

import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.SecureRandom

object Store {
    val dir = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Banditboard").apply { mkdirs() }
    private val file = File(dir, "desktop.json")
    private val data = runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())

    @Synchronized
    fun get(name: String): String? = data.optString(name, "").ifEmpty { null }

    @Synchronized
    fun int(name: String): Int = data.optInt(name, 0)

    @Synchronized
    fun put(name: String, value: Any) {
        data.put(name, value)
        val tmp = File(dir, "desktop.json.tmp")
        tmp.writeText(data.toString())
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    val key: String by lazy {
        get("key") ?: ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }.also { put("key", it) }
    }
}
