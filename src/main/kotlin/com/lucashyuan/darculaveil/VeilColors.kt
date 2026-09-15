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

    fun sampleAt(ramp: List<Color>, position: Double): Color {
        val clamped = position.coerceIn(0.0, 1.0)
        val scaled = clamped * (ramp.size - 1)
        val lower = floor(scaled).toInt().coerceIn(0, ramp.size - 1)
        val upper = ceil(scaled).toInt().coerceIn(0, ramp.size - 1)

        return blend(ramp[lower], ramp[upper], scaled - lower)
    }

    fun resample(ramp: List<Color>, steps: Int, from: Double, to: Double): List<Color> {
        if (ramp.isEmpty()) {
            return emptyList()
        }

        if (ramp.size == 1) {
            return List(steps) { ramp.first() }
        }

        val lastIndex = (steps - 1).coerceAtLeast(1)

        return (0 until steps).map { index ->
            sampleAt(ramp, from + (to - from) * index / lastIndex)
        }
    }

    fun sortByLuminance(colors: Collection<Color>): List<Color> = colors.distinct().sortedBy { luminance(it) }

    fun toChannelTable(palette: List<Color>, channel: (Color) -> Int): String {
        return palette.joinToString(" ") { String.format(Locale.ROOT, "%.4f", channel(it) / 255.0) }
    }

    fun parseHexList(value: String): List<Color> {
        return value.split(',', ' ', '\n', '\t')
            .map { it.trim().removePrefix("#") }
            .filter { it.length == 6 && it.all { character -> character.isDigit() || character.lowercaseChar() in 'a'..'f' } }
            .map { Color(it.toInt(16)) }
    }
}
