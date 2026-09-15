package com.lucashyuan.darculaveil.nativewindow

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT

class VeilWindowEmbedder(private val target: HWND, private val host: HWND) {

    private val user32 = VeilUser32.INSTANCE
    private val originalStyle = user32.GetWindowLong(target, VeilUser32.GWL_STYLE)
    private val originalExStyle = user32.GetWindowLong(target, VeilUser32.GWL_EXSTYLE)
    private val originalParent = user32.GetParent(target)

    private var detached = false

    fun attach() {
        user32.SetWindowLong(target, VeilUser32.GWL_STYLE, buildChildStyle())
        user32.SetWindowLong(target, VeilUser32.GWL_EXSTYLE, buildChildExStyle())
        user32.SetParent(target, host)
        user32.SetWindowPos(target, null, 0, 0, 0, 0, VeilUser32.SWP_FRAMECHANGED or VeilUser32.SWP_NOZORDER or VeilUser32.SWP_NOACTIVATE)
        user32.ShowWindow(target, VeilUser32.SW_SHOW)
        syncGeometry()
    }

    fun detach() {
        if (detached) {
            return
        }

        detached = true

        if (!isAlive()) {
            return
        }

        user32.SetParent(target, originalParent)
        user32.SetWindowLong(target, VeilUser32.GWL_STYLE, originalStyle)
        user32.SetWindowLong(target, VeilUser32.GWL_EXSTYLE, originalExStyle)
        user32.SetWindowPos(target, null, 0, 0, 0, 0, VeilUser32.SWP_FRAMECHANGED or VeilUser32.SWP_NOZORDER or VeilUser32.SWP_NOACTIVATE)
        user32.ShowWindow(target, VeilUser32.SW_SHOW)
    }

    fun syncGeometry() {
        if (detached || !isAlive()) {
            return
        }

        val clientRect = RECT()

        if (!user32.GetClientRect(host, clientRect)) {
            return
        }

        val width = clientRect.right - clientRect.left
        val height = clientRect.bottom - clientRect.top

        user32.SetWindowPos(target, null, 0, 0, width, height, VeilUser32.SWP_NOZORDER or VeilUser32.SWP_NOACTIVATE)
    }

    fun applyOpacity(enabled: Boolean, opacityPercent: Int) {
        if (detached || !isAlive()) {
            return
        }

        val exStyle = user32.GetWindowLong(target, VeilUser32.GWL_EXSTYLE)

        if (!enabled) {
            user32.SetWindowLong(target, VeilUser32.GWL_EXSTYLE, exStyle and VeilUser32.WS_EX_LAYERED.inv())
            user32.SetWindowPos(target, null, 0, 0, 0, 0, VeilUser32.SWP_FRAMECHANGED or VeilUser32.SWP_NOZORDER or VeilUser32.SWP_NOACTIVATE)
            return
        }

        user32.SetWindowLong(target, VeilUser32.GWL_EXSTYLE, exStyle or VeilUser32.WS_EX_LAYERED)
        user32.SetLayeredWindowAttributes(target, 0, toAlpha(opacityPercent), VeilUser32.LWA_ALPHA)
    }

    fun transferFocus() {
        if (detached || !isAlive()) {
            return
        }

        val currentThreadId = Kernel32.INSTANCE.GetCurrentThreadId()
        val targetThreadId = user32.GetWindowThreadProcessId(target, null)

        if (currentThreadId == targetThreadId) {
            user32.SetFocus(target)
            return
        }

        user32.AttachThreadInput(currentThreadId, targetThreadId, true)
        user32.SetFocus(target)
        user32.AttachThreadInput(currentThreadId, targetThreadId, false)
    }

    fun isAlive(): Boolean = user32.IsWindow(target)

    private fun buildChildStyle(): Int {
        val removed = VeilUser32.WS_POPUP or VeilUser32.WS_CAPTION or VeilUser32.WS_THICKFRAME or VeilUser32.WS_MINIMIZEBOX or VeilUser32.WS_MAXIMIZEBOX or VeilUser32.WS_SYSMENU

        return originalStyle and removed.inv() or VeilUser32.WS_CHILD
    }

    private fun buildChildExStyle(): Int {
        return originalExStyle and VeilUser32.WS_EX_APPWINDOW.inv() or VeilUser32.WS_EX_TOOLWINDOW
    }

    private fun toAlpha(opacityPercent: Int): Byte = (opacityPercent.coerceIn(10, 100) * 255 / 100).toByte()
}
