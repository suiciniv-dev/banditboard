package dev.clawdboard.desktop

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.Executors

object Share {
    private val PORTS = 47830..47839
    private var server: HttpServer? = null
    val on = MutableStateFlow(false)
    var port = 0
        private set

    val key: String by lazy {
        Store.get("shareKey") ?: ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
            .also { Store.put("shareKey", it) }
    }

    fun restore() {
        if (Store.get("share") == "true") set(true)
    }

    @Synchronized
    fun set(enabled: Boolean) {
        Store.put("share", enabled.toString())
        if (enabled && server == null) start()
        if (!enabled) {
            server?.stop(0)
            server = null
            port = 0
        }
        on.value = server != null
    }

    private fun start() {
        for (p in PORTS) {
            try {
                val s = HttpServer.create(InetSocketAddress(p), 0)
                s.createContext("/") { handle(it) }
                s.executor = Executors.newFixedThreadPool(2) { r -> Thread(r, "banditboard-share").apply { isDaemon = true } }
                s.start()
                server = s
                port = p
                return
            } catch (_: IOException) {
            }
        }
    }

    private fun handle(ex: HttpExchange) = ex.use {
        val (status, body) = when {
            ex.requestMethod != "GET" || ex.requestURI.path != "/api/usage" -> 404 to JSONObject()
            !same(ex.requestHeaders.getFirst("X-Banditboard-Key")) -> 401 to JSONObject()
            else -> {
                val now = System.currentTimeMillis()
                val usage = Forward.envelope(now)
                200 to JSONObject()
                    .put("at", maxOf(Store.get("lastAt")?.toLongOrNull() ?: 0L, Claude.changedAt))
                    .put("now", now)
                    .put("usage", if (usage.has("five_hour") || usage.has("seven_day")) usage else JSONObject.NULL)
            }
        }
        val out = body.toString().toByteArray()
        ex.responseHeaders.add("Content-Type", "application/json")
        ex.responseHeaders.add("Cache-Control", "no-store")
        ex.sendResponseHeaders(status, out.size.toLong())
        ex.responseBody.write(out)
    }

    private fun same(a: String?) = a != null && MessageDigest.isEqual(a.trim().toByteArray(), key.toByteArray())

    fun addresses(): List<String> {
        val ips = runCatching {
            NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { it.inetAddresses.toList() }.filterIsInstance<Inet4Address>().filter { it.isSiteLocalAddress }
                .mapNotNull { it.hostAddress }
        }.getOrDefault(emptyList())
        val host = runCatching { InetAddress.getLocalHost().hostName.substringBefore('.') }.getOrNull()
            ?.takeIf { it.isNotBlank() && it != "localhost" }?.let { "$it.local" }
        return (listOfNotNull(host) + ips).distinct()
    }

    fun pairUri(): String {
        val urls = addresses().joinToString(",") { "http://$it:$port" }
        return "banditboard://pair?k=$key&u=" + URLEncoder.encode(urls, Charsets.UTF_8)
    }
}
