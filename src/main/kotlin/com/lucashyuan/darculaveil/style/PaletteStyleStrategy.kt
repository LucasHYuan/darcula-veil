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

        return VeilStyleDocument("html{filter:url(#$FILTER_ELEMENT_ID);}", buildSvgMarkup(palette, settings))
    }

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
