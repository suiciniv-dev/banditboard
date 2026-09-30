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

    private fun sh(s: String) = if (onMac) s.replace("'", "'\\''") else s

    fun installer(url: String, key: String, activity: String = ""): String {
        val kind = if (onMac) "sh" else "ps1"
        return resource("pc/install.$kind").replace("__USAGE__", resource("pc/usage.$kind").trimEnd())
            .replace("__T_CONNECTED__", sh(txt.installConnected)).replace("__T_BACKUP__", sh(txt.installBackup("")))
            .replace("__T_EVERY__", sh(txt.installEvery(url))).replace("__URL__", url).replace("__KEY__", key).replace("__ENC__", "").replace("__ACTIVITY__", activity).replace("__ID__", "pc")
    }

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

    val watching: Boolean get() = Store.get("activity") == "on"

    fun install(port: Int, activity: String = if (watching) "on" else ""): Boolean {
        failure.value = null
        val result = attempt(port, activity)
        if (result == null) runCatching { Store.put("hookPort", port) }
        failure.value = result
        return result == null
    }

    private fun attempt(port: Int, activity: String): Failure? {
        val script = File(Store.dir, if (onMac) "install.sh" else "install.ps1")
        log.delete()
        return try {
            val install = installer("http://127.0.0.1:$port", Store.key, activity)
            val command = if (onMac) {
                script.writeText(install)
                listOf("/bin/sh", script.absolutePath)
            } else {
                val body = resource("connect.ps1").replace("__LOG__", log.path.replace("'", "''")).replace("__INSTALL__", install)
                script.writeText("\uFEFF" + body, Charsets.UTF_8)
                listOf(powershell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.absolutePath)
            }
            val p = ProcessBuilder(command).redirectErrorStream(true).start()
            p.outputStream.close()
            var out = ByteArray(0)
            val reader = thread(isDaemon = true) { out = runCatching { p.inputStream.readBytes() }.getOrDefault(ByteArray(0)) }
            if (!p.waitFor(60, TimeUnit.SECONDS)) {
                p.destroyForcibly()
                log.writeText(if (onMac) "O script de conex\u00E3o ficou sem resposta depois de 60 s.\n" else "PowerShell sem resposta depois de 60 s.\n")
                return Failure(Cause.TIMEOUT)
            }
            reader.join(5_000)
            when {
                p.exitValue() == 0 -> null
                onMac -> {
                    log.writeText("exit ${p.exitValue()}\n\n${String(out, Charsets.UTF_8).replace(Store.key, "\u2026")}")
                    Failure(
                        when (p.exitValue()) {
                            3 -> Cause.SETTINGS
                            4 -> Cause.DENIED
                            else -> Cause.OTHER
                        },
                    )
                }
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
        runCatching { ProcessBuilder(if (onMac) listOf("open", "-t", file.absolutePath) else listOf("notepad.exe", file.absolutePath)).start() }
    }

    private val usageScript = File(claudeDir, if (onMac) "clawdboard-usage.sh" else "clawdboard-usage.ps1")

    fun connected(): Boolean = Store.int("hookPort") != 0 && usageScript.exists()

    fun stale(): Boolean = !onMac && connected() && runCatching {
        !usageScript.readText().contains("WindowsPowerShell") || !settings.readText().contains("WindowsPowerShell")
    }.getOrDefault(false)

    fun refresh(): Boolean = runCatching {
        val before = pushedAt.value
        val command = if (onMac) listOf("/bin/sh", usageScript.absolutePath, "--work")
        else listOf(powershell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", usageScript.absolutePath, "-Work")
        val p = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
        p.waitFor(90, TimeUnit.SECONDS)
        pushedAt.value > before
    }.getOrDefault(false)
}
