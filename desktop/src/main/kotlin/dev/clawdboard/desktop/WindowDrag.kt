package dev.clawdboard.desktop

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import kotlin.math.abs

internal fun Modifier.dragWindow(
    window: Window,
    key: Any?,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onMoved: () -> Unit,
): Modifier = pointerInput(window, key) {
    var lastTap = 0L
    awaitEachGesture {
        val down = awaitFirstDown()
        val awt = currentEvent.nativeEvent as? MouseEvent
        if (currentEvent.buttons.isSecondaryPressed || (awt != null && SwingUtilities.isRightMouseButton(awt))) {
            down.consume()
            lastTap = 0L
            return@awaitEachGesture
        }
        val start = MouseInfo.getPointerInfo()?.location ?: return@awaitEachGesture
        val origin = window.location
        var moved = false
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val now = MouseInfo.getPointerInfo()?.location ?: continue
            val dx = now.x - start.x
            val dy = now.y - start.y
            if (!moved && abs(dx) + abs(dy) > 3) moved = true
            if (moved) {
                window.setLocation(origin.x + dx, origin.y + dy)
                change.consume()
            }
        }
        if (moved) {
            lastTap = 0L
            onMoved()
        } else if (down.uptimeMillis - lastTap < 450) {
            lastTap = 0L
            onDoubleClick()
        } else {
            lastTap = down.uptimeMillis
            onClick()
        }
    }
}

object WidgetSpot {
    private fun key(layout: Layout) = "pos.${layout.name}"

    fun save(layout: Layout, window: Window) = Store.put(key(layout), "${window.x},${window.y}")

    fun place(layout: Layout, window: Window) {
        val saved = Store.get(key(layout))?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
        if (saved != null && saved.size == 2) {
            val spot = Rectangle(saved[0], saved[1], window.width, window.height)
            val visible = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.any { d ->
                val seen = d.defaultConfiguration.bounds.intersection(spot)
                !seen.isEmpty && seen.width >= 40 && seen.height >= 40
            }
            if (visible) {
                window.setLocation(saved[0], saved[1])
                return
            }
        }
        val gc = window.graphicsConfiguration
        val b = gc.bounds
        val ins = Toolkit.getDefaultToolkit().getScreenInsets(gc)
        window.setLocation(b.x + b.width - ins.right - window.width - 12, b.y + b.height - ins.bottom - window.height - 12)
    }
}
