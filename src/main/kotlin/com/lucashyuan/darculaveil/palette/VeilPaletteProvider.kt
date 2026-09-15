package com.lucashyuan.darculaveil.palette

import com.intellij.openapi.editor.colors.EditorColorsScheme
import java.awt.Color

interface VeilPaletteProvider {

    val id: String

    val displayName: String

    fun buildPalette(scheme: EditorColorsScheme, steps: Int): List<Color>
}
