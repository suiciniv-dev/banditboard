param([int]$Parent)
$ErrorActionPreference = 'SilentlyContinue'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | Select-Object -First 1
[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]
$op = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()
$manager = $asTask.MakeGenericMethod([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]).Invoke($null, @($op)).Result
$last = ''
while ($true) {
    if ($Parent -and -not (Get-Process -Id $Parent -ErrorAction SilentlyContinue)) { exit 0 }
    $playing = '0'
    foreach ($s in $manager.GetSessions()) { if ($s.GetPlaybackInfo().PlaybackStatus -eq 'Playing') { $playing = '1' } }
    if ($playing -ne $last) { [Console]::Out.WriteLine($playing); [Console]::Out.Flush(); $last = $playing }
    Start-Sleep -Seconds 2
}
