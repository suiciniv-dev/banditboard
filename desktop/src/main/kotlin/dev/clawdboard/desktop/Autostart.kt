package dev.clawdboard.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference
import java.io.File

object Autostart {
    private interface Advapi : Library {
        fun RegGetValueW(key: Pointer, subKey: WString, value: WString, flags: Int, type: Pointer?, data: CharArray?, size: IntByReference): Int
        fun RegSetKeyValueW(key: Pointer, subKey: WString, value: WString, type: Int, data: CharArray, size: Int): Int
        fun RegDeleteKeyValueW(key: Pointer, subKey: WString, value: WString): Int
    }

    private val api by lazy { if (onMac) null else runCatching { Native.load("advapi32", Advapi::class.java) }.getOrNull() }
    private val user = Pointer(0x80000001.toInt().toLong())
    private val run = WString("Software\\Microsoft\\Windows\\CurrentVersion\\Run")
    private val name = WString("Banditboard")
    private val agent = File(System.getProperty("user.home"), "Library/LaunchAgents/dev.clawdboard.banditboard.plist")
    private val exe: String? = System.getProperty("jpackage.app-path")
    val available get() = exe != null && (onMac || api != null)

    private fun registered(): String? {
        if (onMac) {
            if (!agent.exists()) return null
            val text = runCatching { agent.readText() }.getOrNull() ?: return null
            return Regex("<key>ProgramArguments</key>\\s*<array>\\s*<string>([^<]*)</string>").find(text)?.groupValues?.get(1)
                ?.replace("&lt;", "<")?.replace("&gt;", ">")?.replace("&amp;", "&") ?: ""
        }
        val a = api ?: return null
        val size = IntByReference(0)
        if (a.RegGetValueW(user, run, name, 2, null, null, size) != 0) return null
        val data = CharArray(size.value / 2)
        if (a.RegGetValueW(user, run, name, 2, null, data, size) != 0) return null
        return String(data).substringBefore('\u0000')
    }

    fun enabled() = available && registered() != null

    fun set(on: Boolean) {
        val path = exe ?: return
        if (onMac) {
            runCatching {
                if (on) agent.apply { parentFile.mkdirs() }.writeText(plist(path)) else agent.delete()
            }
            return
        }
        val a = api ?: return
        if (on) {
            val data = "\"$path\"\u0000".toCharArray()
            a.RegSetKeyValueW(user, run, name, 1, data, data.size * 2)
        } else a.RegDeleteKeyValueW(user, run, name)
    }

    fun repair() {
        val path = exe ?: return
        val now = registered() ?: return
        if (!now.trim('"').equals(path, ignoreCase = true)) set(true)
    }

    private fun plist(path: String): String {
        val safe = path.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>dev.clawdboard.banditboard</string>
                <key>ProgramArguments</key>
                <array>
                    <string>$safe</string>
                </array>
                <key>RunAtLoad</key>
                <true/>
                <key>ProcessType</key>
                <string>Interactive</string>
            </dict>
            </plist>
        """.trimIndent() + "\n"
    }
}
