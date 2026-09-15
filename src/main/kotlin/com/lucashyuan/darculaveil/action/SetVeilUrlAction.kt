package com.lucashyuan.darculaveil.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.lucashyuan.darculaveil.VeilBrowserPanel
import com.lucashyuan.darculaveil.VeilSettings

class SetVeilUrlAction(private val panel: VeilBrowserPanel) : AnAction("Set URL", "Change the embedded page address", AllIcons.General.Settings) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val settings = VeilSettings.state()
        val input = Messages.showInputDialog(e.project, "URL:", "Darcula Veil", null, settings.homeUrl, null)

        if (input.isNullOrBlank()) {
            return
        }

        settings.homeUrl = input.trim()
        panel.loadUrl(settings.homeUrl)
    }
}
