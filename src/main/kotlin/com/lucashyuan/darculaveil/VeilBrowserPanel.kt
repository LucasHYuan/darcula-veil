package com.lucashyuan.darculaveil

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.UIUtil
import com.lucashyuan.darculaveil.style.VeilStyleStrategies
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefKeyboardHandler
import org.cef.handler.CefKeyboardHandlerAdapter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import java.awt.BorderLayout
import javax.swing.JPanel

class VeilBrowserPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {

    private val browser = JBCefBrowser(VeilSettings.state().homeUrl)

    init {
        Disposer.register(this, browser)
        background = UIUtil.getPanelBackground()
        add(browser.component, BorderLayout.CENTER)
        installLoadHandler()
        installLifeSpanHandler()
        installKeyboardHandler()
        subscribeToStateChanges()
    }

    fun loadUrl(url: String) {
        browser.loadURL(url)
    }

    fun reload() {
        browser.cefBrowser.reload()
    }

    fun goBack() {
        browser.cefBrowser.goBack()
    }

    fun goForward() {
        browser.cefBrowser.goForward()
    }

    fun canGoBack(): Boolean = browser.cefBrowser.canGoBack()

    fun canGoForward(): Boolean = browser.cefBrowser.canGoForward()

    fun showMediaReport() {
        browser.loadHTML(VeilMediaReport.buildHtml())
    }

    fun pauseMedia() {
        executeScript(VeilMediaScript.buildPauseScript())
    }

    fun refreshVeil() {
        executeScript(buildVeilScript())
        executeInAllFrames(VeilPageTurnScript.buildInstallScript(VeilSettings.state()))
    }

    fun turnPage(direction: Int) {
        executeInAllFrames(VeilPageTurnScript.buildTurnCall(direction))
    }

    fun showPageTurnDiagnostics() {
        executeInAllFrames(VeilPageTurnScript.buildDiagnosticScript())
    }

    override fun dispose() {
    }

    private fun buildVeilScript(): String {
        val settings = VeilSettings.state()

        if (!settings.veilEnabled) {
            return VeilScriptBuilder.buildRemoveScript()
        }

        val strategy = VeilStyleStrategies.byId(settings.styleModeId)

        return VeilScriptBuilder.buildInjectScript(strategy.buildDocument(settings))
    }

    private fun installLoadHandler() {
        val handler = object : CefLoadHandlerAdapter() {
            override fun onLoadStart(cefBrowser: CefBrowser, frame: CefFrame, transitionType: CefRequest.TransitionType) {
                refreshVeil()
            }

            override fun onLoadEnd(cefBrowser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                refreshVeil()
            }
        }

        browser.jbCefClient.addLoadHandler(handler, browser.cefBrowser)
    }

    private fun installKeyboardHandler() {
        val handler = object : CefKeyboardHandlerAdapter() {
            override fun onPreKeyEvent(cefBrowser: CefBrowser, event: CefKeyboardHandler.CefKeyEvent, isShortcut: BoolRef): Boolean {
                if (!VeilSettings.state().keymapBridgeEnabled) {
                    return false
                }

                if (event.type != CefKeyboardHandler.CefKeyEvent.EventType.KEYEVENT_RAWKEYDOWN) {
                    return false
                }

                val actionId = VeilKeymapBridge.resolveActionId(event.windows_key_code, event.modifiers) ?: return false

                VeilKeymapBridge.invoke(project, actionId)

                return true
            }
        }

        browser.jbCefClient.addKeyboardHandler(handler, browser.cefBrowser)
    }

    private fun installLifeSpanHandler() {
        val handler = object : CefLifeSpanHandlerAdapter() {
            override fun onBeforePopup(cefBrowser: CefBrowser, frame: CefFrame, targetUrl: String?, targetFrameName: String?): Boolean {
                if (targetUrl.isNullOrBlank()) {
                    return true
                }

                ApplicationManager.getApplication().invokeLater { loadUrl(targetUrl) }

                return true
            }
        }

        browser.jbCefClient.addLifeSpanHandler(handler, browser.cefBrowser)
    }

    private fun subscribeToStateChanges() {
        val connection = ApplicationManager.getApplication().messageBus.connect(this)

        connection.subscribe(VeilStateListener.TOPIC, object : VeilStateListener {
            override fun veilStateChanged() {
                refreshVeil()
            }
        })

        connection.subscribe(LafManagerListener.TOPIC, LafManagerListener { _: LafManager ->
            background = UIUtil.getPanelBackground()
            refreshVeil()
        })

        connection.subscribe(EditorColorsManager.TOPIC, EditorColorsListener { _: EditorColorsScheme? ->
            refreshVeil()
        })

        connection.subscribe(KeymapManagerListener.TOPIC, object : KeymapManagerListener {
            override fun activeKeymapChanged(keymap: Keymap?) {
                refreshVeil()
            }

            override fun shortcutChanged(keymap: Keymap, actionId: String) {
                refreshVeil()
            }
        })
    }

    private fun executeScript(script: String) {
        val cefBrowser = browser.cefBrowser
        cefBrowser.executeJavaScript(script, cefBrowser.url ?: "", 0)
    }

    private fun executeInAllFrames(script: String) {
        executeScript(script)

        val cefBrowser = browser.cefBrowser

        cefBrowser.frameNames.forEach { name ->
            val frame = cefBrowser.getFrameByName(name) ?: return@forEach

            frame.executeJavaScript(script, frame.url ?: "", 0)
        }
    }
}
