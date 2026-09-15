package com.lucashyuan.darculaveil.palette

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.lucashyuan.darculaveil.VeilSettings
import java.awt.Color

object MonochromePaletteProvider : VeilPaletteProvider {

    const val ID = "mono"

    override val id: String = ID

    override val displayName: String = "Editor monochrome (background to foreground)"

    override fun buildRamp(settings: VeilSettings.State): List<Color> {
        val scheme = EditorColorsManager.getInstance().globalScheme

        return listOf(scheme.defaultBackground, scheme.defaultForeground)
    }
}
