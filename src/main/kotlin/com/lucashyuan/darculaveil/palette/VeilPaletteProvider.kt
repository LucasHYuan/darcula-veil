package com.lucashyuan.darculaveil.palette

import com.lucashyuan.darculaveil.VeilSettings
import java.awt.Color

interface VeilPaletteProvider {

    val id: String

    val displayName: String

    fun buildRamp(settings: VeilSettings.State): List<Color>
}
