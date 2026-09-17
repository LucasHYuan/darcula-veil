package com.lucashyuan.darculaveil.style

import com.lucashyuan.darculaveil.VeilSettings
import java.awt.Color
import java.util.Locale

class VeilStyleDocument(val css: String, val svgMarkup: String)

object VeilVignette {

    fun wrap(filterValue: String, edge: Color, settings: VeilSettings.State): String {
        if (settings.webVignettePercent <= 0) {
            return "html{filter:$filterValue;}"
        }

        val edgeHex = toHex(edge)
        val opacity = String.format(Locale.ROOT, "%.2f", settings.webVignettePercent / 100.0)
        val clearRadius = settings.webVignetteCenterPercent.coerceIn(0, 95)

        return buildString {
            append("html{background-color:").append(edgeHex).append(";}")
            append("body{filter:").append(filterValue).append(";}")
            append("html::after{content:\"\";position:fixed;left:0;top:0;right:0;bottom:0;pointer-events:none;z-index:2147483647;")
            append("opacity:").append(opacity).append(";")
            append("background:radial-gradient(ellipse at center,rgba(0,0,0,0) ").append(clearRadius).append("%,").append(edgeHex).append(" 100%);}")
        }
    }

    fun toHex(color: Color): String = String.format(Locale.ROOT, "#%02x%02x%02x", color.red, color.green, color.blue)
}
