package dev.clawdboard.desktop

object Autostart {
    private const val KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private val exe: String? = System.getProperty("jpackage.app-path")
    val available get() = exe != null

    fun enabled() = available && run("reg", "query", KEY, "/v", "Banditboard") == 0

    fun set(on: Boolean) {
        if (!available) return
        if (on) run("reg", "add", KEY, "/v", "Banditboard", "/t", "REG_SZ", "/d", "\"$exe\"", "/f")
        else run("reg", "delete", KEY, "/v", "Banditboard", "/f")
    }

    private fun run(vararg args: String) = runCatching {
        ProcessBuilder(*args).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor()
    }.getOrDefault(-1)
}
