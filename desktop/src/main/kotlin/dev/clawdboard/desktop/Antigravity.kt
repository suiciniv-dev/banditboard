package dev.clawdboard.desktop

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import dev.clawdboard.core.AgFamily
import dev.clawdboard.core.AgPool
import dev.clawdboard.core.AgParse
import dev.clawdboard.core.AgSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

object Antigravity {
    enum class State { OFF, CLOSED, STARTING, NO_ACCESS, UNSUPPORTED, NO_ANSWER, OK }

    private const val BASE = "/exa.language_server_pb.LanguageServerService/"
    private val HOSTS = listOf("127.0.0.1", "[::1]")
    const val QUERY_MS = 90_000L
    const val CHECK_MS = 15_000L

    val enabled = MutableStateFlow(Store.get("tool_ag") == "true")
    val state = MutableStateFlow(State.OFF)
    val open = MutableStateFlow(false)
    val snapshot = MutableStateFlow(Store.get("ag_last")?.let { runCatching { AgSnapshot.fromJson(JSONObject(it)) }.getOrNull() })
    val lastAt = MutableStateFlow(Store.get("ag_lastAt")?.toLongOrNull() ?: 0L)
    val diagnosis = MutableStateFlow("")

    private class Server(val pid: Long, val token: String, val ports: List<Int>)
    private class Endpoint(val host: String, val port: Int, val https: Boolean)

    private var server: Server? = null
    private var endpoint: Endpoint? = null
    private var queriedAt = 0L
    private val cimTried = mutableSetOf<Long>()
    private var logKey = ""
    private var logRead: Pair<Long?, List<Int>> = null to emptyList()
    private var reported = ""
    private var steps: List<String> = emptyList()
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
        if (!on) {
            forget()
            setState(State.OFF)
        }
        Forward.changed()
    }

    private fun forget() {
        server = null
        endpoint = null
        queriedAt = 0L
    }

    private fun setState(s: State) {
        val wasOpen = open.value
        state.value = s
        open.value = s == State.OK
        if (wasOpen != open.value) Forward.changed()
    }

    @Synchronized
    fun export(now: Long): File {
        val log = mutableListOf<String>()
        val (found, sure, why) = discover(log)
        if (found != null) {
            val body = query(found, log)
            val snap = body?.let { AgParse.parse(it, now) }
            log += when {
                body == null -> "GetUserStatus: sem resposta em nenhuma porta"
                snap == null -> "GetUserStatus: formato desconhecido (${body.length} bytes)"
                else -> "GetUserStatus: ok, ${snap.models.size} modelos, plano ${snap.plan ?: "?"}"
            }
        } else log += "resultado da busca: $why (Antigravity confirmado: $sure)"
        val s = snapshot.value
        val text = listOf(
            "Banditboard ${System.getProperty("jpackage.app-version").orEmpty()} · ${System.getProperty("os.name")} ${System.getProperty("os.version")} · Java ${System.getProperty("java.version")}",
            "gerado em ${Instant.ofEpochMilli(now)}",
            "Antigravity ligado: ${enabled.value} (escolha salva: ${Store.get("tool_ag") ?: "nenhuma"}) · estado ${state.value}",
            "última leitura: ${if (lastAt.value > 0) Instant.ofEpochMilli(lastAt.value) else "nunca"}",
            "cotas: " + (s?.let { AgPool.entries.joinToString { p -> "$p ${it.pool(p)?.percent ?: "-"}% ${it.pool(p)?.kind ?: ""}" } } ?: "nenhuma"),
            "",
            "checagem agora:",
        ) + log.map { "  $it" } + listOf("", "último registro:") + diagnosis.value.lines().map { "  $it" }
        val dir = listOf(File(System.getProperty("user.home"), "Downloads"), File(System.getProperty("user.home"))).first { it.isDirectory }
        val file = File(dir, "Banditboard-diagnostico-antigravity.txt")
        file.writeText(masked(text.joinToString("\n")) + "\n")
        runCatching {
            if (onMac) ProcessBuilder("open", "-R", file.absolutePath).start()
            else ProcessBuilder("explorer.exe", "/select,", file.absolutePath).start()
        }
        return file
    }

    @Synchronized
    fun tick(now: Long): AgSnapshot? {
        val auto = !enabled.value && Store.get("tool_ag") == null
        if (!enabled.value && !auto) return null
        server?.let { if (!alive(it.pid)) forget() }
        if (server == null) {
            val log = mutableListOf<String>()
            val (found, sure, why) = discover(log)
            if (sure && auto) setEnabled(true)
            if (!enabled.value) return null
            steps = log
            if (found == null) {
                setState(why)
                report(now, log)
                return null
            }
            server = found
            endpoint = null
            queriedAt = 0L
        }
        if (open.value && now - queriedAt < QUERY_MS) return null
        val s = server ?: return null
        val log = mutableListOf<String>()
        val body = query(s, log)
        if (body == null) {
            forget()
            setState(State.NO_ANSWER)
            report(now, log)
            return null
        }
        queriedAt = now
        val snap = AgParse.parse(body, now, firstSeen)
        if (snap == null) {
            log += "GetUserStatus respondeu num formato desconhecido (${body.length} bytes)"
            setState(State.UNSUPPORTED)
            report(now, log)
            return null
        }
        setState(State.OK)
        report(now, steps + log + "cota lida: ${snap.models.size} modelos")
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

    private data class Found(val server: Server?, val sure: Boolean, val why: State)

    private class Candidate(val pid: Long, val ports: List<Int>, val fromLog: Boolean)

    private fun discover(log: MutableList<String>): Found {
        val candidates = mutableListOf<Candidate>()
        fromLog(log)?.let { candidates += it }
        scan().filter { pid -> candidates.none { it.pid == pid } }.forEach { candidates += Candidate(it, emptyList(), false) }
        log += "candidatos: " + candidates.joinToString { "${it.pid}${if (it.fromLog) " (log)" else ""} ${exe(it.pid)}" }.ifEmpty { "nenhum" }
        if (candidates.isEmpty()) return Found(null, false, State.CLOSED)
        var sure = false
        var why = State.CLOSED
        for (c in candidates) {
            if (c.fromLog || "antigravity" in exe(c.pid).lowercase()) sure = true
            val cmd = commandLine(c.pid, log)
            if (cmd == null) {
                if (c.fromLog || "antigravity" in exe(c.pid).lowercase()) why = State.NO_ACCESS
                continue
            }
            if ("antigravity" !in cmd.lowercase()) {
                log += "${c.pid}: não é do Antigravity"
                continue
            }
            sure = true
            val token = Regex("""--csrf_token[ =]"?([^\s"]+)""").find(cmd)?.groupValues?.get(1)
            if (token == null) {
                log += "${c.pid}: sem --csrf_token na linha de comando"
                if (why != State.NO_ACCESS) why = State.UNSUPPORTED
                continue
            }
            val ports = c.ports.ifEmpty { listening(c.pid, log) }
            log += "${c.pid}: csrf ok, portas ${ports.joinToString().ifEmpty { "nenhuma" }}"
            if (ports.isEmpty()) {
                why = State.STARTING
                continue
            }
            return Found(Server(c.pid, token, ports), true, State.OK)
        }
        return Found(null, sure, why)
    }

    private fun logDirs(): List<File> {
        val home = System.getProperty("user.home")
        val roots = if (onMac) listOf(File(home, "Library/Application Support"))
        else listOfNotNull(System.getenv("APPDATA")?.let(::File), File(home, "AppData/Roaming"))
        return roots.map { File(it, "Antigravity/logs") }.distinctBy { it.absolutePath.lowercase() }
    }

    private fun fromLog(log: MutableList<String>): Candidate? {
        val file = logDirs().map { File(it, "language_server.log") }.filter { it.isFile }.maxByOrNull { it.lastModified() }
        if (file == null) {
            log += "log do language server não encontrado em ${logDirs().joinToString()}"
            return null
        }
        val key = "${file.path}|${file.lastModified()}|${file.length()}"
        if (key != logKey) {
            val lines = runCatching { file.readLines() }.getOrElse {
                log += "não deu para ler ${file.path}: ${it.javaClass.simpleName}"
                return null
            }
            val start = lines.indexOfLast { "Starting language server process with pid" in it }
            val found = lines.getOrNull(start)?.let { Regex("""pid (\d+)""").find(it)?.groupValues?.get(1)?.toLongOrNull() }
            val listed = if (start < 0) emptyList() else lines.drop(start).mapNotNull { Regex("""listening on random port at (\d+) for (HTTPS|HTTP)""").find(it) }
                .sortedBy { if (it.groupValues[2] == "HTTPS") 0 else 1 }.mapNotNull { it.groupValues[1].toIntOrNull() }.distinct()
            logKey = key
            logRead = found to listed
        }
        val (pid, ports) = logRead
        if (pid == null) {
            log += "log sem a linha do pid: ${file.path}"
            return null
        }
        if (!alive(pid)) {
            log += "log aponta o pid $pid, que não está rodando"
            return null
        }
        log += "log: pid $pid, portas ${ports.joinToString().ifEmpty { "nenhuma" }}"
        return Candidate(pid, ports, true)
    }

    private fun scan(): List<Long> = runCatching {
        ProcessHandle.allProcesses().use { all ->
            all.filter { h -> File(h.info().command().orElse("")).name.lowercase().startsWith("language_server") }.map { it.pid() }.toList()
        }
    }.getOrDefault(emptyList())

    private fun alive(pid: Long) = ProcessHandle.of(pid).map { it.isAlive }.orElse(false)

    private fun exe(pid: Long): String = ProcessHandle.of(pid).flatMap { it.info().command() }.orElse("?")

    private fun commandLine(pid: Long, log: MutableList<String>): String? {
        if (onMac) return runCatching { run("ps", "-ww", "-o", "command=", "-p", pid.toString()).trim().ifEmpty { null } }.getOrNull()
            .also { if (it == null) log += "$pid: ps não devolveu a linha de comando" }
        WinProc.commandLine(pid)?.let { return it }
        log += "$pid: leitura direta da linha de comando falhou"
        if (!cimTried.add(pid)) return null
        val script = "(Get-CimInstance Win32_Process -Filter \"ProcessId=$pid\").CommandLine"
        return runCatching { run(Hook.powershell, "-NoProfile", "-NonInteractive", "-Command", script).trim().ifEmpty { null } }.getOrNull()
            .also { if (it == null) log += "$pid: PowerShell também não leu (Antigravity como administrador?)" }
    }

    private fun listening(pid: Long, log: MutableList<String>): List<Int> = runCatching {
        if (onMac) {
            Regex(""":(\d+) \(LISTEN\)""").findAll(run("lsof", "-nP", "-a", "-iTCP", "-sTCP:LISTEN", "-p", pid.toString()))
                .mapNotNull { it.groupValues[1].toIntOrNull() }.distinct().toList()
        } else {
            val netstat = File(System.getenv("SystemRoot") ?: "C:\\Windows", "System32\\netstat.exe").takeIf { it.exists() }?.path ?: "netstat.exe"
            val row = Regex("""^\s*TCP\s+\S+:(\d+)\s+(?:0\.0\.0\.0:0|\[::]:0)\s+\S+\s+(\d+)\s*$""")
            run(netstat, "-ano").lineSequence().mapNotNull { row.find(it) }.filter { it.groupValues[2].toLongOrNull() == pid }
                .mapNotNull { it.groupValues[1].toIntOrNull() }.distinct().toList()
        }
    }.getOrElse {
        log += "$pid: não deu para listar as portas (${it.javaClass.simpleName})"
        emptyList()
    }

    private fun run(vararg command: String): String {
        val p = ProcessBuilder(*command).redirectErrorStream(false).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor(10, TimeUnit.SECONDS)
        return out
    }

    private fun query(s: Server, log: MutableList<String>): String? {
        endpoint?.let { e -> post(e, s.token, log)?.let { return it } }
        for (port in s.ports) for (https in listOf(true, false)) for (host in HOSTS) {
            val e = Endpoint(host, port, https)
            val body = post(e, s.token, log) ?: continue
            endpoint = e
            return body
        }
        return null
    }

    private const val STATUS_BODY = "{\"metadata\":{\"ideName\":\"antigravity\",\"extensionName\":\"antigravity\",\"locale\":\"en\"}}"

    private fun post(e: Endpoint, token: String, log: MutableList<String>): String? {
        val scheme = if (e.https) "https" else "http"
        return runCatching {
            val c = URL("$scheme://${e.host}:${e.port}${BASE}GetUserStatus").openConnection(Proxy.NO_PROXY) as HttpURLConnection
            try {
                if (c is HttpsURLConnection) {
                    c.sslSocketFactory = localTls
                    c.hostnameVerifier = HostnameVerifier { host, _ -> host in setOf("127.0.0.1", "::1", "[::1]") }
                }
                c.requestMethod = "POST"
                c.connectTimeout = 3_000
                c.readTimeout = 10_000
                c.useCaches = false
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.setRequestProperty("Connect-Protocol-Version", "1")
                c.setRequestProperty("X-Codeium-Csrf-Token", token)
                c.outputStream.use { it.write(STATUS_BODY.toByteArray(Charsets.UTF_8)) }
                val code = c.responseCode
                if (code != 200) {
                    log += "$scheme ${e.host}:${e.port} -> HTTP $code"
                    null
                } else c.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            } finally {
                c.disconnect()
            }
        }.getOrElse {
            log += "$scheme ${e.host}:${e.port} -> ${it.javaClass.simpleName}"
            null
        }
    }

    private fun masked(text: String): String {
        val home = System.getProperty("user.home").orEmpty()
        if (home.length < 4) return text
        return text.replace(home, "%USERPROFILE%", ignoreCase = true).replace(home.replace('\\', '/'), "%USERPROFILE%", ignoreCase = true)
    }

    private fun report(now: Long, log: List<String>) {
        val head = listOf(
            "Banditboard ${System.getProperty("jpackage.app-version").orEmpty()} · ${System.getProperty("os.name")} ${System.getProperty("os.version")}",
            "${Instant.ofEpochMilli(now)} · estado ${state.value}",
        )
        val body = log.joinToString("\n")
        if (body == reported) return
        reported = body
        val text = (head + log).joinToString("\n")
        diagnosis.value = text
        runCatching { File(Store.dir, "antigravity-diagnostico.txt").writeText(masked(text) + "\n") }
    }
}

internal object WinProc {
    private interface Kernel32 : Library {
        fun OpenProcess(access: Int, inherit: Boolean, pid: Int): Pointer?
        fun CloseHandle(handle: Pointer): Boolean
    }

    private interface NtDll : Library {
        fun NtQueryInformationProcess(handle: Pointer, infoClass: Int, info: Pointer, length: Int, returned: IntByReference): Int
    }

    private const val QUERY_LIMITED = 0x1000
    private const val COMMAND_LINE = 60

    private val kernel32 by lazy { if (onMac) null else runCatching { Native.load("kernel32", Kernel32::class.java) }.getOrNull() }
    private val ntdll by lazy { if (onMac) null else runCatching { Native.load("ntdll", NtDll::class.java) }.getOrNull() }

    fun commandLine(pid: Long): String? = runCatching {
        val k = kernel32 ?: return null
        val nt = ntdll ?: return null
        val handle = k.OpenProcess(QUERY_LIMITED, false, pid.toInt()) ?: return null
        try {
            val size = IntByReference()
            var mem = Memory(8192)
            var status = nt.NtQueryInformationProcess(handle, COMMAND_LINE, mem, mem.size().toInt(), size)
            if (status != 0 && size.value > mem.size()) {
                mem = Memory(size.value.toLong())
                status = nt.NtQueryInformationProcess(handle, COMMAND_LINE, mem, mem.size().toInt(), size)
            }
            if (status != 0) return null
            val length = mem.getShort(0).toInt() and 0xFFFF
            val text = mem.getPointer(Native.POINTER_SIZE.toLong()) ?: return null
            String(text.getCharArray(0, length / 2)).ifEmpty { null }
        } finally {
            k.CloseHandle(handle)
        }
    }.getOrNull()
}
