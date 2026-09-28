package dev.clawdboard.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import dev.clawdboard.core.txt
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

enum class Cause { POLICY, SETTINGS, DENIED, TIMEOUT, OTHER }

class Failure(val cause: Cause, val detail: String? = null)

object Hook {
    private fun resource(name: String) = Hook::class.java.classLoader.getResourceAsStream(name)!!.use { it.readBytes().toString(Charsets.UTF_8) }

    fun installer(url: String, key: String): String =
        resource("pc/install.ps1").replace("__USAGE__", resource("pc/usage.ps1").trimEnd())
            .replace("__T_CONNECTED__", txt.installConnected).replace("__T_BACKUP__", txt.installBackup(""))
            .replace("__T_EVERY__", txt.installEvery(url)).replace("__URL__", url).replace("__KEY__", key).replace("__ID__", "pc")

    val claudeDir = File(System.getProperty("user.home"), ".claude")
    val settings = File(claudeDir, "settings.json")
    val log = File(Store.dir, "conectar.log")
    val failure = MutableStateFlow<Failure?>(null)

    private val powershell = File(System.getenv("SystemRoot") ?: "C:\\Windows", "System32\\WindowsPowerShell\\v1.0\\powershell.exe")
        .takeIf { it.exists() }?.path ?: "powershell.exe"

    private interface Kernel32 : Library {
        fun GetOEMCP(): Int
    }

    private val console: Charset by lazy {
        runCatching { Native.load("kernel32", Kernel32::class.java).GetOEMCP() }
            .mapCatching { if (it == 65001) Charsets.UTF_8 else Charset.forName("cp$it") }
            .getOrDefault(Charset.defaultCharset())
    }

    fun install(port: Int): Boolean {
        failure.value = null
        val result = attempt(port)
        if (result == null) runCatching { Store.put("hookPort", port) }
        failure.value = result
        return result == null
    }

    private fun attempt(port: Int): Failure? {
        val script = File(Store.dir, "install.ps1")
        log.delete()
        return try {
            val body = resource("connect.ps1").replace("__LOG__", log.path.replace("'", "''"))
                .replace("__INSTALL__", installer("http://127.0.0.1:$port", Store.key))
            script.writeText("\uFEFF" + body, Charsets.UTF_8)
            val p = ProcessBuilder(powershell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.absolutePath)
                .redirectErrorStream(true).start()
            p.outputStream.close()
            var out = ByteArray(0)
            val reader = thread(isDaemon = true) { out = runCatching { p.inputStream.readBytes() }.getOrDefault(ByteArray(0)) }
            if (!p.waitFor(60, TimeUnit.SECONDS)) {
                p.destroyForcibly()
                log.writeText("PowerShell sem resposta depois de 60 s.\n")
                return Failure(Cause.TIMEOUT)
            }
            reader.join(5_000)
            when {
                p.exitValue() == 0 -> null
                log.exists() -> fromLog()
                else -> {
                    val text = String(out, console).replace(Store.key, "…")
                    log.writeText("exit ${p.exitValue()}\n\n$text")
                    Failure(if ("UnauthorizedAccess" in text) Cause.POLICY else Cause.OTHER)
                }
            }
        } catch (e: Exception) {
            runCatching { log.writeText(e.stackTraceToString()) }
            Failure(Cause.OTHER, e.message)
        } finally {
            script.delete()
        }
    }

    private fun fromLog(): Failure {
        val text = log.readText().replace(Store.key, "…").also { log.writeText(it) }
        val head = text.substringBefore("\n\n").lines()
        val kinds = head.first()
        val detail = head.drop(1).joinToString(" ").trim().ifEmpty { null }
        val cause = when {
            "ConvertFromJsonCommand" in kinds -> Cause.SETTINGS
            "UnauthorizedAccess" in kinds -> Cause.DENIED
            else -> Cause.OTHER
        }
        return Failure(cause, detail)
    }

    fun show(file: File) {
        runCatching { ProcessBuilder("notepad.exe", file.absolutePath).start() }
    }

    private val usageScript = File(claudeDir, "clawdboard-usage.ps1")

    fun connected(): Boolean = Store.int("hookPort") != 0 && usageScript.exists()

    fun refresh(): Boolean = runCatching {
        val before = pushedAt.value
        val p = ProcessBuilder(powershell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", usageScript.absolutePath, "-Work")
            .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
        p.waitFor(90, TimeUnit.SECONDS)
        pushedAt.value > before
    }.getOrDefault(false)
}
