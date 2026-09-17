package com.lucashyuan.darculaveil.pageturn

object VeilPageTurnStrategies {

    val ALL: List<VeilPageTurnStrategy> = listOf(ScrollPageTurnStrategy, ArrowKeyPageTurnStrategy, ClickSelectorPageTurnStrategy)

    fun byId(id: String): VeilPageTurnStrategy = ALL.firstOrNull { it.id == id } ?: ScrollPageTurnStrategy

    fun byDisplayName(displayName: String): VeilPageTurnStrategy = ALL.firstOrNull { it.displayName == displayName } ?: ScrollPageTurnStrategy

    fun displayNames(): List<String> = ALL.map { it.displayName }
}
