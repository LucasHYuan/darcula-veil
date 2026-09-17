package com.lucashyuan.darculaveil

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project
import java.awt.event.InputEvent
import javax.swing.KeyStroke

object VeilKeymapBridge {

    private val BRIDGED_ACTION_IDS = setOf(
        "DarculaVeil.PageForward",
        "DarculaVeil.PageBackward",
        "DarculaVeil.Toggle",
        "DarculaVeil.BossKey"
    )

    private const val EVENTFLAG_SHIFT_DOWN = 1 shl 1
    private const val EVENTFLAG_CONTROL_DOWN = 1 shl 2
    private const val EVENTFLAG_ALT_DOWN = 1 shl 3

    fun resolveActionId(windowsKeyCode: Int, cefModifiers: Int): String? {
        if (windowsKeyCode == 0) {
            return null
        }

        val keyStroke = KeyStroke.getKeyStroke(windowsKeyCode, toAwtModifiers(cefModifiers))
        val candidates = KeymapManager.getInstance().activeKeymap.getActionIds(keyStroke)

        return candidates.firstOrNull { BRIDGED_ACTION_IDS.contains(it) }
    }

    fun invoke(project: Project, actionId: String) {
        val action = ActionManager.getInstance().getAction(actionId) ?: return

        ApplicationManager.getApplication().invokeLater {
            ActionUtil.invokeAction(action, SimpleDataContext.getProjectContext(project), ActionPlaces.UNKNOWN, null, null)
        }
    }

    private fun toAwtModifiers(cefModifiers: Int): Int {
        var modifiers = 0

        if (cefModifiers and EVENTFLAG_SHIFT_DOWN != 0) {
            modifiers = modifiers or InputEvent.SHIFT_DOWN_MASK
        }

        if (cefModifiers and EVENTFLAG_CONTROL_DOWN != 0) {
            modifiers = modifiers or InputEvent.CTRL_DOWN_MASK
        }

        if (cefModifiers and EVENTFLAG_ALT_DOWN != 0) {
            modifiers = modifiers or InputEvent.ALT_DOWN_MASK
        }

        return modifiers
    }
}
