package com.lucashyuan.darculaveil.nativewindow

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

interface VeilUser32 : StdCallLibrary {

    fun EnumWindows(callback: WinUser.WNDENUMPROC, data: Pointer?): Boolean

    fun GetWindowText(hWnd: HWND, buffer: CharArray, maxCount: Int): Int

    fun IsWindowVisible(hWnd: HWND): Boolean

    fun IsWindow(hWnd: HWND?): Boolean

    fun GetWindowThreadProcessId(hWnd: HWND, processId: IntByReference?): Int

    fun GetParent(hWnd: HWND): HWND?

    fun SetParent(child: HWND, parent: HWND?): HWND?

    fun GetWindowLong(hWnd: HWND, index: Int): Int

    fun SetWindowLong(hWnd: HWND, index: Int, value: Int): Int

    fun SetWindowPos(hWnd: HWND, insertAfter: HWND?, x: Int, y: Int, cx: Int, cy: Int, flags: Int): Boolean

    fun GetClientRect(hWnd: HWND, rect: RECT): Boolean

    fun ShowWindow(hWnd: HWND, command: Int): Boolean

    fun AttachThreadInput(attachFrom: Int, attachTo: Int, attach: Boolean): Boolean

    fun SetFocus(hWnd: HWND?): HWND?

    fun SetLayeredWindowAttributes(hWnd: HWND, colorKey: Int, alpha: Byte, flags: Int): Boolean

    companion object {
        val INSTANCE: VeilUser32 = Native.load("user32", VeilUser32::class.java, W32APIOptions.DEFAULT_OPTIONS)

        const val GWL_STYLE = -16
        const val GWL_EXSTYLE = -20

        const val WS_CHILD = 0x40000000
        const val WS_CAPTION = 0x00C00000
        const val WS_THICKFRAME = 0x00040000
        const val WS_MINIMIZEBOX = 0x00020000
        const val WS_MAXIMIZEBOX = 0x00010000
        const val WS_SYSMENU = 0x00080000

        const val WS_EX_APPWINDOW = 0x00040000
        const val WS_EX_LAYERED = 0x00080000
        const val WS_EX_TOOLWINDOW = 0x00000080

        const val SWP_NOZORDER = 0x0004
        const val SWP_NOACTIVATE = 0x0010
        const val SWP_FRAMECHANGED = 0x0020
        const val SWP_SHOWWINDOW = 0x0040

        const val SW_SHOW = 5
        const val LWA_ALPHA = 0x00000002

        val WS_POPUP: Int = 0x80000000.toInt()
    }
}
