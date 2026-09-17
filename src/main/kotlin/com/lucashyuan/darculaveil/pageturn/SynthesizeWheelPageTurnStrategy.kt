package com.lucashyuan.darculaveil.pageturn

import com.lucashyuan.darculaveil.VeilSettings

object SynthesizeWheelPageTurnStrategy : VeilPageTurnStrategy {

    const val ID = "synthesize-wheel"

    override val id: String = ID

    override val displayName: String = "Send a wheel event to the page"

    override fun buildTurnBody(settings: VeilSettings.State): String {
        val ratio = settings.pageTurnScrollPercent.coerceIn(10, 100)

        return """
            var centerX = Math.round(window.innerWidth / 2);
            var centerY = Math.round(window.innerHeight / 2);
            var amount = Math.round(window.innerHeight * $ratio / 100);
            var target = document.elementFromPoint(centerX, centerY) || document.body || document.documentElement;

            target.dispatchEvent(new WheelEvent("wheel", {
                deltaX: 0,
                deltaY: direction * amount,
                deltaMode: 0,
                clientX: centerX,
                clientY: centerY,
                bubbles: true,
                cancelable: true,
                composed: true
            }));
        """.trimIndent()
    }
}
