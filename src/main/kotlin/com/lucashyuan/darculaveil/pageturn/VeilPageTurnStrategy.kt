package com.lucashyuan.darculaveil.pageturn

import com.lucashyuan.darculaveil.VeilSettings

interface VeilPageTurnStrategy {

    val id: String

    val displayName: String

    fun buildTurnBody(settings: VeilSettings.State): String
}
