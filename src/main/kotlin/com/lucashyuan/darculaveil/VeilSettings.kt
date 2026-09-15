package com.lucashyuan.darculaveil

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.lucashyuan.darculaveil.palette.SyntaxPaletteProvider
import com.lucashyuan.darculaveil.style.PaletteStyleStrategy

@Service(Service.Level.APP)
@State(name = "DarculaVeilSettings", storages = [Storage("darcula-veil.xml")])
class VeilSettings : PersistentStateComponent<VeilSettings.State> {

    class State {
        @JvmField var homeUrl: String = DEFAULT_HOME_URL
        @JvmField var veilEnabled: Boolean = true
        @JvmField var styleModeId: String = PaletteStyleStrategy.ID

        @JvmField var paletteSourceId: String = SyntaxPaletteProvider.ID
        @JvmField var paletteSteps: Int = DEFAULT_PALETTE_STEPS
        @JvmField var paletteSourceSaturationPercent: Int = DEFAULT_PALETTE_SATURATION_PERCENT

        @JvmField var invertPercent: Int = 92
        @JvmField var hueRotateDegrees: Int = 180
        @JvmField var saturatePercent: Int = 85
        @JvmField var brightnessPercent: Int = 95
        @JvmField var contrastPercent: Int = 100
        @JvmField var revertMedia: Boolean = true
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
        const val MIN_PALETTE_STEPS = 2
        const val MAX_PALETTE_STEPS = 16
        const val MIN_PERCENT = 0
        const val MAX_PERCENT = 200
        const val MIN_HUE_DEGREES = 0
        const val MAX_HUE_DEGREES = 360

        fun getInstance(): VeilSettings = ApplicationManager.getApplication().getService(VeilSettings::class.java)

        fun state(): State = getInstance().state
    }
}
