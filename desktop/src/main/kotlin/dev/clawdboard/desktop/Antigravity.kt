package dev.clawdboard.desktop

import dev.clawdboard.core.AgFamily
import dev.clawdboard.core.AgPool
import dev.clawdboard.core.AgParse
import dev.clawdboard.core.AgSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

object Antigravity {
    private const val BASE = "/exa.language_server_pb.LanguageServerService/"
    private const val HOST = "127.0.0.1"
    const val QUERY_MS = 90_000L
    const val CHECK_MS = 15_000L

    val enabled = MutableStateFlow(Store.get("tool_ag") == "true")
    val open = MutableStateFlow(false)
    val snapshot = MutableStateFlow(Store.get("ag_last")?.let { runCatching { AgSnapshot.fromJson(JSONObject(it)) }.getOrNull() })
    val lastAt = MutableStateFlow(Store.get("ag_lastAt")?.toLongOrNull() ?: 0L)

    private class Server(val pid: Long, val token: String, val ports: List<Int>)

    private var server: Server? = null
    private var endpoint: Pair<Int, Boolean>? = null
    private var queriedAt = 0L
    private val firstSeen: MutableMap<Long, Long> = runCatching {
        val o = JSONObject(Store.get("ag_resets") ?: "{}")
        o.keys().asSequence().associate { it.toLong() to o.getLong(it) }.toMutableMap()
    }.getOrDefault(mutableMapOf())

    private val localTls: SSLSocketFactory by lazy {
        val trust = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), SecureRandom()) }.socketFactory
    }

    fun setEnabled(on: Boolean) {
        Store.put("tool_ag", on.toString())
        enabled.value = on
        if (!on) forget()
        Forward.changed()
    }

    private fun forget() {
        server = null
        endpoint = null
        queriedAt = 0L
        open.value = false
    }

    fun tick(now: Long): AgSnapshot? {
        if (!enabled.value) {
            if (Store.get("tool_ag") != null || findPid() == null) return null
            setEnabled(true)
        }
        val pid = findPid()
        if (pid == null) {
            if (open.value) Forward.changed()
            forget()
            return null
        }
        if (server?.pid != pid) {
            server = inspect(pid) ?: return null
            endpoint = null
            queriedAt = 0L
        }
        if (open.value && now - queriedAt < QUERY_MS) return null
        val s = server ?: return null
        val body = query(s)
        if (body == null) {
            forget()
            return null
        }
        queriedAt = now
        val snap = AgParse.parse(body, now, firstSeen) ?: return null
        open.value = true
        snapshot.value = snap
        lastAt.value = now
        Store.put("ag_last", snap.toJson().toString())
        Store.put("ag_lastAt", now.toString())
        Store.put("ag_resets", JSONObject(firstSeen.mapKeys { it.key.toString() }).toString())
        return snap
    }

    fun phoneJson(): JSONObject? {
        if (!enabled.value) return null
        val s = snapshot.value ?: return null
        fun w(x: dev.clawdboard.core.UsageWindow?): Any = x?.let { JSONObject().put("percent", it.percent).put("resetsAt", it.resetsAt ?: JSONObject.NULL) } ?: JSONObject.NULL
        val models = JSONArray()
        AgFamily.entries.forEach { f ->
            s.family(f)?.let { m ->
                models.put(JSONObject().put("label", f.label).put("percent", m.percent).put("resetsAt", m.resetsAt ?: JSONObject.NULL).put("exhausted", m.exhausted))
            }
        }
        val pools = JSONArray()
        AgPool.entries.forEach { p ->
            s.pool(p)?.let { m ->
                pools.put(JSONObject().put("pool", p.name).put("percent", m.percent).put("resetsAt", m.resetsAt ?: JSONObject.NULL).put("kind", m.kind?.name ?: JSONObject.NULL))
            }
        }
        return JSONObject().put("open", open.value).put("pools", pools).put("plan", s.plan ?: JSONObject.NULL).put("usage_at", lastAt.value)
            .put("session", w(s.session)).put("week", w(s.week)).put("models", models)
    }

    private fun findPid(): Long? = ProcessHandle.allProcesses().use { all ->
        all.filter { h ->
            val cmd = h.info().command().orElse("").lowercase()
            "language_server" in cmd && "antigravity" in cmd
        }.findFirst().map { it.pid() }.orElse(null)
    }

    private fun inspect(pid: Long): Server? = runCatching {
        val lines = if (onMac) {
            listOf(run("ps", "-o", "command=", "-p", pid.toString())) +
                Regex(""":(\d+) \(LISTEN\)""").findAll(run("lsof", "-nP", "-a", "-iTCP", "-sTCP:LISTEN", "-p", pid.toString())).map { it.groupValues[1] }
        } else {
            val script = "(Get-CimInstance Win32_Process -Filter \"ProcessId=$pid\").CommandLine; " +
                "(Get-NetTCPConnection -State Listen -OwningProcess $pid -ErrorAction SilentlyContinue).LocalPort"
            run(Hook.powershell, "-NoProfile", "-NonInteractive", "-Command", script).lines()
        }
        val cmd = lines.firstOrNull().orEmpty()
        if ("antigravity" !in cmd.lowercase()) return null
        val token = Regex("""--csrf_token[ =]"?([^\s"]+)""").find(cmd)?.groupValues?.get(1) ?: return null
        val ports = lines.drop(1).mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..65535 }.distinct()
        if (ports.isEmpty()) null else Server(pid, token, ports)
    }.getOrNull()

    private fun run(vararg command: String): String {
        val p = ProcessBuilder(*command).redirectErrorStream(false).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor(10, TimeUnit.SECONDS)
        return out
    }

    private fun query(s: Server): String? {
        val known = endpoint
        if (known != null) post(known.first, known.second, "GetUserStatus", STATUS_BODY, s.token)?.let { return it }
        for (port in s.ports) for (https in listOf(true, false)) {
            if (post(port, https, "GetUnleashData", "{\"wrapper_data\":{}}", s.token) == null) continue
            endpoint = port to https
            return post(port, https, "GetUserStatus", STATUS_BODY, s.token)
        }
        return null
    }

    private const val STATUS_BODY = "{\"metadata\":{\"ideName\":\"antigravity\",\"extensionName\":\"antigravity\",\"locale\":\"en\"}}"

    private fun post(port: Int, https: Boolean, method: String, body: String, token: String): String? = runCatching {
        val url = URL("${if (https) "https" else "http"}://$HOST:$port$BASE$method")
        val c = url.openConnection(Proxy.NO_PROXY) as HttpURLConnection
        try {
            if (c is HttpsURLConnection) {
                c.sslSocketFactory = localTls
                c.hostnameVerifier = HostnameVerifier { host, _ -> host == HOST }
            }
            c.requestMethod = "POST"
            c.connectTimeout = 3_000
            c.readTimeout = 5_000
            c.useCaches = false
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("Connect-Protocol-Version", "1")
            c.setRequestProperty("X-Codeium-Csrf-Token", token)
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (c.responseCode != 200) null else c.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            c.disconnect()
        }
    }.getOrNull()
}
