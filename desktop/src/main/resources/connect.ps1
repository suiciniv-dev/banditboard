try {
__INSTALL__
} catch {
    $kinds = @($_.FullyQualifiedErrorId)
    $e = $_.Exception
    while ($e) { $kinds += $e.GetType().Name; $e = $e.InnerException }
    $report = ($kinds -join ' ') + "`n" + $_.Exception.Message + "`n`n" + ($_ | Out-String)
    [System.IO.File]::WriteAllText('__LOG__', $report, (New-Object System.Text.UTF8Encoding $false))
    exit 2
}
