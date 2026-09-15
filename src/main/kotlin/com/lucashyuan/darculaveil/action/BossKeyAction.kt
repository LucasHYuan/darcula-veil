package com.lucashyuan.darculaveil.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.lucashyuan.darculaveil.VeilBrowserPanel
import com.lucashyuan.darculaveil.VeilToolWindowFactory
import com.lucashyuan.darculaveil.nativewindow.VeilNativePanel
import com.lucashyuan.darculaveil.nativewindow.VeilNativeToolWindowFactory

class BossKeyAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val windows = collectWindows(project)

        if (windows.isEmpty()) {
            return
        }

        val visible = windows.filter { it.isVisible }

        if (visible.isNotEmpty()) {
            project.putUserData(CONCEALED_IDS, visible.map { it.id })
            visible.forEach { conceal(it) }
            return
        }

        val remembered = project.getUserData(CONCEALED_IDS).orEmpty()
        val restored = if (remembered.isEmpty()) windows else windows.filter { remembered.contains(it.id) }
        restored.forEach { reveal(it) }
    }

    private fun collectWindows(project: Project): List<ToolWindow> {
        val manager = ToolWindowManager.getInstance(project)

        return listOfNotNull(manager.getToolWindow(VeilToolWindowFactory.TOOL_WINDOW_ID), manager.getToolWindow(VeilNativeToolWindowFactory.TOOL_WINDOW_ID))
    }

    private fun conceal(toolWindow: ToolWindow) {
        forEachPanel(toolWindow) { component ->
            when (component) {
                is VeilBrowserPanel -> component.pauseMedia()
                is VeilNativePanel -> component.conceal()
            }
        }

        toolWindow.hide(null)
    }

    private fun reveal(toolWindow: ToolWindow) {
        toolWindow.activate(null)

        forEachPanel(toolWindow) { component ->
            if (component is VeilNativePanel) {
                component.reveal()
            }
        }
    }

    private fun forEachPanel(toolWindow: ToolWindow, action: (Any) -> Unit) {
        toolWindow.contentManager.contents.forEach { content -> action(content.component) }
    }

    companion object {
        private val CONCEALED_IDS: Key<List<String>> = Key.create("darcula.veil.concealed.tool.windows")
    }
}
