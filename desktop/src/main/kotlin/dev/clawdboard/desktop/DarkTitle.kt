package dev.clawdboard.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference

object DarkTitle {
    private interface Dwm : Library {
        fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, value: IntByReference, size: Int): Int
    }

    private val dwm by lazy { runCatching { Native.load("dwmapi", Dwm::class.java) }.getOrNull() }

    private fun bgr(argb: Int) = ((argb and 0xFF) shl 16) or (argb and 0xFF00) or ((argb shr 16) and 0xFF)

    fun apply(window: java.awt.Window, background: Int, text: Int) {
        if (onMac) {
            (window as? javax.swing.RootPaneContainer)?.rootPane?.apply {
                putClientProperty("apple.awt.fullWindowContent", true)
                putClientProperty("apple.awt.transparentTitleBar", true)
                putClientProperty("apple.awt.windowTitleVisible", false)
            }
            return
        }
        val api = dwm ?: return
        runCatching {
            val hwnd = Native.getWindowPointer(window)
            api.DwmSetWindowAttribute(hwnd, 20, IntByReference(1), 4)
            api.DwmSetWindowAttribute(hwnd, 35, IntByReference(bgr(background)), 4)
            api.DwmSetWindowAttribute(hwnd, 36, IntByReference(bgr(text)), 4)
        }
    }
}
