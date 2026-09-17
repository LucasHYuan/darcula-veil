package com.lucashyuan.darculaveil

import com.intellij.openapi.util.text.StringUtil
import com.lucashyuan.darculaveil.pageturn.VeilPageTurnStrategies

object VeilPageTurnScript {

    private const val TURN_PROPERTY = "__darculaVeilTurn"
    private const val LISTENER_PROPERTY = "__darculaVeilTurnKeys"

    fun buildInstallScript(settings: VeilSettings.State): String {
        val body = VeilPageTurnStrategies.byId(settings.pageTurnStrategyId).buildTurnBody(settings)
        val forwardKey = toJsLiteral(settings.pageTurnForwardKey)
        val backwardKey = toJsLiteral(settings.pageTurnBackwardKey)

        return """
            (function() {
                window.$TURN_PROPERTY = function(direction) {
                    $body
                };

                if (window.$LISTENER_PROPERTY) {
                    document.removeEventListener("keydown", window.$LISTENER_PROPERTY, true);
                    window.$LISTENER_PROPERTY = null;
                }

                if (!${settings.pageTurnKeysEnabled}) {
                    return;
                }

                window.$LISTENER_PROPERTY = function(event) {
                    if (!event.isTrusted || event.defaultPrevented) {
                        return;
                    }
                    if (event.ctrlKey || event.altKey || event.metaKey) {
                        return;
                    }
                    var node = event.target;
                    var tag = node && node.tagName ? node.tagName.toLowerCase() : "";
                    if (tag === "input" || tag === "textarea" || (node && node.isContentEditable)) {
                        return;
                    }
                    if (event.key === $forwardKey) {
                        window.$TURN_PROPERTY(1);
                        event.preventDefault();
                        return;
                    }
                    if (event.key === $backwardKey) {
                        window.$TURN_PROPERTY(-1);
                        event.preventDefault();
                    }
                };

                document.addEventListener("keydown", window.$LISTENER_PROPERTY, true);
            })();
        """.trimIndent()
    }

    fun buildTurnCall(direction: Int): String {
        return "if (window.$TURN_PROPERTY) { window.$TURN_PROPERTY($direction); }"
    }

    private fun toJsLiteral(value: String): String = "\"" + StringUtil.escapeStringCharacters(value) + "\""
}
