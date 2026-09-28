package dev.clawdboard.desktop

import dev.clawdboard.core.txt
import java.io.File
import java.util.concurrent.TimeUnit

object Hook {
    private fun resource(name: String) = Hook::class.java.classLoader.getResourceAsStream(name)!!.use { it.readBytes().toString(Charsets.UTF_8) }

    fun installer(url: String, key: String): String =
        resource("pc/install.ps1").replace("__USAGE__", resource("pc/usage.ps1").trimEnd())
            .replace("__T_CONNECTED__", txt.installConnected).replace("__T_BACKUP__", txt.installBackup(""))
            .replace("__T_EVERY__", txt.installEvery(url)).replace("__URL__", url).replace("__KEY__", key).replace("__ID__", "pc")

    fun install(port: Int): Boolean = runCatching {
        val script = File(Store.dir, "install.ps1")
        script.writeText("﻿" + installer("http://127.0.0.1:$port", Store.key), Charsets.UTF_8)
        val p = ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.absolutePath)
            .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
        val done = p.waitFor(60, TimeUnit.SECONDS) && p.exitValue() == 0
        script.delete()
        if (done) Store.put("hookPort", port)
        done
    }.getOrDefault(false)

    private val usageScript = File(System.getProperty("user.home"), ".claude/clawdboard-usage.ps1")

    fun connected(): Boolean = Store.int("hookPort") != 0 && usageScript.exists()

    fun refresh(): Boolean = runCatching {
        val before = pushedAt.value
        val p = ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", usageScript.absolutePath, "-Work")
            .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
        p.waitFor(90, TimeUnit.SECONDS)
        pushedAt.value > before
    }.getOrDefault(false)
}
