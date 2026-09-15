package com.lucashyuan.darculaveil.palette

object VeilPaletteProviders {

    val ALL: List<VeilPaletteProvider> = listOf(MonochromePaletteProvider, SyntaxPaletteProvider)

    fun byId(id: String): VeilPaletteProvider = ALL.firstOrNull { it.id == id } ?: SyntaxPaletteProvider

    fun byDisplayName(displayName: String): VeilPaletteProvider = ALL.firstOrNull { it.displayName == displayName } ?: SyntaxPaletteProvider

    fun displayNames(): List<String> = ALL.map { it.displayName }
}
