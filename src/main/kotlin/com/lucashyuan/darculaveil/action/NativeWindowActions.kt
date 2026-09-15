package com.lucashyuan.darculaveil.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.lucashyuan.darculaveil.nativewindow.VeilNativePanel
import com.lucashyuan.darculaveil.nativewindow.VeilWindowEnumerator
import com.lucashyuan.darculaveil.nativewindow.VeilWindowInfo

class SelectNativeWindowAction(private val panel: VeilNativePanel) : AnAction("Pick Window", "Choose a top level window to embed", AllIcons.General.Settings) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val candidates = VeilWindowEnumerator.listCandidates()

        if (candidates.isEmpty()) {
            Messages.showInfoMessage(e.project, "No embeddable top level window was found.", "Darcula Veil")
            return
        }

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(candidates)
            .setTitle("Embed Window")
            .setItemChosenCallback { chosen: VeilWindowInfo -> panel.embed(chosen) }
            .createPopup()
            .showInFocusCenter()
    }
}

class ReembedNativeWindowAction(private val panel: VeilNativePanel) : AnAction("Re-embed Last Window", "Embed the window that was detached last", AllIcons.Actions.Rerun) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = panel.canReembed()
        e.presentation.text = if (panel.canReembed()) "Re-embed ${panel.lastTargetTitle()}" else "Re-embed Last Window"
    }

    override fun actionPerformed(e: AnActionEvent) {
        panel.reembedLast()
    }
}

class DetachNativeWindowAction(private val panel: VeilNativePanel) : AnAction("Detach", "Restore the embedded window to its own top level frame", AllIcons.Actions.Cancel) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = panel.hasEmbeddedWindow()
    }

    override fun actionPerformed(e: AnActionEvent) {
        panel.detach()
    }
}

class SyncNativeWindowAction(private val panel: VeilNativePanel) : AnAction("Resync Geometry", "Force the embedded window back to the panel bounds", AllIcons.Actions.Refresh) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = panel.hasEmbeddedWindow()
    }

    override fun actionPerformed(e: AnActionEvent) {
        panel.syncGeometry()
        panel.applyOpacity()
    }
}
