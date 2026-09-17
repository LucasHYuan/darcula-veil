package com.lucashyuan.darculaveil

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.MouseShortcut
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

    private val PAGE_TURN_DIRECTIONS = mapOf("DarculaVeil.PageForward" to 1, "DarculaVeil.PageBackward" to -1)

    private const val LEGACY_SHIFT_MASK = 1
    private const val LEGACY_CTRL_MASK = 2
    private const val LEGACY_ALT_MASK = 8

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

    fun buildWheelBindingsLiteral(): String {
        val bindings = PAGE_TURN_DIRECTIONS.flatMap { entry -> wheelBindingsOf(entry.key, entry.value) }

        return bindings.joinToString(",", "[", "]")
    }

    private fun wheelBindingsOf(actionId: String, direction: Int): List<String> {
        val keymap = KeymapManager.getInstance().activeKeymap

        return keymap.getShortcuts(actionId)
            .filterIsInstance<MouseShortcut>()
            .filter { it.button == MouseShortcut.BUTTON_WHEEL_UP || it.button == MouseShortcut.BUTTON_WHEEL_DOWN }
            .map { shortcut -> toLiteral(shortcut, direction) }
    }

    private fun toLiteral(shortcut: MouseShortcut, direction: Int): String {
        val up = shortcut.button == MouseShortcut.BUTTON_WHEEL_UP
        val modifiers = shortcut.modifiers

        return "{up:$up,shift:${hasShift(modifiers)},ctrl:${hasControl(modifiers)},alt:${hasAlt(modifiers)},direction:$direction}"
    }

    private fun hasShift(modifiers: Int): Boolean = hasMask(modifiers, InputEvent.SHIFT_DOWN_MASK or LEGACY_SHIFT_MASK)

    private fun hasControl(modifiers: Int): Boolean = hasMask(modifiers, InputEvent.CTRL_DOWN_MASK or LEGACY_CTRL_MASK)

    private fun hasAlt(modifiers: Int): Boolean = hasMask(modifiers, InputEvent.ALT_DOWN_MASK or LEGACY_ALT_MASK)

    private fun hasMask(modifiers: Int, mask: Int): Boolean = modifiers and mask != 0

    fun describeWheelShortcuts(): String {
        return PAGE_TURN_DIRECTIONS.entries.joinToString("; ") { entry ->
            val shortcuts = KeymapManager.getInstance().activeKeymap.getShortcuts(entry.key)
            val rendered = shortcuts.joinToString(",") { shortcut ->
                if (shortcut is MouseShortcut) "mouse(button=${shortcut.button},mod=${shortcut.modifiers})" else shortcut.toString()
            }

            "${entry.key}=[$rendered]"
        }
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
