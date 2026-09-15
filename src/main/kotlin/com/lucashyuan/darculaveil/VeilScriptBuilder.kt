package com.lucashyuan.darculaveil

import com.intellij.openapi.util.text.StringUtil
import com.lucashyuan.darculaveil.style.VeilStyleDocument

object VeilScriptBuilder {

    private const val STYLE_ELEMENT_ID = "darcula-veil-style"
    private const val SVG_ELEMENT_ID = "darcula-veil-svg"
    private const val STATE_PROPERTY = "__darculaVeilState"
    private const val OBSERVER_PROPERTY = "__darculaVeilObserver"
    private const val GUARD_INTERVAL_MS = 200

    fun buildInjectScript(styleDocument: VeilStyleDocument): String {
        val cssLiteral = toJsLiteral(styleDocument.css)
        val svgLiteral = toJsLiteral(styleDocument.svgMarkup)

        return """
            (function() {
                var css = $cssLiteral;
                var markup = $svgLiteral;

                function ensureStyle() {
                    var root = document.head || document.documentElement;
                    var style = document.getElementById("$STYLE_ELEMENT_ID");
                    if (!style) {
                        style = document.createElement("style");
                        style.id = "$STYLE_ELEMENT_ID";
                        root.appendChild(style);
                    }
                    if (style.textContent !== css) {
                        style.textContent = css;
                    }
                }

                function ensureSvg() {
                    var holder = document.getElementById("$SVG_ELEMENT_ID");
                    if (markup.length === 0) {
                        if (holder && holder.parentNode) {
                            holder.parentNode.removeChild(holder);
                        }
                        return;
                    }
                    if (!holder) {
                        holder = document.createElement("div");
                        holder.id = "$SVG_ELEMENT_ID";
                        holder.setAttribute("style", "position:absolute;width:0;height:0;overflow:hidden;pointer-events:none;");
                        (document.body || document.documentElement).appendChild(holder);
                    }
                    if (holder.innerHTML !== markup) {
                        holder.innerHTML = markup;
                    }
                }

                function apply() {
                    ensureStyle();
                    ensureSvg();
                }

                window.$STATE_PROPERTY = { apply: apply, hasSvg: markup.length > 0, scheduled: false };
                apply();

                if (!window.$OBSERVER_PROPERTY) {
                    window.$OBSERVER_PROPERTY = new MutationObserver(function() {
                        var state = window.$STATE_PROPERTY;
                        if (!state || state.scheduled) {
                            return;
                        }
                        state.scheduled = true;
                        setTimeout(function() {
                            var current = window.$STATE_PROPERTY;
                            if (!current) {
                                return;
                            }
                            current.scheduled = false;
                            var styleLost = !document.getElementById("$STYLE_ELEMENT_ID");
                            var svgLost = current.hasSvg && !document.getElementById("$SVG_ELEMENT_ID");
                            if (styleLost || svgLost) {
                                current.apply();
                            }
                        }, $GUARD_INTERVAL_MS);
                    });
                    window.$OBSERVER_PROPERTY.observe(document.documentElement, { childList: true, subtree: true });
                }
            })();
        """.trimIndent()
    }

    fun buildRemoveScript(): String {
        return """
            (function() {
                if (window.$OBSERVER_PROPERTY) {
                    window.$OBSERVER_PROPERTY.disconnect();
                    window.$OBSERVER_PROPERTY = null;
                }
                window.$STATE_PROPERTY = null;

                var ids = ["$STYLE_ELEMENT_ID", "$SVG_ELEMENT_ID"];
                for (var i = 0; i < ids.length; i++) {
                    var element = document.getElementById(ids[i]);
                    if (element && element.parentNode) {
                        element.parentNode.removeChild(element);
                    }
                }
            })();
        """.trimIndent()
    }

    private fun toJsLiteral(value: String): String = "\"" + StringUtil.escapeStringCharacters(value) + "\""
}
