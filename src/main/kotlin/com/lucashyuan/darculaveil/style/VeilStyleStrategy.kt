package com.lucashyuan.darculaveil.style

import com.lucashyuan.darculaveil.VeilSettings

interface VeilStyleStrategy {

    val id: String

    val displayName: String

    fun buildDocument(settings: VeilSettings.State): VeilStyleDocument
}
