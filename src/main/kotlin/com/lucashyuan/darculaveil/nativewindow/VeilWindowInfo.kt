package com.lucashyuan.darculaveil.nativewindow

import com.sun.jna.platform.win32.WinDef.HWND

class VeilWindowInfo(val handle: HWND, val title: String, val processId: Int) {

    override fun toString(): String = "$title   [pid $processId]"
}
