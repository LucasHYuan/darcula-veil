package com.lucashyuan.darculaveil.palette

import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import com.lucashyuan.darculaveil.VeilColors
import com.lucashyuan.darculaveil.VeilSettings
import java.awt.Color

object IdeUiPaletteProvider : VeilPaletteProvider {

    const val ID = "ide-ui"

    override val id: String = ID

    override val displayName: String = "IDE tool window chrome"

    override fun buildRamp(settings: VeilSettings.State): List<Color> {
        val collected = listOf(UIUtil.getPanelBackground(), JBColor.border(), UIUtil.getLabelForeground())

        return VeilColors.sortByLuminance(collected.map { Color(it.rgb, false) })
    }
}
