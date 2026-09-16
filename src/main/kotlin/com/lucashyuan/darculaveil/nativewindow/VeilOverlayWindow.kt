package com.lucashyuan.darculaveil.nativewindow

import com.intellij.openapi.diagnostic.Logger
import com.lucashyuan.darculaveil.VeilSettings
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HBITMAP
import com.sun.jna.platform.win32.WinDef.HDC
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.ptr.PointerByReference
import java.awt.Color

class VeilOverlayWindow(private val host: HWND) {

    private val user32 = VeilUser32.INSTANCE
    private val gdi32 = VeilGdi32.INSTANCE

    private var handle: HWND? = null
    private var device: HDC? = null
    private var bitmap: HBITMAP? = null
    private var previousObject: HANDLE? = null
    private var bits: Pointer? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var renderedSignature = ""

    fun update(width: Int, height: Int, tint: Color, settings: VeilSettings.State) {
        if (width <= 0 || height <= 0) {
            return
        }

        if (!settings.overlayEnabled) {
            setVisible(false)
            return
        }

        if (!ensureWindow(width, height)) {
            return
        }

        if (!ensureSurface(width, height)) {
            return
        }

        val signature = VeilOverlayRenderer.signatureOf(width, height, tint, settings)

        if (signature != renderedSignature) {
            writePixels(VeilOverlayRenderer.render(width, height, tint, settings))
            renderedSignature = signature
        }

        pushToWindow(width, height)
        raise(width, height)
    }

    fun setVisible(visible: Boolean) {
        val current = handle ?: return

        user32.ShowWindow(current, if (visible) VeilUser32.SW_SHOW else VeilUser32.SW_HIDE)
    }

    fun destroy() {
        releaseSurface()

        val current = handle

        if (current != null) {
            user32.DestroyWindow(current)
            handle = null
        }

        renderedSignature = ""
    }

    private fun ensureWindow(width: Int, height: Int): Boolean {
        if (handle != null) {
            return true
        }

        val exStyle = VeilUser32.WS_EX_LAYERED or VeilUser32.WS_EX_TRANSPARENT or VeilUser32.WS_EX_NOACTIVATE
        val created = user32.CreateWindowEx(exStyle, OVERLAY_CLASS, "", VeilUser32.WS_CHILD or VeilUser32.WS_VISIBLE, 0, 0, width, height, host, null, null, null)

        if (created == null) {
            LOG.warn("Failed to create overlay window")
            return false
        }

        handle = created

        return true
    }

    private fun ensureSurface(width: Int, height: Int): Boolean {
        if (device != null && surfaceWidth == width && surfaceHeight == height) {
            return true
        }

        releaseSurface()

        val screenDevice = user32.GetDC(null)
        val created = gdi32.CreateCompatibleDC(screenDevice)

        if (screenDevice != null) {
            user32.ReleaseDC(null, screenDevice)
        }

        if (created == null) {
            LOG.warn("Failed to create overlay device context")
            return false
        }

        val header = VeilBitmapInfoHeader()
        header.biSize = VeilGdi32.BITMAP_INFO_HEADER_SIZE
        header.biWidth = width
        header.biHeight = -height
        header.biPlanes = 1
        header.biBitCount = VeilGdi32.BITS_PER_PIXEL.toShort()
        header.biCompression = VeilGdi32.BI_RGB

        val bitsReference = PointerByReference()
        val section = gdi32.CreateDIBSection(created, header, VeilGdi32.DIB_RGB_COLORS, bitsReference, null, 0)

        if (section == null || bitsReference.value == null) {
            LOG.warn("Failed to create overlay DIB section")
            gdi32.DeleteDC(created)
            return false
        }

        device = created
        bitmap = section
        bits = bitsReference.value
        previousObject = gdi32.SelectObject(created, section)
        surfaceWidth = width
        surfaceHeight = height
        renderedSignature = ""

        return true
    }

    private fun writePixels(pixels: IntArray) {
        bits?.write(0, pixels, 0, pixels.size)
    }

    private fun pushToWindow(width: Int, height: Int) {
        val current = handle ?: return
        val currentDevice = device ?: return

        val size = VeilSize()
        size.cx = width
        size.cy = height

        val sourcePoint = VeilPoint()

        val blend = VeilBlendFunction()
        blend.blendOp = VeilGdi32.AC_SRC_OVER
        blend.blendFlags = 0
        blend.sourceConstantAlpha = ALPHA_OPAQUE
        blend.alphaFormat = VeilGdi32.AC_SRC_ALPHA

        user32.UpdateLayeredWindow(current, null, null, size, currentDevice, sourcePoint, 0, blend, VeilGdi32.ULW_ALPHA)
    }

    private fun raise(width: Int, height: Int) {
        val current = handle ?: return

        user32.SetWindowPos(current, null, 0, 0, width, height, VeilUser32.SWP_NOACTIVATE)
        user32.ShowWindow(current, VeilUser32.SW_SHOW)
    }

    private fun releaseSurface() {
        val currentDevice = device
        val currentBitmap = bitmap
        val previous = previousObject

        if (currentDevice != null && previous != null) {
            gdi32.SelectObject(currentDevice, previous)
        }

        if (currentBitmap != null) {
            gdi32.DeleteObject(currentBitmap)
        }

        if (currentDevice != null) {
            gdi32.DeleteDC(currentDevice)
        }

        device = null
        bitmap = null
        previousObject = null
        bits = null
        surfaceWidth = 0
        surfaceHeight = 0
    }

    companion object {
        private val LOG = Logger.getInstance(VeilOverlayWindow::class.java)
        private const val OVERLAY_CLASS = "STATIC"
        private const val ALPHA_OPAQUE: Byte = -1
    }
}
