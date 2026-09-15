package com.lucashyuan.darculaveil

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(name = "DarculaVeilSettings", storages = [Storage("darcula-veil.xml")])
class VeilSettings : PersistentStateComponent<VeilSettings.State> {

    class State {
        @JvmField var homeUrl: String = DEFAULT_HOME_URL
        @JvmField var veilEnabled: Boolean = true
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

        fun getInstance(): VeilSettings = ApplicationManager.getApplication().getService(VeilSettings::class.java)

        fun state(): State = getInstance().state
    }
}
