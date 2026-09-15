package com.lucashyuan.darculaveil.palette

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.lucashyuan.darculaveil.VeilColors
import java.awt.Color

object MonochromePaletteProvider : VeilPaletteProvider {

    const val ID = "mono"

    override val id: String = ID

    override val displayName: String = "Monochrome (background to foreground)"

    override fun buildPalette(scheme: EditorColorsScheme, steps: Int): List<Color> {
        return VeilColors.resample(listOf(scheme.defaultBackground, scheme.defaultForeground), steps)
    }
}
