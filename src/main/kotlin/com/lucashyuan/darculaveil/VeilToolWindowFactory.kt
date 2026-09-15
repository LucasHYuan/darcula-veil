package com.lucashyuan.darculaveil

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.lucashyuan.darculaveil.action.GoBackVeilPageAction
import com.lucashyuan.darculaveil.action.GoForwardVeilPageAction
import com.lucashyuan.darculaveil.action.ReloadVeilPageAction
import com.lucashyuan.darculaveil.action.SetVeilUrlAction
import javax.swing.JComponent
import javax.swing.SwingConstants

class VeilToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = if (JBCefApp.isSupported()) VeilBrowserPanel() else null
        val component: JComponent = panel ?: unsupportedPlaceholder()
        val content = ContentFactory.getInstance().createContent(component, "", false)

        if (panel != null) {
            Disposer.register(content, panel)
            toolWindow.setTitleActions(buildTitleActions(panel))
        }

        toolWindow.contentManager.addContent(content)
    }

    private fun buildTitleActions(panel: VeilBrowserPanel): List<AnAction> {
        val actions = mutableListOf<AnAction>()
        val toggle = ActionManager.getInstance().getAction(TOGGLE_ACTION_ID)

        if (toggle != null) {
            actions.add(toggle)
        }

        actions.add(GoBackVeilPageAction(panel))
        actions.add(GoForwardVeilPageAction(panel))
        actions.add(ReloadVeilPageAction(panel))
        actions.add(SetVeilUrlAction(panel))

        return actions
    }

    private fun unsupportedPlaceholder(): JComponent {
        return JBLabel("JCEF is not available in this IDE build.", SwingConstants.CENTER)
    }

    companion object {
        const val TOOL_WINDOW_ID = "Darcula Veil"

        private const val TOGGLE_ACTION_ID = "DarculaVeil.Toggle"
    }
}
