package com.lucashyuan.darculaveil.style

import com.lucashyuan.darculaveil.VeilSettings
import java.util.Locale

object FilterStyleStrategy : VeilStyleStrategy {

    const val ID = "filter"

    private const val MEDIA_SELECTORS = "img,video,canvas,picture,iframe,[style*=\"background-image\"]"

    override val id: String = ID

    override val displayName: String = "Continuous filter (invert / saturate / brightness)"

    override fun buildDocument(settings: VeilSettings.State): VeilStyleDocument {
        val rules = StringBuilder()
        rules.append("html{filter:").append(buildFilterChain(settings)).append(";}")

        if (settings.revertMedia) {
            rules.append(MEDIA_SELECTORS).append("{filter:invert(1) hue-rotate(180deg);}")
        }

        return VeilStyleDocument(rules.toString(), "")
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
