package com.lucashyuan.darculaveil

import com.intellij.openapi.util.text.StringUtil
import com.lucashyuan.darculaveil.style.VeilStyleDocument

object VeilScriptBuilder {

    private const val STYLE_ELEMENT_ID = "darcula-veil-style"
    private const val SVG_ELEMENT_ID = "darcula-veil-svg"

    fun buildInjectScript(styleDocument: VeilStyleDocument): String {
        val cssLiteral = toJsLiteral(styleDocument.css)
        val svgLiteral = toJsLiteral(styleDocument.svgMarkup)

        return """
            (function() {
                var root = document.head || document.documentElement;
                var style = document.getElementById("$STYLE_ELEMENT_ID");
                if (!style) {
                    style = document.createElement("style");
                    style.id = "$STYLE_ELEMENT_ID";
                    root.appendChild(style);
                }
                style.textContent = $cssLiteral;

                var markup = $svgLiteral;
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
                holder.innerHTML = markup;
            })();
        """.trimIndent()
    }

    fun buildRemoveScript(): String {
        return """
            (function() {
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
