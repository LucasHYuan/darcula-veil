package com.lucashyuan.darculaveil.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.lucashyuan.darculaveil.VeilBrowserPanel

class ReloadVeilPageAction(private val panel: VeilBrowserPanel) : AnAction("Reload", "Reload the embedded page", AllIcons.Actions.Refresh) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        panel.reload()
    }
}
