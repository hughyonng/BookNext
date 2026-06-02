param(
    [string]$Task = "assembleDebug"
)

$ErrorActionPreference = "Stop"
. 'D:\code\android-build\env.ps1'
Set-Location 'D:\code\BookNext'

Write-Host "Building: gradle $Task" -ForegroundColor Cyan
$result = gradle $Task 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "BUILD FAILED (exit code: $LASTEXITCODE)" -ForegroundColor Red
    exit $LASTEXITCODE
} else {
    Write-Host "BUILD SUCCESSFUL" -ForegroundColor Green
}
