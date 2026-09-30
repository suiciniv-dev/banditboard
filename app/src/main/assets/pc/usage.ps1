param([switch]$Work)
$ErrorActionPreference = 'SilentlyContinue'
$targetsPath = [System.IO.Path]::Combine($HOME, '.claude', 'clawdboard-targets.json')
$tmp = [System.IO.Path]::GetTempPath()
$mark = [System.IO.Path]::Combine($tmp, 'clawdboard-uso.txt')
$seen = [System.IO.Path]::Combine($tmp, 'clawdboard-sessoes.txt')

if (-not $Work) {
    $hookInput = [Console]::In.ReadToEnd()
    try {
        $h = $hookInput | ConvertFrom-Json
        $model = ''
        if ($h.transcript_path -and [System.IO.File]::Exists($h.transcript_path)) {
            $fs = [System.IO.File]::Open($h.transcript_path, 'Open', 'Read', 'ReadWrite')
            $len = [int][Math]::Min($fs.Length, 262144)
            [void]$fs.Seek(-$len, 'End')
            $buf = New-Object byte[] $len
            [void]$fs.Read($buf, 0, $len)
            $fs.Close()
            $found = [regex]::Matches([System.Text.Encoding]::UTF8.GetString($buf), '"model"\s*:\s*"claude-([a-z]+)')
            if ($found.Count -gt 0) { $model = $found[$found.Count - 1].Groups[1].Value }
        }
        if ($h.session_id) {
            $now = [DateTimeOffset]::Now.ToUnixTimeSeconds()
            $lines = @()
            if ([System.IO.File]::Exists($seen)) { $lines = [System.IO.File]::ReadAllLines($seen) }
            $keep = @($lines | Where-Object { $f = $_.Split("`t"); $f.Count -eq 3 -and $f[0] -ne $h.session_id -and ($now - [long]$f[2]) -lt 900 })
            $keep += "$($h.session_id)`t$model`t$now"
            [System.IO.File]::WriteAllLines($seen, [string[]]$keep)
        }
    } catch {
    }
    if ([System.IO.File]::Exists($mark) -and [System.IO.File]::GetLastWriteTime($mark) -gt [DateTime]::Now.AddSeconds(-120)) { exit 0 }
    [System.IO.File]::WriteAllText($mark, [DateTime]::Now.ToString('o'))
    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = 'powershell.exe'
    $psi.Arguments = '-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "' + $PSCommandPath + '" -Work'
    $psi.UseShellExecute = $true
    $psi.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Hidden
    [void][System.Diagnostics.Process]::Start($psi)
    exit 0
}

$claude = $null
foreach ($dir in ($env:PATH -split ';')) {
    if (-not $dir) { continue }
    $candidate = [System.IO.Path]::Combine($dir.Trim('"'), 'claude.exe')
    if ([System.IO.File]::Exists($candidate)) { $claude = $candidate; break }
}
if (-not $claude) {
    $ext = [System.IO.Path]::Combine($HOME, '.vscode', 'extensions')
    if ([System.IO.Directory]::Exists($ext)) {
        $claude = [System.IO.Directory]::GetDirectories($ext, 'anthropic.claude-code-*') |
            ForEach-Object { [System.IO.Path]::Combine($_, 'resources', 'native-binary', 'claude.exe') } |
            Where-Object { [System.IO.File]::Exists($_) } |
            Sort-Object { [System.IO.File]::GetLastWriteTime($_) } -Descending |
            Select-Object -First 1
    }
}
if (-not $claude) { exit 0 }

$psi = [System.Diagnostics.ProcessStartInfo]::new()
$psi.FileName = $claude
$psi.Arguments = '-p "/usage" --output-format json --no-session-persistence --settings "{\"disableAllHooks\":true}"'
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.StandardOutputEncoding = [System.Text.Encoding]::UTF8
$psi.CreateNoWindow = $true
$psi.WorkingDirectory = $tmp
$p = [System.Diagnostics.Process]::Start($psi)
$errors = $p.StandardError.ReadToEndAsync()
$out = $p.StandardOutput.ReadToEnd()
[void]$p.WaitForExit(60000)
$text = ($out | ConvertFrom-Json).result
if (-not $text) { exit 0 }

$culture = [System.Globalization.CultureInfo]::GetCultureInfo('en-US')
function Get-ResetEpoch([string]$s) {
    $s = $s.Trim()
    $now = [DateTime]::Now
    if ($s -match '^in\s') {
        $at = $now
        if ($s -match '(\d+)\s*d') { $at = $at.AddDays([int]$Matches[1]) }
        if ($s -match '(\d+)\s*h') { $at = $at.AddHours([int]$Matches[1]) }
        if ($s -match '(\d+)\s*m') { $at = $at.AddMinutes([int]$Matches[1]) }
        return [DateTimeOffset]::new($at).ToUnixTimeSeconds()
    }
    foreach ($format in 'MMM d, h:mmtt', 'MMM d, htt', 'MMM d, h:mm tt', 'MMM d, h tt', 'h:mmtt', 'htt') {
        $d = [DateTime]::MinValue
        if ([DateTime]::TryParseExact($s, $format, $culture, [System.Globalization.DateTimeStyles]::AllowWhiteSpaces, [ref]$d)) {
            if ($d -lt $now.AddHours(-1)) { $d = if ($format -like 'MMM*') { $d.AddYears(1) } else { $d.AddDays(1) } }
            return [DateTimeOffset]::new($d).ToUnixTimeSeconds()
        }
    }
    return $null
}
function New-Window([string]$pct, [string]$reset) {
    $w = [ordered]@{ used_percentage = [double]::Parse($pct, $culture) }
    $epoch = Get-ResetEpoch $reset
    if ($epoch) { $w.resets_at = $epoch }
    return $w
}

$payload = [ordered]@{}
$m = [regex]::Match($text, 'Current session:\s*([0-9.]+)%\s*used\W+resets\s+([^(\r\n]+)')
if ($m.Success) { $payload.five_hour = New-Window $m.Groups[1].Value $m.Groups[2].Value }
$m = [regex]::Match($text, 'Current week \(all models\):\s*([0-9.]+)%\s*used\W+resets\s+([^(\r\n]+)')
if ($m.Success) { $payload.seven_day = New-Window $m.Groups[1].Value $m.Groups[2].Value }
$scoped = @()
foreach ($m in [regex]::Matches($text, 'Current week \((?!all models)([^)]+)\):\s*([0-9.]+)%\s*used\W+resets\s+([^(\r\n]+)')) {
    $w = New-Window $m.Groups[2].Value $m.Groups[3].Value
    $w.label = $m.Groups[1].Value.Trim()
    $scoped += $w
}
if ($scoped.Count -gt 0) { $payload.scoped = $scoped }
if ($payload.Count -eq 0) { exit 0 }
$active = @()
try {
    $now = [DateTimeOffset]::Now.ToUnixTimeSeconds()
    foreach ($l in [System.IO.File]::ReadAllLines($seen)) {
        $f = $l.Split("`t")
        if ($f.Count -eq 3 -and ($now - [long]$f[2]) -lt 600) { $active += $f[1] }
    }
} catch {
}
if ($active.Count -gt 0) { $payload.sessions = [string[]]$active }

$targets = @()
try { $targets = @([System.IO.File]::ReadAllText($targetsPath) | ConvertFrom-Json | ForEach-Object { $_ }) } catch { exit 0 }
$json = $payload | ConvertTo-Json -Compress -Depth 5
$bytes = [System.Text.Encoding]::UTF8.GetBytes($json)

function Get-Level($w) {
    $level = 0
    if ($w) { foreach ($l in 80, 90, 100) { if ($w.used_percentage -ge $l - 0.5) { $level = $l } } }
    return $level
}
function Protect-Usage([string]$text, [string]$hex) {
    $master = New-Object byte[] ($hex.Length / 2)
    for ($i = 0; $i -lt $master.Length; $i++) { $master[$i] = [Convert]::ToByte($hex.Substring($i * 2, 2), 16) }
    $derive = New-Object System.Security.Cryptography.HMACSHA256 (, $master)
    $aes = [System.Security.Cryptography.Aes]::Create()
    $aes.Mode = [System.Security.Cryptography.CipherMode]::CBC
    $aes.Padding = [System.Security.Cryptography.PaddingMode]::PKCS7
    $aes.Key = $derive.ComputeHash([System.Text.Encoding]::UTF8.GetBytes('banditboard-enc'))
    $aes.GenerateIV()
    $plain = [System.Text.Encoding]::UTF8.GetBytes($text)
    $sealed = [byte[]]($aes.IV + $aes.CreateEncryptor().TransformFinalBlock($plain, 0, $plain.Length))
    $mac = New-Object System.Security.Cryptography.HMACSHA256 (, $derive.ComputeHash([System.Text.Encoding]::UTF8.GetBytes('banditboard-mac')))
    return [Convert]::ToBase64String([byte[]]($sealed + $mac.ComputeHash($sealed)))
}
$level = "$(Get-Level $payload.five_hour),$(Get-Level $payload.seven_day)"
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor [System.Net.SecurityProtocolType]::Tls12

foreach ($t in $targets) {
    try {
        $body = $bytes
        $path = '/api/push'
        if ($t.enc) {
            $body = [System.Text.Encoding]::UTF8.GetBytes('{"blob":"' + (Protect-Usage $json $t.enc) + '","lvl":"' + $level + '"}')
            $path = '/push'
        }
        $req = [System.Net.HttpWebRequest]::Create("$($t.url)$path")
        $req.Method = 'POST'
        if (-not $t.enc) { $req.Proxy = $null }
        $req.Timeout = 5000
        $req.ContentType = 'application/json'
        $req.Headers.Add('X-Clawdboard', '1')
        $req.Headers.Add('X-Clawdboard-Key', $t.key)
        $req.ContentLength = $body.Length
        $stream = $req.GetRequestStream()
        $stream.Write($body, 0, $body.Length)
        $stream.Close()
        $req.GetResponse().Close()
    } catch {
    }
}
