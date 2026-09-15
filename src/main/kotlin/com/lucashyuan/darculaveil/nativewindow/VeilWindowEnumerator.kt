package com.lucashyuan.darculaveil.nativewindow

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference

object VeilWindowEnumerator {

    private const val TITLE_BUFFER_LENGTH = 512

    fun listCandidates(): List<VeilWindowInfo> {
        val user32 = VeilUser32.INSTANCE
        val ownProcessId = ProcessHandle.current().pid().toInt()
        val collected = mutableListOf<VeilWindowInfo>()

        user32.EnumWindows(object : WinUser.WNDENUMPROC {
            override fun callback(handle: HWND, data: Pointer?): Boolean {
                collect(user32, handle, ownProcessId, collected)

                return true
            }
        }, null)

        return collected.sortedBy { it.title.lowercase() }
    }

    private fun collect(user32: VeilUser32, handle: HWND, ownProcessId: Int, collected: MutableList<VeilWindowInfo>) {
        if (!user32.IsWindowVisible(handle)) {
            return
        }

        if (user32.GetParent(handle) != null) {
            return
        }

        val title = readTitle(user32, handle)

        if (title.isBlank()) {
            return
        }

        val processIdHolder = IntByReference()
        user32.GetWindowThreadProcessId(handle, processIdHolder)

        if (processIdHolder.value == ownProcessId) {
            return
        }

        collected.add(VeilWindowInfo(handle, title, processIdHolder.value))
    }

    private fun readTitle(user32: VeilUser32, handle: HWND): String {
        val buffer = CharArray(TITLE_BUFFER_LENGTH)
        val length = user32.GetWindowText(handle, buffer, TITLE_BUFFER_LENGTH)

        if (length <= 0) {
            return ""
        }

        return String(buffer, 0, length)
    }
}
