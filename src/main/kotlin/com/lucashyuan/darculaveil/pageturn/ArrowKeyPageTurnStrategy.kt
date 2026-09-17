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
            var targets = [document, document.body];
            if (document.activeElement) {
                targets.push(document.activeElement);
            }
            for (var i = 0; i < targets.length; i++) {
                if (!targets[i]) {
                    continue;
                }
                targets[i].dispatchEvent(new KeyboardEvent("keydown", {
                    key: name, code: name, keyCode: code, which: code, bubbles: true, cancelable: true
                }));
            }
        """.trimIndent()
    }
}
