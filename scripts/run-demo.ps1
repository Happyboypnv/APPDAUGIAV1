<#
Run-demo PowerShell script

Usage:
  - Open PowerShell as Administrator (if needed)
  - Run: .\scripts\run-demo.ps1

What it does:
  1. Build the project (maven package)
  2. Start the ServerApp in a new PowerShell window (HTTP + WebSocket)
  3. Start the JavaFX client App in another new PowerShell window

Notes:
  - This script uses mvn exec:java to start the mains. It assumes Maven is on PATH.
  - If your environment requires custom JVM args for JavaFX, edit the $clientCmd variable.
  - If exec:java isn't configured or fails, you can instead run from your IDE.
#>

Push-Location "$PSScriptRoot/.."

Write-Host "Building project..." -ForegroundColor Cyan
mvn -DskipTests=true package

if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed. Fix compile errors before running demo."
    Pop-Location
    exit 1
}

$repoRoot = Get-Location

Write-Host "Starting ServerApp in a new window..." -ForegroundColor Green
$serverCmd = 'mvn -DskipTests=true exec:java -Dexec.mainClass="com.mycompany.server.ServerApp"'
Start-Process powershell -ArgumentList "-NoExit","-Command","cd '$repoRoot'; $serverCmd"

Start-Sleep -Seconds 3

Write-Host "Starting JavaFX client App in a new window..." -ForegroundColor Green
# If your JavaFX runtime requires module path / VM options adjust the command below
$clientCmd = 'mvn -DskipTests=true exec:java -Dexec.mainClass="com.mycompany.App"'
Start-Process powershell -ArgumentList "-NoExit","-Command","cd '$repoRoot'; $clientCmd"

Write-Host "Launched server and client. Check the two new PowerShell windows for logs." -ForegroundColor Yellow

Pop-Location

