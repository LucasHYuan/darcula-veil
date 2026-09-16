package com.lucashyuan.darculaveil.nativewindow

import com.lucashyuan.darculaveil.VeilSettings
import java.awt.Color
import kotlin.math.min
import kotlin.math.sqrt

object VeilOverlayRenderer {

    fun render(width: Int, height: Int, tint: Color, settings: VeilSettings.State): IntArray {
        val pixels = IntArray(width * height)
        val baseAlpha = toChannel(settings.overlayTintPercent)
        val vignetteAlpha = toChannel(settings.overlayVignettePercent)
        val scanlineAlpha = toChannel(settings.overlayScanlinePercent)
        val spacing = settings.overlayScanlineSpacing
        val centerX = width / 2.0
        val centerY = height / 2.0
        val maxDistance = sqrt(centerX * centerX + centerY * centerY)

        for (y in 0 until height) {
            val scanlineBoost = if (spacing > 1 && y % spacing == 0) scanlineAlpha else 0
            val rowOffset = y * width

            for (x in 0 until width) {
                val alpha = min(255, baseAlpha + vignetteAt(x, y, centerX, centerY, maxDistance, vignetteAlpha) + scanlineBoost)
                pixels[rowOffset + x] = premultiply(tint, alpha)
            }
        }

        return pixels
    }

    fun signatureOf(width: Int, height: Int, tint: Color, settings: VeilSettings.State): String {
        return "$width|$height|${tint.rgb}|${settings.overlayTintPercent}|${settings.overlayVignettePercent}|${settings.overlayScanlineSpacing}|${settings.overlayScanlinePercent}"
    }

    private fun vignetteAt(x: Int, y: Int, centerX: Double, centerY: Double, maxDistance: Double, vignetteAlpha: Int): Int {
        if (vignetteAlpha == 0 || maxDistance <= 0.0) {
            return 0
        }

        val deltaX = x - centerX
        val deltaY = y - centerY
        val normalized = sqrt(deltaX * deltaX + deltaY * deltaY) / maxDistance

        return (normalized * normalized * vignetteAlpha).toInt()
    }

    private fun premultiply(tint: Color, alpha: Int): Int {
        val red = tint.red * alpha / 255
        val green = tint.green * alpha / 255
        val blue = tint.blue * alpha / 255

        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun toChannel(percent: Int): Int = (percent.coerceIn(0, 100) * 255 / 100)
}
