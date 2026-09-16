package com.lucashyuan.darculaveil.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.lucashyuan.darculaveil.VeilBrowserPanel

class MediaSupportReportAction(private val panel: VeilBrowserPanel) : AnAction("Media Support Report", "Probe which audio and video codecs this JCEF build can decode", AllIcons.General.Information) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        panel.showMediaReport()
    }
}
