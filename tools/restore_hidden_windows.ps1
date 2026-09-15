param([int]$TargetPid)

$signature = @"
using System;
using System.Runtime.InteropServices;
using System.Text;

public class VeilRestore {
    public delegate bool EnumProc(IntPtr handle, IntPtr param);

    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc callback, IntPtr param);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr handle, out uint processId);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr handle, int command);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr handle);
    [DllImport("user32.dll")] public static extern IntPtr GetParent(IntPtr handle);
    [DllImport("user32.dll")] public static extern IntPtr SetParent(IntPtr child, IntPtr parent);
    [DllImport("user32.dll")] public static extern int GetWindowLong(IntPtr handle, int index);
    [DllImport("user32.dll")] public static extern int SetWindowLong(IntPtr handle, int index, int value);
    [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr handle, IntPtr after, int x, int y, int cx, int cy, uint flags);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowTextW(IntPtr handle, StringBuilder text, int count);
}
"@

Add-Type -TypeDefinition $signature

$collected = New-Object System.Collections.ArrayList

$callback = [VeilRestore+EnumProc] {
    param($handle, $param)
    $owner = 0
    [void][VeilRestore]::GetWindowThreadProcessId($handle, [ref]$owner)
    if ($owner -eq $TargetPid) {
        $builder = New-Object System.Text.StringBuilder 512
        [void][VeilRestore]::GetWindowTextW($handle, $builder, 512)
        [void]$collected.Add([pscustomobject]@{
            Handle  = $handle
            Visible = [VeilRestore]::IsWindowVisible($handle)
            Parent  = [VeilRestore]::GetParent($handle)
            Style   = "0x{0:X8}" -f [VeilRestore]::GetWindowLong($handle, -16)
            Title   = $builder.ToString()
        })
    }
    return $true
}

[void][VeilRestore]::EnumWindows($callback, [IntPtr]::Zero)

if ($collected.Count -eq 0) {
    Write-Output "no top level window found for pid $TargetPid"
    exit 0
}

$collected | Format-Table -AutoSize

$WS_CHILD = 0x40000000
$WS_POPUP = [int]0x80000000
$WS_CAPTION = 0x00C00000
$WS_THICKFRAME = 0x00040000
$WS_SYSMENU = 0x00080000
$SWP_FLAGS = 0x0004 -bor 0x0010 -bor 0x0020

foreach ($window in $collected) {
    if ($window.Parent -ne [IntPtr]::Zero) {
        [void][VeilRestore]::SetParent($window.Handle, [IntPtr]::Zero)
        Write-Output ("reparented to desktop -> " + $window.Handle)
    }

    $style = [VeilRestore]::GetWindowLong($window.Handle, -16)
    $restored = ($style -band (-bnot $WS_CHILD)) -bor $WS_POPUP -bor $WS_CAPTION -bor $WS_THICKFRAME -bor $WS_SYSMENU
    [void][VeilRestore]::SetWindowLong($window.Handle, -16, $restored)
    [void][VeilRestore]::SetWindowPos($window.Handle, [IntPtr]::Zero, 200, 200, 900, 640, $SWP_FLAGS)
    [void][VeilRestore]::ShowWindow($window.Handle, 5)
    Write-Output ("restored and shown -> " + $window.Handle + "  " + $window.Title)
}
