package com.lucashyuan.darculaveil.pageturn

import com.lucashyuan.darculaveil.VeilSettings

object ScrollPageTurnStrategy : VeilPageTurnStrategy {

    const val ID = "scroll"

    override val id: String = ID

    override val displayName: String = "Scroll by viewport"

    override fun buildTurnBody(settings: VeilSettings.State): String {
        val ratio = settings.pageTurnScrollPercent.coerceIn(10, 100)

        return """
            var amount = Math.round(window.innerHeight * $ratio / 100);
            var scroller = document.scrollingElement || document.documentElement;
            var before = scroller.scrollTop;
            window.scrollBy({ top: direction * amount, left: 0, behavior: "smooth" });
            if (scroller.scrollTop === before) {
                scroller.scrollTop = before + direction * amount;
            }
        """.trimIndent()
    }
}
