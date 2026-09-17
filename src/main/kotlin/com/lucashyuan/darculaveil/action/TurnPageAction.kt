package com.lucashyuan.darculaveil.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager
import com.lucashyuan.darculaveil.VeilBrowserPanel
import com.lucashyuan.darculaveil.VeilToolWindowFactory

abstract class TurnPageAction(private val direction: Int) : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(VeilToolWindowFactory.TOOL_WINDOW_ID) ?: return
        val panel = toolWindow.contentManager.contents.firstNotNullOfOrNull { it.component as? VeilBrowserPanel } ?: return

        panel.turnPage(direction)
    }
}

class PageForwardAction : TurnPageAction(1)

class PageBackwardAction : TurnPageAction(-1)
