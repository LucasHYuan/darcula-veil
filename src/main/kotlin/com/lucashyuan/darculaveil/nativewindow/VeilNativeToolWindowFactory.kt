package com.lucashyuan.darculaveil.nativewindow

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.lucashyuan.darculaveil.action.DetachNativeWindowAction
import com.lucashyuan.darculaveil.action.SelectNativeWindowAction
import com.lucashyuan.darculaveil.action.SyncNativeWindowAction
import javax.swing.JComponent
import javax.swing.SwingConstants

class VeilNativeToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = if (SystemInfo.isWindows) VeilNativePanel() else null
        val component: JComponent = panel ?: unsupportedPlaceholder()
        val content = ContentFactory.getInstance().createContent(component, "", false)

        if (panel != null) {
            Disposer.register(content, panel)
            toolWindow.setTitleActions(buildTitleActions(panel))
        }

        toolWindow.contentManager.addContent(content)
    }

    private fun buildTitleActions(panel: VeilNativePanel): List<AnAction> {
        return listOf(SelectNativeWindowAction(panel), SyncNativeWindowAction(panel), DetachNativeWindowAction(panel))
    }

    private fun unsupportedPlaceholder(): JComponent {
        return JBLabel("Native window embedding is only implemented for Windows.", SwingConstants.CENTER)
    }

    companion object {
        const val TOOL_WINDOW_ID = "Darcula Veil Window"
    }
}
