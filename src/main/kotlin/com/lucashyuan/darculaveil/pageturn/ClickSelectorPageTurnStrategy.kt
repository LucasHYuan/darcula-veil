package com.lucashyuan.darculaveil.pageturn

import com.intellij.openapi.util.text.StringUtil
import com.lucashyuan.darculaveil.VeilSettings

object ClickSelectorPageTurnStrategy : VeilPageTurnStrategy {

    const val ID = "click-selector"

    override val id: String = ID

    override val displayName: String = "Click next / previous elements"

    override fun buildTurnBody(settings: VeilSettings.State): String {
        val forward = toJsLiteral(settings.pageTurnForwardSelector)
        val backward = toJsLiteral(settings.pageTurnBackwardSelector)

        return """
            var selector = direction > 0 ? $forward : $backward;
            if (selector.length > 0) {
                var element = document.querySelector(selector);
                if (element) {
                    element.click();
                }
            }
        """.trimIndent()
    }

    private fun toJsLiteral(value: String): String = "\"" + StringUtil.escapeStringCharacters(value) + "\""
}
