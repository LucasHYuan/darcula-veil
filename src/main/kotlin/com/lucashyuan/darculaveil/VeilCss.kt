package com.lucashyuan.darculaveil

import com.intellij.openapi.util.text.StringUtil
import java.util.Locale

object VeilCss {

    private const val STYLE_ELEMENT_ID = "darcula-veil-style"
    private const val MEDIA_SELECTORS = "img,video,canvas,svg,picture,iframe,[style*=\"background-image\"]"

    fun buildCss(settings: VeilSettings.State): String {
        val rules = StringBuilder()
        rules.append("html{filter:").append(buildFilterChain(settings)).append(";}")

        if (settings.revertMedia) {
            rules.append(MEDIA_SELECTORS).append("{filter:invert(1) hue-rotate(180deg);}")
        }

        return rules.toString()
    }

    fun buildInjectScript(css: String): String {
        val literal = "\"" + StringUtil.escapeStringCharacters(css) + "\""

        return """
            (function() {
                var element = document.getElementById("$STYLE_ELEMENT_ID");
                if (!element) {
                    element = document.createElement("style");
                    element.id = "$STYLE_ELEMENT_ID";
                    (document.head || document.documentElement).appendChild(element);
                }
                element.textContent = $literal;
            })();
        """.trimIndent()
    }

    fun buildRemoveScript(): String {
        return """
            (function() {
                var element = document.getElementById("$STYLE_ELEMENT_ID");
                if (element && element.parentNode) {
                    element.parentNode.removeChild(element);
                }
            })();
        """.trimIndent()
    }

    private fun buildFilterChain(settings: VeilSettings.State): String {
        val parts = listOf(
            "invert(${ratio(settings.invertPercent)})",
            "hue-rotate(${settings.hueRotateDegrees}deg)",
            "saturate(${ratio(settings.saturatePercent)})",
            "brightness(${ratio(settings.brightnessPercent)})",
            "contrast(${ratio(settings.contrastPercent)})"
        )

        return parts.joinToString(" ")
    }

    private fun ratio(percent: Int): String = String.format(Locale.ROOT, "%.2f", percent / 100.0)
}
