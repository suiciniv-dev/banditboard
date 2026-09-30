package dev.clawdboard.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.awt.Window

internal object WinTop {
    private interface User32 : Library {
        fun SetWindowPos(hwnd: Pointer, after: Pointer, x: Int, y: Int, cx: Int, cy: Int, flags: Int): Boolean
    }

    private val user32: User32? by lazy { if (onMac) null else runCatching { Native.load("user32", User32::class.java) }.getOrNull() }
    private val TOPMOST = Pointer(-1L)
    private const val FLAGS = 0x0001 or 0x0002 or 0x0010 or 0x0200

    fun raise(window: Window) {
        val api = user32 ?: return
        val hwnd = runCatching { Native.getWindowPointer(window) }.getOrNull() ?: return
        runCatching { api.SetWindowPos(hwnd, TOPMOST, 0, 0, 0, 0, FLAGS) }
    }
}
