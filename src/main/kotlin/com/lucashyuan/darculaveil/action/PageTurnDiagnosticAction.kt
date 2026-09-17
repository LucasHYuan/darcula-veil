package com.lucashyuan.darculaveil.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.lucashyuan.darculaveil.VeilBrowserPanel

class PageTurnDiagnosticAction(private val panel: VeilBrowserPanel) : AnAction("Page Turn Diagnostics", "Overlay what the page turn script sees in every frame", AllIcons.Actions.Preview) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        panel.showPageTurnDiagnostics()
    }
}
