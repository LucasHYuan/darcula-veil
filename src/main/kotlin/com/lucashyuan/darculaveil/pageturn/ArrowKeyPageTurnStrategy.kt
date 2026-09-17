package com.lucashyuan.darculaveil.pageturn

import com.lucashyuan.darculaveil.VeilSettings

object ArrowKeyPageTurnStrategy : VeilPageTurnStrategy {

    const val ID = "arrow-keys"

    override val id: String = ID

    override val displayName: String = "Send arrow keys to the page"

    override fun buildTurnBody(settings: VeilSettings.State): String {
        return """
            var forward = direction > 0;
            var name = forward ? "ArrowRight" : "ArrowLeft";
            var code = forward ? 39 : 37;
            var target = document.activeElement;

            if (!target || !target.isConnected) {
                target = document.body || document.documentElement;
            }

            target.dispatchEvent(new KeyboardEvent("keydown", {
                key: name, code: name, keyCode: code, which: code, bubbles: true, cancelable: true
            }));
        """.trimIndent()
    }
}
