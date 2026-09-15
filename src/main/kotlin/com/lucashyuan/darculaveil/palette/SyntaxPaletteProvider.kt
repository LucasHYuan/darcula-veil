package com.lucashyuan.darculaveil.palette

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.lucashyuan.darculaveil.VeilColors
import com.lucashyuan.darculaveil.VeilSettings
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

    override val displayName: String = "Editor syntax colors"

    override fun buildRamp(settings: VeilSettings.State): List<Color> {
        val scheme = EditorColorsManager.getInstance().globalScheme

        return VeilColors.sortByLuminance(collect(scheme))
    }

    private fun collect(scheme: EditorColorsScheme): List<Color> {
        val collected = mutableListOf(scheme.defaultBackground)

        SYNTAX_KEYS.forEach { key ->
            val color = scheme.getAttributes(key)?.foregroundColor

            if (color != null) {
                collected.add(color)
            }
        }

        collected.add(scheme.defaultForeground)

        return collected
    }
}
