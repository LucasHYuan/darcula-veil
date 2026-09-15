package com.lucashyuan.darculaveil

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JPanel

class VeilPalettePreview(private val paletteSupplier: () -> List<Color>) : JPanel() {

    init {
        preferredSize = Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT)
        minimumSize = preferredSize
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)

        val palette = paletteSupplier()

        if (palette.isEmpty()) {
            return
        }

        val cellWidth = width.toDouble() / palette.size

        palette.forEachIndexed { index, color ->
            val left = (index * cellWidth).toInt()
            val right = ((index + 1) * cellWidth).toInt()
            graphics.color = color
            graphics.fillRect(left, 0, right - left, height)
        }
    }

    companion object {
        private const val PREVIEW_WIDTH = 360
        private const val PREVIEW_HEIGHT = 32
    }
}
