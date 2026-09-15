package com.lucashyuan.darculaveil.nativewindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import com.lucashyuan.darculaveil.VeilSettings
import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef.HWND
import java.awt.BorderLayout
import java.awt.Canvas
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.Timer

class VeilNativePanel : JPanel(BorderLayout()), Disposable {

    private val canvas = HostCanvas()
    private val placeholder = JBLabel("No window embedded. Use Pick Window in the tool window title bar.", SwingConstants.CENTER)
    private val aliveTimer = Timer(ALIVE_POLL_INTERVAL_MS) { checkTargetAlive() }

    private var embedder: VeilWindowEmbedder? = null
    private var target: VeilWindowInfo? = null

    init {
        canvas.background = UIUtil.getPanelBackground()
        background = UIUtil.getPanelBackground()
        add(placeholder, BorderLayout.CENTER)
        installCanvasListeners()
    }

    fun embeddedWindowTitle(): String = target?.title ?: ""

    fun hasEmbeddedWindow(): Boolean = target != null

    fun embed(info: VeilWindowInfo) {
        releaseEmbedder()
        target = info
        showCanvas()

        if (embedder == null) {
            attachToCanvas()
        }
    }

    fun detach() {
        releaseEmbedder()
        target = null
        showPlaceholder()
    }

    fun syncGeometry() {
        embedder?.syncGeometry()
    }

    fun applyOpacity() {
        val settings = VeilSettings.state()
        embedder?.applyOpacity(settings.nativeLayeredEnabled, settings.nativeOpacityPercent)
    }

    override fun dispose() {
        detach()
    }

    private fun installCanvasListeners() {
        canvas.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) {
                syncGeometry()
            }
        })

        canvas.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                embedder?.transferFocus()
            }
        })
    }

    private fun attachToCanvas() {
        val info = target ?: return
        val host = resolveCanvasHandle()

        if (host == null) {
            LOG.warn("Canvas peer is not available, cannot embed ${info.title}")
            return
        }

        val settings = VeilSettings.state()
        val created = VeilWindowEmbedder(info.handle, host)
        created.attach()
        created.applyOpacity(settings.nativeLayeredEnabled, settings.nativeOpacityPercent)
        embedder = created
        aliveTimer.start()
    }

    private fun releaseEmbedder() {
        aliveTimer.stop()
        embedder?.detach()
        embedder = null
    }

    private fun showCanvas() {
        if (canvas.parent != null) {
            return
        }

        remove(placeholder)
        add(canvas, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private fun showPlaceholder() {
        if (placeholder.parent != null) {
            return
        }

        remove(canvas)
        add(placeholder, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private fun resolveCanvasHandle(): HWND? {
        if (!canvas.isDisplayable) {
            return null
        }

        val pointer = Native.getComponentPointer(canvas) ?: return null

        return HWND(pointer)
    }

    private fun checkTargetAlive() {
        val current = embedder ?: return

        if (current.isAlive()) {
            return
        }

        LOG.info("Embedded window disappeared, detaching")
        detach()
    }

    private inner class HostCanvas : Canvas() {

        override fun addNotify() {
            super.addNotify()

            if (target != null && embedder == null) {
                attachToCanvas()
            }
        }

        override fun removeNotify() {
            releaseEmbedder()
            super.removeNotify()
        }
    }

    companion object {
        private val LOG = Logger.getInstance(VeilNativePanel::class.java)
        private const val ALIVE_POLL_INTERVAL_MS = 1000
    }
}
