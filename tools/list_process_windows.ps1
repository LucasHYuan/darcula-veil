param([int]$TargetPid)

$signature = @"
using System;
using System.Runtime.InteropServices;
using System.Text;

public class VeilProbe {
    public delegate bool EnumProc(IntPtr handle, IntPtr param);

    [DllImport("user32.dll")] public static extern bool EnumChildWindows(IntPtr parent, EnumProc callback, IntPtr param);
    [DllImport("user32.dll")] public static extern IntPtr GetDesktopWindow();
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr handle, out uint processId);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr handle);
    [DllImport("user32.dll")] public static extern IntPtr GetParent(IntPtr handle);
    [DllImport("user32.dll")] public static extern int GetWindowLong(IntPtr handle, int index);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowTextW(IntPtr handle, StringBuilder text, int count);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetClassNameW(IntPtr handle, StringBuilder text, int count);
}
"@

Add-Type -TypeDefinition $signature

$collected = New-Object System.Collections.ArrayList

$callback = [VeilProbe+EnumProc] {
    param($handle, $param)
    $owner = 0
    [void][VeilProbe]::GetWindowThreadProcessId($handle, [ref]$owner)
    if ($owner -eq $TargetPid) {
        $title = New-Object System.Text.StringBuilder 512
        $class = New-Object System.Text.StringBuilder 256
        [void][VeilProbe]::GetWindowTextW($handle, $title, 512)
        [void][VeilProbe]::GetClassNameW($handle, $class, 256)
        [void]$collected.Add([pscustomobject]@{
            Handle  = $handle
            Class   = $class.ToString()
            Visible = [VeilProbe]::IsWindowVisible($handle)
            Parent  = [VeilProbe]::GetParent($handle)
            Style   = "0x{0:X8}" -f [VeilProbe]::GetWindowLong($handle, -16)
            Title   = $title.ToString()
        })
    }
    return $true
}

[void][VeilProbe]::EnumChildWindows([VeilProbe]::GetDesktopWindow(), $callback, [IntPtr]::Zero)

Write-Output ("windows owned by pid " + $TargetPid + ": " + $collected.Count)
$collected | Format-Table -AutoSize
