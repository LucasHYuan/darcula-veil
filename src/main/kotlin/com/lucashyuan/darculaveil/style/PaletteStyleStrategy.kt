package com.lucashyuan.darculaveil.style

import com.lucashyuan.darculaveil.VeilColors
import com.lucashyuan.darculaveil.VeilSettings
import com.lucashyuan.darculaveil.palette.VeilPaletteProviders
import java.awt.Color
import java.util.Locale

object PaletteStyleStrategy : VeilStyleStrategy {

    const val ID = "palette"

    private const val FILTER_ELEMENT_ID = "darcula-veil-quantize"

    override val id: String = ID

    override val displayName: String = "Palette quantization (IDE theme colors)"

    override fun buildDocument(settings: VeilSettings.State): VeilStyleDocument {
        val palette = buildPalette(settings)

        if (palette.isEmpty()) {
            return VeilStyleDocument("", "")
        }

        return VeilStyleDocument(buildCss(palette, settings), buildSvgMarkup(palette, settings))
    }

    private fun buildCss(palette: List<Color>, settings: VeilSettings.State): String {
        if (settings.webVignettePercent <= 0) {
            return "html{filter:url(#$FILTER_ELEMENT_ID);}"
        }

        val edge = toHex(palette.last())
        val opacity = String.format(Locale.ROOT, "%.2f", settings.webVignettePercent / 100.0)
        val clearRadius = settings.webVignetteCenterPercent.coerceIn(0, 95)

        return buildString {
            append("html{background-color:").append(edge).append(";}")
            append("body{filter:url(#").append(FILTER_ELEMENT_ID).append(");}")
            append("html::after{content:\"\";position:fixed;left:0;top:0;right:0;bottom:0;pointer-events:none;z-index:2147483647;")
            append("opacity:").append(opacity).append(";")
            append("background:radial-gradient(ellipse at center,rgba(0,0,0,0) ").append(clearRadius).append("%,").append(edge).append(" 100%);}")
        }
    }

    private fun toHex(color: Color): String = String.format(Locale.ROOT, "#%02x%02x%02x", color.red, color.green, color.blue)

    fun buildPalette(settings: VeilSettings.State): List<Color> {
        val ramp = VeilPaletteProviders.byId(settings.paletteSourceId).buildRamp(settings)

        if (ramp.isEmpty()) {
            return emptyList()
        }

        val sampled = VeilColors.resample(ramp, settings.paletteSteps, settings.paletteFloorPercent / 100.0, settings.paletteCeilingPercent / 100.0)

        if (settings.paletteReversed) {
            return sampled.reversed()
        }

        return sampled
    }

    private fun buildSvgMarkup(palette: List<Color>, settings: VeilSettings.State): String {
        val tableRed = VeilColors.toChannelTable(palette) { it.red }
        val tableGreen = VeilColors.toChannelTable(palette) { it.green }
        val tableBlue = VeilColors.toChannelTable(palette) { it.blue }

        return """
            <svg xmlns="http://www.w3.org/2000/svg" width="0" height="0" focusable="false" aria-hidden="true">
              <filter id="$FILTER_ELEMENT_ID" color-interpolation-filters="sRGB">
                <feColorMatrix type="saturate" values="${ratio(settings.paletteSourceSaturationPercent)}"/>
                <feComponentTransfer>
                  <feFuncR type="discrete" tableValues="$tableRed"/>
                  <feFuncG type="discrete" tableValues="$tableGreen"/>
                  <feFuncB type="discrete" tableValues="$tableBlue"/>
                </feComponentTransfer>
              </filter>
            </svg>
        """.trimIndent()
    }

    private fun ratio(percent: Int): String = String.format(Locale.ROOT, "%.2f", percent / 100.0)
}
