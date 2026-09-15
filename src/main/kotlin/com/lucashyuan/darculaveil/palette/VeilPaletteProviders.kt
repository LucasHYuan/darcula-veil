package com.lucashyuan.darculaveil.palette

object VeilPaletteProviders {

    val ALL: List<VeilPaletteProvider> = listOf(MonochromePaletteProvider, IdeUiPaletteProvider, SyntaxPaletteProvider, CustomPaletteProvider)

    fun byId(id: String): VeilPaletteProvider = ALL.firstOrNull { it.id == id } ?: MonochromePaletteProvider

    fun byDisplayName(displayName: String): VeilPaletteProvider = ALL.firstOrNull { it.displayName == displayName } ?: MonochromePaletteProvider

    fun displayNames(): List<String> = ALL.map { it.displayName }
}
