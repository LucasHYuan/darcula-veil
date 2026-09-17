package com.lucashyuan.darculaveil

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.lucashyuan.darculaveil.pageturn.ScrollPageTurnStrategy
import com.lucashyuan.darculaveil.palette.MonochromePaletteProvider
import com.lucashyuan.darculaveil.style.PaletteStyleStrategy

@Service(Service.Level.APP)
@State(name = "DarculaVeilSettings", storages = [Storage("darcula-veil.xml")])
class VeilSettings : PersistentStateComponent<VeilSettings.State> {

    class State {
        @JvmField var homeUrl: String = DEFAULT_HOME_URL
        @JvmField var veilEnabled: Boolean = true
        @JvmField var styleModeId: String = PaletteStyleStrategy.ID

        @JvmField var paletteSourceId: String = MonochromePaletteProvider.ID
        @JvmField var paletteSteps: Int = DEFAULT_PALETTE_STEPS
        @JvmField var paletteSourceSaturationPercent: Int = DEFAULT_PALETTE_SATURATION_PERCENT
        @JvmField var paletteReversed: Boolean = true
        @JvmField var paletteFloorPercent: Int = DEFAULT_PALETTE_FLOOR_PERCENT
        @JvmField var paletteCeilingPercent: Int = DEFAULT_PALETTE_CEILING_PERCENT
        @JvmField var customPaletteHex: String = ""
        @JvmField var webVignettePercent: Int = DEFAULT_WEB_VIGNETTE_PERCENT
        @JvmField var webVignetteCenterPercent: Int = DEFAULT_WEB_VIGNETTE_CENTER_PERCENT

        @JvmField var invertPercent: Int = 92
        @JvmField var hueRotateDegrees: Int = 180
        @JvmField var saturatePercent: Int = 85
        @JvmField var brightnessPercent: Int = 95
        @JvmField var contrastPercent: Int = 100
        @JvmField var revertMedia: Boolean = true

        @JvmField var pageTurnStrategyId: String = ScrollPageTurnStrategy.ID
        @JvmField var pageTurnScrollPercent: Int = DEFAULT_PAGE_TURN_SCROLL_PERCENT
        @JvmField var pageTurnKeysEnabled: Boolean = true
        @JvmField var keymapBridgeEnabled: Boolean = true
        @JvmField var pageTurnForwardKey: String = "ArrowRight"
        @JvmField var pageTurnBackwardKey: String = "ArrowLeft"
        @JvmField var pageTurnForwardSelector: String = ""
        @JvmField var pageTurnBackwardSelector: String = ""

        @JvmField var nativeFocusTransferEnabled: Boolean = true
        @JvmField var nativeLayeredEnabled: Boolean = false
        @JvmField var nativeOpacityPercent: Int = DEFAULT_NATIVE_OPACITY_PERCENT

        @JvmField var overlayEnabled: Boolean = true
        @JvmField var overlayTintPercent: Int = DEFAULT_OVERLAY_TINT_PERCENT
        @JvmField var overlayVignettePercent: Int = DEFAULT_OVERLAY_VIGNETTE_PERCENT
        @JvmField var overlayScanlineSpacing: Int = DEFAULT_OVERLAY_SCANLINE_SPACING
        @JvmField var overlayScanlinePercent: Int = DEFAULT_OVERLAY_SCANLINE_PERCENT
    }

    private val currentState = State()

    override fun getState(): State = currentState

    override fun loadState(loaded: State) {
        XmlSerializerUtil.copyBean(loaded, currentState)
    }

    companion object {
        const val DEFAULT_HOME_URL = "https://www.jetbrains.com/"
        const val DEFAULT_PALETTE_STEPS = 6
        const val DEFAULT_PALETTE_SATURATION_PERCENT = 0
        const val DEFAULT_PALETTE_FLOOR_PERCENT = 0
        const val DEFAULT_PALETTE_CEILING_PERCENT = 70
        const val MIN_PALETTE_STEPS = 2
        const val MAX_PALETTE_STEPS = 16
        const val MIN_PERCENT = 0
        const val MAX_PERCENT = 200
        const val MIN_HUE_DEGREES = 0
        const val MAX_HUE_DEGREES = 360
        const val DEFAULT_NATIVE_OPACITY_PERCENT = 100
        const val MIN_NATIVE_OPACITY_PERCENT = 10
        const val MAX_NATIVE_OPACITY_PERCENT = 100
        const val DEFAULT_PAGE_TURN_SCROLL_PERCENT = 90
        const val MIN_PAGE_TURN_SCROLL_PERCENT = 10
        const val DEFAULT_WEB_VIGNETTE_PERCENT = 45
        const val DEFAULT_WEB_VIGNETTE_CENTER_PERCENT = 35
        const val DEFAULT_OVERLAY_TINT_PERCENT = 30
        const val DEFAULT_OVERLAY_VIGNETTE_PERCENT = 45
        const val DEFAULT_OVERLAY_SCANLINE_SPACING = 0
        const val DEFAULT_OVERLAY_SCANLINE_PERCENT = 18
        const val MAX_SCANLINE_SPACING = 32

        fun getInstance(): VeilSettings = ApplicationManager.getApplication().getService(VeilSettings::class.java)

        fun state(): State = getInstance().state
    }
}
