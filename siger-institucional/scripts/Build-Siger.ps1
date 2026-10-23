#Requires -Version 7.4
$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '../front')
try {
    & npm.cmd ci
    if ($LASTEXITCODE -ne 0) { throw 'Falló npm ci institucional.' }
    foreach($step in @('lint','test','build')) {
        & npm.cmd run $step
        if ($LASTEXITCODE -ne 0) { throw "Falló frontend institucional: $step" }
    }
} finally { Pop-Location }
Push-Location (Join-Path $PSScriptRoot '../backend')
try {
    & mvn -B -ntp clean verify
    if ($LASTEXITCODE -ne 0) { throw 'La compilación institucional falló.' }
} finally { Pop-Location }
