package com.lucashyuan.darculaveil.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.lucashyuan.darculaveil.VeilBrowserPanel
import javax.swing.Icon

abstract class NavigateVeilPageAction(text: String, icon: Icon, protected val panel: VeilBrowserPanel) : AnAction(text, text, icon) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = isAvailable()
    }

    protected abstract fun isAvailable(): Boolean
}

class GoBackVeilPageAction(panel: VeilBrowserPanel) : NavigateVeilPageAction("Back", AllIcons.Actions.Back, panel) {

    override fun isAvailable(): Boolean = panel.canGoBack()

    override fun actionPerformed(e: AnActionEvent) {
        panel.goBack()
    }
}

class GoForwardVeilPageAction(panel: VeilBrowserPanel) : NavigateVeilPageAction("Forward", AllIcons.Actions.Forward, panel) {

    override fun isAvailable(): Boolean = panel.canGoForward()

    override fun actionPerformed(e: AnActionEvent) {
        panel.goForward()
    }
}
