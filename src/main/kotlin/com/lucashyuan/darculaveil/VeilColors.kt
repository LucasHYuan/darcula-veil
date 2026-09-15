package com.lucashyuan.darculaveil

import java.awt.Color
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

object VeilColors {

    fun luminance(color: Color): Double = (0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue) / 255.0

    fun blend(from: Color, to: Color, ratio: Double): Color {
        val clamped = ratio.coerceIn(0.0, 1.0)
        val red = from.red + (to.red - from.red) * clamped
        val green = from.green + (to.green - from.green) * clamped
        val blue = from.blue + (to.blue - from.blue) * clamped

        return Color(red.toInt().coerceIn(0, 255), green.toInt().coerceIn(0, 255), blue.toInt().coerceIn(0, 255))
    }

    fun resample(ramp: List<Color>, steps: Int): List<Color> {
        if (ramp.isEmpty()) {
            return emptyList()
        }

        if (ramp.size == 1) {
            return List(steps) { ramp.first() }
        }

        val lastIndex = (steps - 1).coerceAtLeast(1)

        return (0 until steps).map { index ->
            val position = index.toDouble() / lastIndex * (ramp.size - 1)
            val lower = floor(position).toInt().coerceIn(0, ramp.size - 1)
            val upper = ceil(position).toInt().coerceIn(0, ramp.size - 1)

            blend(ramp[lower], ramp[upper], position - lower)
        }
    }

    fun toChannelTable(palette: List<Color>, channel: (Color) -> Int): String {
        return palette.joinToString(" ") { String.format(Locale.ROOT, "%.4f", channel(it) / 255.0) }
    }
}
