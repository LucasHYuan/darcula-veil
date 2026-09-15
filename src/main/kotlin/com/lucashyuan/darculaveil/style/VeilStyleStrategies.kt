package com.lucashyuan.darculaveil.style

object VeilStyleStrategies {

    val ALL: List<VeilStyleStrategy> = listOf(PaletteStyleStrategy, FilterStyleStrategy)

    fun byId(id: String): VeilStyleStrategy = ALL.firstOrNull { it.id == id } ?: PaletteStyleStrategy

    fun byDisplayName(displayName: String): VeilStyleStrategy = ALL.firstOrNull { it.displayName == displayName } ?: PaletteStyleStrategy

    fun displayNames(): List<String> = ALL.map { it.displayName }
}
