package com.lucashyuan.darculaveil.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import com.lucashyuan.darculaveil.VeilSettings
import com.lucashyuan.darculaveil.VeilStateListener

class ToggleVeilAction : ToggleAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun isSelected(e: AnActionEvent): Boolean = VeilSettings.state().veilEnabled

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        VeilSettings.state().veilEnabled = state
        ApplicationManager.getApplication().messageBus.syncPublisher(VeilStateListener.TOPIC).veilStateChanged()
    }
}
