param([long]$Handle)

$signature = @"
using System;
using System.Runtime.InteropServices;
using System.Text;

[StructLayout(LayoutKind.Sequential)]
public struct VeilRect {
    public int Left;
    public int Top;
    public int Right;
    public int Bottom;
}

public class VeilChain {
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr handle, out uint processId);
    [DllImport("user32.dll")] public static extern bool IsWindow(IntPtr handle);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr handle);
    [DllImport("user32.dll")] public static extern IntPtr GetParent(IntPtr handle);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr handle, out VeilRect rect);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetClassNameW(IntPtr handle, StringBuilder text, int count);
}
"@

Add-Type -TypeDefinition $signature

$current = [IntPtr]$Handle
$level = 0

while ($current -ne [IntPtr]::Zero -and $level -lt 6) {
    if (-not [VeilChain]::IsWindow($current)) {
        Write-Output ("level " + $level + ": handle " + $current + " is no longer a window")
        break
    }

    $owner = 0
    [void][VeilChain]::GetWindowThreadProcessId($current, [ref]$owner)
    $class = New-Object System.Text.StringBuilder 256
    [void][VeilChain]::GetClassNameW($current, $class, 256)

    $rect = New-Object VeilRect
    [void][VeilChain]::GetWindowRect($current, [ref]$rect)
    $width = $rect.Right - $rect.Left
    $height = $rect.Bottom - $rect.Top

    Write-Output ("level " + $level + ": " + $class.ToString() + " pid=" + $owner + " visible=" + [VeilChain]::IsWindowVisible($current) + " size=" + $width + "x" + $height + " at=" + $rect.Left + "," + $rect.Top)

    $current = [VeilChain]::GetParent($current)
    $level = $level + 1
}
