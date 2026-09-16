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
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.Timer

class VeilNativePanel : JPanel(BorderLayout()), Disposable {

    private val canvas = HostCanvas()
    private val placeholder = JBLabel("No window embedded. Use Pick Window in the tool window title bar.", SwingConstants.CENTER)
    private val watchdog = Timer(WATCHDOG_INTERVAL_MS) { tick() }

    private var embedder: VeilWindowEmbedder? = null
    private var target: VeilWindowInfo? = null
    private var lastTarget: VeilWindowInfo? = null
    private var concealed = false
    private var awaitingGeometry = false

    init {
        canvas.background = UIUtil.getPanelBackground()
        canvas.minimumSize = SHRINKABLE_SIZE
        canvas.preferredSize = SHRINKABLE_SIZE
        background = UIUtil.getPanelBackground()
        minimumSize = SHRINKABLE_SIZE
        add(placeholder, BorderLayout.CENTER)
        installCanvasListeners()
    }

    fun embeddedWindowTitle(): String = target?.title ?: ""

    fun hasEmbeddedWindow(): Boolean = target != null

    fun canReembed(): Boolean = target == null && lastTarget != null

    fun lastTargetTitle(): String = lastTarget?.title ?: ""

    fun embed(info: VeilWindowInfo) {
        releaseEmbedder()
        concealed = false
        target = info
        lastTarget = info
        showCanvas()
        attachToCanvas()
        watchdog.start()
    }

    fun reembedLast() {
        val info = lastTarget ?: return

        embed(info)
    }

    fun detach() {
        watchdog.stop()
        concealed = false
        releaseEmbedder()
        target = null
        showPlaceholder()
    }

    fun conceal() {
        concealed = true
        awaitingGeometry = false
        embedder?.setVisible(false)
    }

    fun reveal() {
        concealed = false
        awaitingGeometry = true
        revealWhenReady()
    }

    fun syncGeometry() {
        revealWhenReady()
    }

    fun applyOpacity() {
        val settings = VeilSettings.state()
        embedder?.applyOpacity(settings.nativeLayeredEnabled, settings.nativeOpacityPercent)
    }

    override fun dispose() {
        detach()
    }

    override fun getMinimumSize(): Dimension = SHRINKABLE_SIZE

    private fun installCanvasListeners() {
        canvas.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) {
                revealWhenReady()
            }

            override fun componentShown(event: ComponentEvent) {
                reveal()
                scheduleAttach()
            }
        })

        canvas.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                embedder?.transferFocus()
            }
        })
    }

    private fun tick() {
        val current = embedder

        if (current == null) {
            scheduleAttach()
            return
        }

        if (current.isAlive()) {
            revealWhenReady()
            return
        }

        LOG.info("Embedded window disappeared, detaching")
        detach()
    }

    private fun scheduleAttach() {
        if (target == null || embedder != null) {
            return
        }

        SwingUtilities.invokeLater { attachToCanvas() }
    }

    private fun attachToCanvas() {
        val info = target ?: return

        if (embedder != null) {
            return
        }

        val host = resolveCanvasHandle()

        if (host == null) {
            LOG.info("Canvas peer not ready, deferring embed of ${info.title}")
            return
        }

        val settings = VeilSettings.state()
        val created = VeilWindowEmbedder(info.handle, host)
        created.attach()
        created.applyOpacity(settings.nativeLayeredEnabled, settings.nativeOpacityPercent)
        embedder = created
        awaitingGeometry = true
        LOG.info("Embedded window ${info.title}, concealed=$concealed")
        revealWhenReady()
    }

    private fun revealWhenReady() {
        val current = embedder ?: return

        if (!current.syncGeometry()) {
            return
        }

        if (concealed || !awaitingGeometry) {
            return
        }

        current.setVisible(true)
        awaitingGeometry = false
    }

    private fun releaseEmbedder() {
        embedder?.detach(!concealed)
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
        if (!canvas.isDisplayable || canvas.width <= 0 || canvas.height <= 0) {
            return null
        }

        val pointer = Native.getComponentPointer(canvas) ?: return null

        return HWND(pointer)
    }

    private inner class HostCanvas : Canvas() {

        override fun getMinimumSize(): Dimension = SHRINKABLE_SIZE

        override fun getPreferredSize(): Dimension = SHRINKABLE_SIZE

        override fun addNotify() {
            super.addNotify()
            LOG.info("Canvas peer created")
            scheduleAttach()
        }

        override fun removeNotify() {
            LOG.info("Canvas peer about to be destroyed, releasing embedded window")
            releaseEmbedder()
            super.removeNotify()
        }
    }

    companion object {
        private val LOG = Logger.getInstance(VeilNativePanel::class.java)
        private const val WATCHDOG_INTERVAL_MS = 1000
        private val SHRINKABLE_SIZE = Dimension(1, 1)
    }
}
