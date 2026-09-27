package dev.clawdboard.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import kotlin.concurrent.thread

object WinMusic {
    val playing = MutableStateFlow(false)

    fun start() {
        val script = File(Store.dir, "music.ps1")
        runCatching { script.writeText(WinMusic::class.java.classLoader.getResourceAsStream("music.ps1")!!.use { it.readBytes().toString(Charsets.UTF_8) }) }
        thread(isDaemon = true, name = "banditboard-music") {
            while (true) {
                runCatching {
                    val p = ProcessBuilder(
                        "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden",
                        "-File", script.absolutePath, ProcessHandle.current().pid().toString(),
                    ).redirectErrorStream(true).start()
                    p.inputStream.bufferedReader().forEachLine { line ->
                        when (line.trim()) {
                            "1" -> playing.value = true
                            "0" -> playing.value = false
                        }
                    }
                    p.waitFor()
                }
                playing.value = false
                Thread.sleep(30_000)
            }
        }
    }
}
