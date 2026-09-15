package com.lucashyuan.darculaveil.palette

import com.lucashyuan.darculaveil.VeilColors
import com.lucashyuan.darculaveil.VeilSettings
import java.awt.Color

object CustomPaletteProvider : VeilPaletteProvider {

    const val ID = "custom"

    override val id: String = ID

    override val displayName: String = "Custom hex list"

    override fun buildRamp(settings: VeilSettings.State): List<Color> {
        val parsed = VeilColors.parseHexList(settings.customPaletteHex)

        if (parsed.size < 2) {
            return MonochromePaletteProvider.buildRamp(settings)
        }

        return VeilColors.sortByLuminance(parsed)
    }
}
