package com.lucashyuan.darculaveil

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.UIUtil
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import javax.swing.JPanel

class VeilBrowserPanel : JPanel(BorderLayout()), Disposable {

    private val browser = JBCefBrowser(VeilSettings.state().homeUrl)

    init {
        Disposer.register(this, browser)
        background = UIUtil.getPanelBackground()
        add(browser.component, BorderLayout.CENTER)
        installLoadHandler()
        subscribeToStateChanges()
    }

    fun loadUrl(url: String) {
        browser.loadURL(url)
    }

    fun reload() {
        browser.cefBrowser.reload()
    }

    fun pauseMedia() {
        executeScript(VeilMediaScript.buildPauseScript())
    }

    fun refreshVeil() {
        val settings = VeilSettings.state()
        val script = if (settings.veilEnabled) VeilCss.buildInjectScript(VeilCss.buildCss(settings)) else VeilCss.buildRemoveScript()
        executeScript(script)
    }

    override fun dispose() {
    }

    private fun installLoadHandler() {
        val handler = object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(cefBrowser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                refreshVeil()
            }
        }

        browser.jbCefClient.addLoadHandler(handler, browser.cefBrowser)
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
    }

    private fun executeScript(script: String) {
        val cefBrowser = browser.cefBrowser
        cefBrowser.executeJavaScript(script, cefBrowser.url ?: "", 0)
    }
}
