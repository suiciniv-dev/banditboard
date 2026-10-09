package dev.clawdboard.desktop

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.concurrent.Executors

class Server(
    private val onHook: (JSONObject) -> Unit = {},
    private val onShow: () -> Unit = {},
    private val onRefresh: () -> Unit = {},
    private val onPush: (JSONObject) -> Boolean,
) {
    var port = 0
        private set

    fun start(): Int {
        for (p in PORTS) {
            try {
                val s = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), p), 0)
                s.createContext("/") { handle(it) }
                s.executor = Executors.newFixedThreadPool(4) { r -> Thread(r, "banditboard-http").apply { isDaemon = true } }
                s.start()
                port = p
                return p
            } catch (_: IOException) {
            }
        }
        error("no free port")
    }

    private fun handle(ex: HttpExchange) = ex.use {
        val host = ex.requestHeaders.getFirst("Host").orEmpty().substringBefore(':')
        val ok = host == "127.0.0.1" || host == "localhost"
        if (ok && ex.requestURI.path in DOCK) {
            dock(ex)
            return@use
        }
        if (ok && ex.requestMethod == "POST" && ex.requestURI.path == "/api/hook") {
            val allowed = ex.requestHeaders.getFirst("X-Clawdboard") == "1" && same(ex.requestHeaders.getFirst("X-Clawdboard-Key"), Store.key)
            if (allowed) {
                val body = ex.requestBody.readNBytes(MAX_HOOK).toString(Charsets.UTF_8)
                runCatching { onHook(JSONObject(body)) }
            }
            ex.sendResponseHeaders(if (allowed) 204 else 401, -1)
            return@use
        }
        val status = when {
            !ok -> 403
            ex.requestMethod != "POST" || ex.requestURI.path != "/api/push" -> 404
            ex.requestHeaders.getFirst("X-Clawdboard") != "1" -> 403
            !same(ex.requestHeaders.getFirst("X-Clawdboard-Key"), Store.key) -> 401
            else -> {
                val body = ex.requestBody.readNBytes(MAX_BODY).toString(Charsets.UTF_8)
                if (runCatching { onPush(JSONObject(body)) }.getOrDefault(false)) 200 else 400
            }
        }
        val out = "{}".toByteArray()
        ex.responseHeaders.add("Content-Type", "application/json")
        ex.sendResponseHeaders(status, out.size.toLong())
        ex.responseBody.write(out)
    }

    private fun dock(ex: HttpExchange) {
        val allowed = ex.requestHeaders.getFirst("X-Clawdboard") == "1" && same(ex.requestHeaders.getFirst("X-Clawdboard-Key"), Store.key)
        val path = ex.requestURI.path
        val out = when {
            !allowed -> null
            ex.requestMethod == "GET" && path == "/api/state" -> state().toString().toByteArray()
            ex.requestMethod == "POST" && path == "/api/show" -> ByteArray(0).also { onShow() }
            ex.requestMethod == "POST" && path == "/api/refresh" -> ByteArray(0).also { onRefresh() }
            else -> null
        }
        ex.responseHeaders.add("Cache-Control", "no-store")
        when {
            !allowed -> ex.sendResponseHeaders(401, -1)
            out == null -> ex.sendResponseHeaders(404, -1)
            out.isEmpty() -> ex.sendResponseHeaders(204, -1)
            else -> {
                ex.responseHeaders.add("Content-Type", "application/json")
                ex.sendResponseHeaders(200, out.size.toLong())
                ex.responseBody.write(out)
            }
        }
    }

    private fun state(): JSONObject {
        val now = System.currentTimeMillis()
        return JSONObject()
            .put("at", maxOf(Store.get("lastAt")?.toLongOrNull() ?: 0L, Claude.changedAt))
            .put("now", now)
            .put("usage", Forward.envelope(now))
    }

    private fun same(a: String?, b: String) = a != null && MessageDigest.isEqual(a.trim().toByteArray(), b.toByteArray())

    companion object {
        val PORTS = 47810..47819
        val DOCK = setOf("/api/state", "/api/show", "/api/refresh")
        const val MAX_BODY = 64 * 1024
        const val MAX_HOOK = 8 * 1024 * 1024
    }
}
