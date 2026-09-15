package com.lucashyuan.darculaveil.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.lucashyuan.darculaveil.VeilBrowserPanel
import com.lucashyuan.darculaveil.VeilToolWindowFactory

class BossKeyAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(VeilToolWindowFactory.TOOL_WINDOW_ID) ?: return

        if (toolWindow.isVisible) {
            findPanel(toolWindow)?.pauseMedia()
            toolWindow.hide(null)
            return
        }

        toolWindow.activate(null)
    }

    private fun findPanel(toolWindow: ToolWindow): VeilBrowserPanel? {
        return toolWindow.contentManager.contents.firstNotNullOfOrNull { it.component as? VeilBrowserPanel }
    }
}
