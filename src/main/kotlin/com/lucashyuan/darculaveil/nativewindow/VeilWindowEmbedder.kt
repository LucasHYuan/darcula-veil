package com.lucashyuan.darculaveil.nativewindow

import com.intellij.openapi.diagnostic.Logger
import com.lucashyuan.darculaveil.VeilSettings
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import java.awt.Color

private val LOG = Logger.getInstance(VeilWindowEmbedder::class.java)

class VeilWindowEmbedder(private val target: HWND, private val host: HWND) {

    private val user32 = VeilUser32.INSTANCE
    private val originalStyle = user32.GetWindowLong(target, VeilUser32.GWL_STYLE)
    private val originalExStyle = user32.GetWindowLong(target, VeilUser32.GWL_EXSTYLE)
    private val originalParent = user32.GetParent(target)
    private val originalBounds = readWindowBounds()

    private var detached = false
    private var overlay: VeilOverlayWindow? = null
    private var overlayActive = false

    fun attach() {
        user32.SetWindowLong(target, VeilUser32.GWL_STYLE, buildChildStyle())
        user32.SetWindowLong(target, VeilUser32.GWL_EXSTYLE, buildChildExStyle())
        user32.SetParent(target, host)
        user32.SetWindowPos(target, null, 0, 0, 0, 0, VeilUser32.SWP_FRAMECHANGED or VeilUser32.SWP_NOZORDER or VeilUser32.SWP_NOACTIVATE or VeilUser32.SWP_ASYNCWINDOWPOS)
        syncGeometry()
    }

    fun setVisible(visible: Boolean) {
        if (detached || !isAlive()) {
            return
        }

        user32.ShowWindow(target, if (visible) VeilUser32.SW_SHOW else VeilUser32.SW_HIDE)
        overlay?.setVisible(visible && overlayActive)
    }

    fun refreshOverlay(tint: Color, settings: VeilSettings.State) {
        if (detached || !isAlive()) {
            return
        }

        overlayActive = settings.overlayEnabled

        if (!overlayActive) {
            overlay?.setVisible(false)
            return
        }

        val clientRect = RECT()

        if (!user32.GetClientRect(host, clientRect)) {
            return
        }

        val width = clientRect.right - clientRect.left
        val height = clientRect.bottom - clientRect.top

        if (width <= 0 || height <= 0) {
            return
        }

        val current = overlay ?: VeilOverlayWindow(host).also { overlay = it }
        current.update(width, height, tint, settings)
    }

    fun detach(showAfterDetach: Boolean) {
        if (detached) {
            return
        }

        overlay?.destroy()
        overlay = null
        detached = true

        if (!isAlive()) {
            return
        }

        user32.SetParent(target, originalParent)
        user32.SetWindowLong(target, VeilUser32.GWL_STYLE, restoredStyle(showAfterDetach))
        user32.SetWindowLong(target, VeilUser32.GWL_EXSTYLE, originalExStyle)
        restoreBounds()

        if (showAfterDetach) {
            user32.ShowWindow(target, VeilUser32.SW_SHOW)
        }
    }

    fun syncGeometry(): Boolean {
        if (detached || !isAlive()) {
            return false
        }

        val clientRect = RECT()

        if (!user32.GetClientRect(host, clientRect)) {
            return false
        }

        val width = clientRect.right - clientRect.left
        val height = clientRect.bottom - clientRect.top

        if (width <= 0 || height <= 0) {
            return false
        }

        val currentBounds = RECT()

        if (user32.GetWindowRect(target, currentBounds) && currentBounds.right - currentBounds.left == width && currentBounds.bottom - currentBounds.top == height) {
            return true
        }

        return user32.SetWindowPos(target, null, 0, 0, width, height, VeilUser32.SWP_NOZORDER or VeilUser32.SWP_NOACTIVATE or VeilUser32.SWP_ASYNCWINDOWPOS)
    }

    fun applyOpacity(enabled: Boolean, opacityPercent: Int) {
        if (detached || !isAlive()) {
            return
        }

        val exStyle = user32.GetWindowLong(target, VeilUser32.GWL_EXSTYLE)

        if (!enabled) {
            user32.SetWindowLong(target, VeilUser32.GWL_EXSTYLE, exStyle and VeilUser32.WS_EX_LAYERED.inv())
            user32.SetWindowPos(target, null, 0, 0, 0, 0, VeilUser32.SWP_FRAMECHANGED or VeilUser32.SWP_NOZORDER or VeilUser32.SWP_NOACTIVATE or VeilUser32.SWP_ASYNCWINDOWPOS)
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

        if (!user32.AttachThreadInput(currentThreadId, targetThreadId, true)) {
            LOG.info("AttachThreadInput refused, skipping focus transfer")
            return
        }

        try {
            user32.SetFocus(target)
        } finally {
            user32.AttachThreadInput(currentThreadId, targetThreadId, false)
        }
    }

    fun isAlive(): Boolean = user32.IsWindow(target)

    private fun readWindowBounds(): RECT? {
        val bounds = RECT()

        if (!user32.GetWindowRect(target, bounds)) {
            return null
        }

        return bounds
    }

    private fun restoreBounds() {
        val bounds = originalBounds

        if (bounds == null) {
            user32.SetWindowPos(target, null, 0, 0, 0, 0, VeilUser32.SWP_FRAMECHANGED or VeilUser32.SWP_NOZORDER or VeilUser32.SWP_NOACTIVATE or VeilUser32.SWP_ASYNCWINDOWPOS)
            return
        }

        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top

        user32.SetWindowPos(target, null, bounds.left, bounds.top, width, height, VeilUser32.SWP_FRAMECHANGED or VeilUser32.SWP_NOZORDER or VeilUser32.SWP_NOACTIVATE or VeilUser32.SWP_ASYNCWINDOWPOS)
    }

    private fun restoredStyle(showAfterDetach: Boolean): Int {
        if (showAfterDetach) {
            return originalStyle
        }

        return originalStyle and VeilUser32.WS_VISIBLE.inv()
    }

    private fun buildChildStyle(): Int {
        val removed = VeilUser32.WS_POPUP or VeilUser32.WS_CAPTION or VeilUser32.WS_THICKFRAME or VeilUser32.WS_MINIMIZEBOX or VeilUser32.WS_MAXIMIZEBOX or VeilUser32.WS_SYSMENU

        return originalStyle and removed.inv() or VeilUser32.WS_CHILD
    }

    private fun buildChildExStyle(): Int {
        return originalExStyle and VeilUser32.WS_EX_APPWINDOW.inv() or VeilUser32.WS_EX_TOOLWINDOW
    }

    private fun toAlpha(opacityPercent: Int): Byte = (opacityPercent.coerceIn(10, 100) * 255 / 100).toByte()
}
