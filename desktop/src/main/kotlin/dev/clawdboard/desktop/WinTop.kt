package dev.clawdboard.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import java.awt.Window

internal object WinTop {
    private interface User32 : Library {
        fun SetWindowPos(hwnd: Pointer, after: Pointer, x: Int, y: Int, cx: Int, cy: Int, flags: Int): Boolean
        fun GetTopWindow(hwnd: Pointer?): Pointer?
        fun GetWindow(hwnd: Pointer, cmd: Int): Pointer?
        fun IsWindowVisible(hwnd: Pointer): Boolean
        fun GetWindowRect(hwnd: Pointer, rect: IntArray): Boolean
        fun GetClassNameW(hwnd: Pointer, name: CharArray, size: Int): Int
        fun GetWindowThreadProcessId(hwnd: Pointer, pid: IntByReference): Int
    }

    private interface Dwm : Library {
        fun DwmGetWindowAttribute(hwnd: Pointer, attribute: Int, value: IntByReference, size: Int): Int
    }

    private val user32: User32? by lazy { if (onMac) null else runCatching { Native.load("user32", User32::class.java) }.getOrNull() }
    private val dwm: Dwm? by lazy { if (onMac) null else runCatching { Native.load("dwmapi", Dwm::class.java) }.getOrNull() }
    private val TOPMOST = Pointer(-1L)
    private const val FLAGS = 0x0001 or 0x0002 or 0x0010 or 0x0200
    private const val NEXT = 2
    private const val CLOAKED = 14
    private val FLYOUTS = setOf("#32768", "TopLevelWindowForOverflowXamlIsland", "NotifyIconOverflowWindow", "Xaml_WindowedPopupClass", "Windows.UI.Core.CoreWindow")
    private val BARS = setOf("Shell_TrayWnd", "Shell_SecondaryTrayWnd", "Progman", "WorkerW")
    private val self = ProcessHandle.current().pid()

    fun raise(window: Window) {
        val api = user32 ?: return
        val hwnd = runCatching { Native.getWindowPointer(window) }.getOrNull() ?: return
        if (runCatching { covered(api, hwnd) }.getOrDefault(false)) return
        runCatching { api.SetWindowPos(hwnd, TOPMOST, 0, 0, 0, 0, FLAGS) }
    }

    private fun covered(api: User32, own: Pointer): Boolean {
        val mine = IntArray(4)
        if (!api.GetWindowRect(own, mine)) return false
        var h = api.GetTopWindow(null)
        var steps = 0
        while (h != null && h != own && steps++ < 128) {
            if (api.IsWindowVisible(h) && !cloaked(h) && overlaps(api, h, mine) && flyout(api, h)) return true
            h = api.GetWindow(h, NEXT)
        }
        return false
    }

    private fun flyout(api: User32, h: Pointer): Boolean {
        val name = CharArray(256)
        val cls = String(name, 0, api.GetClassNameW(h, name, name.size).coerceAtLeast(0))
        if (cls in FLYOUTS) return true
        if (cls in BARS) return false
        val pid = IntByReference()
        api.GetWindowThreadProcessId(h, pid)
        if (pid.value.toLong() == self) return false
        return WinProc.image(pid.value.toLong())?.lowercase()?.endsWith("\\explorer.exe") == true
    }

    private fun overlaps(api: User32, h: Pointer, r: IntArray): Boolean {
        val o = IntArray(4)
        if (!api.GetWindowRect(h, o)) return false
        return o[0] < r[2] && o[2] > r[0] && o[1] < r[3] && o[3] > r[1]
    }

    private fun cloaked(h: Pointer): Boolean {
        val v = IntByReference()
        return dwm?.DwmGetWindowAttribute(h, CLOAKED, v, 4) == 0 && v.value != 0
    }
}
