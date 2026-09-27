package dev.clawdboard.desktop

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.concurrent.Executors

class Server(private val onPush: (JSONObject) -> Boolean) {
    var port = 0
        private set

    fun start(): Int {
        for (p in PORTS) {
            try {
                val s = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), p), 0)
                s.createContext("/") { handle(it) }
                s.executor = Executors.newFixedThreadPool(2) { r -> Thread(r, "banditboard-http").apply { isDaemon = true } }
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

    private fun same(a: String?, b: String) = a != null && MessageDigest.isEqual(a.trim().toByteArray(), b.toByteArray())

    companion object {
        val PORTS = 47810..47819
        const val MAX_BODY = 64 * 1024
    }
}
