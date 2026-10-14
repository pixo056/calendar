param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$calendarRoot = Split-Path -Parent $PSScriptRoot
Push-Location $calendarRoot
try {
    if (-not $SkipBuild) {
        & (Join-Path $calendarRoot 'build.cmd')
        if ($LASTEXITCODE -ne 0) { throw 'Build failed; distribution was not created.' }
    }
    $calendarJar = Join-Path $calendarRoot 'target\calendar.jar'
    if (-not (Test-Path -LiteralPath $calendarJar)) { throw 'Build target/calendar.jar first.' }
    $calendarStage = Join-Path $calendarRoot ('target\distribution-work\' + [Guid]::NewGuid().ToString('N') + '\calendar')
    New-Item -ItemType Directory -Path (Join-Path $calendarStage 'scripts') -Force | Out-Null
    Copy-Item -LiteralPath $calendarJar -Destination (Join-Path $calendarStage 'calendar.jar')
    foreach ($calendarFile in @('run.cmd', 'run.sh', 'START-HERE.md')) {
        Copy-Item -LiteralPath (Join-Path $calendarRoot $calendarFile) -Destination $calendarStage
    }
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'java-env.cmd') -Destination (Join-Path $calendarStage 'scripts')
    $calendarArchive = Join-Path $calendarRoot 'target\calendar-0.1.0.zip'
    Compress-Archive -LiteralPath $calendarStage -DestinationPath $calendarArchive -Force
    $calendarHash = (Get-FileHash -LiteralPath $calendarArchive -Algorithm SHA256).Hash.ToLowerInvariant()
    Set-Content -LiteralPath ($calendarArchive + '.sha256') -Encoding ascii -Value ($calendarHash + '  calendar-0.1.0.zip')
    Write-Output $calendarArchive
    Write-Output ($calendarArchive + '.sha256')
} finally { Pop-Location }
