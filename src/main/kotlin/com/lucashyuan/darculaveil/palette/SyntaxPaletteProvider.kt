package com.lucashyuan.darculaveil.palette

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.lucashyuan.darculaveil.VeilColors
import java.awt.Color

object SyntaxPaletteProvider : VeilPaletteProvider {

    const val ID = "syntax"

    private val SYNTAX_KEYS: List<TextAttributesKey> = listOf(
        DefaultLanguageHighlighterColors.LINE_COMMENT,
        DefaultLanguageHighlighterColors.KEYWORD,
        DefaultLanguageHighlighterColors.STRING,
        DefaultLanguageHighlighterColors.NUMBER,
        DefaultLanguageHighlighterColors.FUNCTION_DECLARATION,
        DefaultLanguageHighlighterColors.CLASS_NAME
    )

    override val id: String = ID

    override val displayName: String = "Syntax highlighting colors"

    override fun buildPalette(scheme: EditorColorsScheme, steps: Int): List<Color> {
        return VeilColors.resample(collectRamp(scheme), steps)
    }

    private fun collectRamp(scheme: EditorColorsScheme): List<Color> {
        val collected = mutableListOf(scheme.defaultBackground)

        SYNTAX_KEYS.forEach { key ->
            val color = scheme.getAttributes(key)?.foregroundColor

            if (color != null) {
                collected.add(color)
            }
        }

        collected.add(scheme.defaultForeground)

        return collected.distinct().sortedBy { VeilColors.luminance(it) }
    }
}
