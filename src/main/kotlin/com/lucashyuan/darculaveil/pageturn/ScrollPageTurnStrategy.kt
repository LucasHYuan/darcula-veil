package com.lucashyuan.darculaveil.pageturn

import com.lucashyuan.darculaveil.VeilSettings

object ScrollPageTurnStrategy : VeilPageTurnStrategy {

    const val ID = "scroll"

    override val id: String = ID

    override val displayName: String = "Scroll by viewport"

    override fun buildTurnBody(settings: VeilSettings.State): String {
        val ratio = settings.pageTurnScrollPercent.coerceIn(10, 100)

        return """
            var scroller = window.__darculaVeilScroller();
            var amount = Math.round(scroller.clientHeight * $ratio / 100);
            var before = scroller.scrollTop;
            scroller.scrollTop = before + direction * amount;
            if (scroller.scrollTop === before) {
                window.scrollBy(0, direction * amount);
            }
        """.trimIndent()
    }
}
