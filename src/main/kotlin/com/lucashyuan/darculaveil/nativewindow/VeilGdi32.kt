package com.lucashyuan.darculaveil.nativewindow

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.WinDef.HBITMAP
import com.sun.jna.platform.win32.WinDef.HDC
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

@Structure.FieldOrder("x", "y")
class VeilPoint : Structure() {
    @JvmField var x: Int = 0
    @JvmField var y: Int = 0
}

@Structure.FieldOrder("cx", "cy")
class VeilSize : Structure() {
    @JvmField var cx: Int = 0
    @JvmField var cy: Int = 0
}

@Structure.FieldOrder("blendOp", "blendFlags", "sourceConstantAlpha", "alphaFormat")
class VeilBlendFunction : Structure() {
    @JvmField var blendOp: Byte = 0
    @JvmField var blendFlags: Byte = 0
    @JvmField var sourceConstantAlpha: Byte = 0
    @JvmField var alphaFormat: Byte = 0
}

@Structure.FieldOrder("biSize", "biWidth", "biHeight", "biPlanes", "biBitCount", "biCompression", "biSizeImage", "biXPelsPerMeter", "biYPelsPerMeter", "biClrUsed", "biClrImportant")
class VeilBitmapInfoHeader : Structure() {
    @JvmField var biSize: Int = 0
    @JvmField var biWidth: Int = 0
    @JvmField var biHeight: Int = 0
    @JvmField var biPlanes: Short = 0
    @JvmField var biBitCount: Short = 0
    @JvmField var biCompression: Int = 0
    @JvmField var biSizeImage: Int = 0
    @JvmField var biXPelsPerMeter: Int = 0
    @JvmField var biYPelsPerMeter: Int = 0
    @JvmField var biClrUsed: Int = 0
    @JvmField var biClrImportant: Int = 0
}

interface VeilGdi32 : StdCallLibrary {

    fun CreateCompatibleDC(reference: HDC?): HDC?

    fun CreateDIBSection(device: HDC?, header: VeilBitmapInfoHeader, usage: Int, bits: PointerByReference, section: Pointer?, offset: Int): HBITMAP?

    fun SelectObject(device: HDC, handle: HANDLE): HANDLE?

    fun DeleteObject(handle: HANDLE): Boolean

    fun DeleteDC(device: HDC): Boolean

    companion object {
        val INSTANCE: VeilGdi32 = Native.load("gdi32", VeilGdi32::class.java, W32APIOptions.DEFAULT_OPTIONS)

        const val BI_RGB = 0
        const val DIB_RGB_COLORS = 0
        const val BITMAP_INFO_HEADER_SIZE = 40
        const val BITS_PER_PIXEL = 32

        const val AC_SRC_OVER: Byte = 0
        const val AC_SRC_ALPHA: Byte = 1
        const val ULW_ALPHA = 0x00000002
    }
}
