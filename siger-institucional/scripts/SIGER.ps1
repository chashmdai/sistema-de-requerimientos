#Requires -Version 7.4
[CmdletBinding()]
param([ValidateSet('iniciar','detener','estado')][string]$Accion='iniciar', [switch]$Reconstruir,
      [switch]$DemoIntegracion,[switch]$RecepcionJdbc)
$ErrorActionPreference='Stop'
$appRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$privateRoot=[IO.Path]::GetFullPath((Join-Path $appRoot '../.local/institucional'))
$jar=Join-Path $appRoot 'backend/target/siger-institucional-0.1.0-SNAPSHOT.jar'
$statePath=Join-Path $privateRoot 'process.json'

function Stop-OwnedInstitutional {
    if (!(Test-Path -LiteralPath $statePath)) { return }
    $record=Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    $running=Get-Process -Id $record.Id -ErrorAction SilentlyContinue
    if ($running -and $running.StartTime.ToUniversalTime().Ticks -eq ([DateTimeOffset]$record.StartedUtc).UtcDateTime.Ticks) {
        $command=(Get-CimInstance Win32_Process -Filter ('ProcessId=' + $record.Id)).CommandLine
        if (!$command -or !$command.Contains($jar,[StringComparison]::OrdinalIgnoreCase)) { throw 'PID institucional no corresponde al JAR esperado.' }
        Stop-Process -Id $record.Id
        $running.WaitForExit(10000) | Out-Null
    }
    Remove-Item -LiteralPath $statePath
}

if ($Accion -eq 'estado') {
    try { Invoke-RestMethod -Uri 'http://127.0.0.1:8081/api/estado' -TimeoutSec 4 -NoProxy | Format-List }
    catch { Write-Host 'SIGER Institucional no está disponible en 127.0.0.1:8081.' }
    return
}
if ($Accion -eq 'detener') { Stop-OwnedInstitutional; Write-Host 'SIGER Institucional detenido.'; return }
Stop-OwnedInstitutional
if (Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue) { throw '8081 está ocupado; no se detendrá otro proceso.' }
if ($Reconstruir -or !(Test-Path -LiteralPath $jar)) { & (Join-Path $PSScriptRoot 'Build-Siger.ps1') }
New-Item -ItemType Directory -Path $privateRoot -Force | Out-Null
$previous=@{}
$isolated=@('SPRING_PROFILES_ACTIVE','SPRING_CONFIG_ADDITIONAL_LOCATION','SPRING_CONFIG_LOCATION','SPRING_APPLICATION_JSON',
 'SIGER_PULL_ENABLED','SIGER_INST_PORT','SIGER_INST_BIND_ADDRESS','SIGER_PUBLIC_SOURCE','SIGER_INTEGRATION_TOKEN','SIGER_DEMO_DIRECTORY')
$cuNames=@(Get-ChildItem Env: | Where-Object { $_.Name -like 'CU_*' -or $_.Name -like 'PUBLIC_*' } | ForEach-Object Name)
foreach ($name in @($isolated)+$cuNames) { $previous[$name]=[Environment]::GetEnvironmentVariable($name,'Process') }
$child=$null
try {
    foreach ($name in $cuNames) { [Environment]::SetEnvironmentVariable($name,$null,'Process') }
    $env:SPRING_CONFIG_ADDITIONAL_LOCATION=$null; $env:SPRING_CONFIG_LOCATION=$null; $env:SPRING_APPLICATION_JSON=$null
    $env:SIGER_INST_PORT='8081'; $env:SIGER_INST_BIND_ADDRESS='127.0.0.1'
    $env:SPRING_PROFILES_ACTIVE=$null; $env:SIGER_PULL_ENABLED='false'
    if ($DemoIntegracion) {
        if (!$env:SIGER_INTEGRATION_TOKEN -or $env:SIGER_INTEGRATION_TOKEN.Length -lt 32 -or !$env:SIGER_DEMO_DIRECTORY -or !$env:SIGER_PUBLIC_SOURCE) {
            throw 'Inicia la demostración con scripts/Demo-Integracion.ps1 -Interactiva; falta configuración externa.'
        }
        $env:SPRING_PROFILES_ACTIVE='local-integration-demo'; $env:SIGER_PULL_ENABLED='true'
    } elseif($RecepcionJdbc) {
        $env:SPRING_PROFILES_ACTIVE='jdbc-reception';$env:SIGER_PULL_ENABLED='false'
    } else { $env:SIGER_INTEGRATION_TOKEN=$null; $env:SIGER_PUBLIC_SOURCE=$null; $env:SIGER_DEMO_DIRECTORY=$null }
    $child=Start-Process -FilePath (Get-Command java).Source -ArgumentList @('-jar',('"'+$jar+'"')) -WorkingDirectory $appRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $privateRoot 'backend.log') -RedirectStandardError (Join-Path $privateRoot 'backend-error.log')
    @{Id=$child.Id;StartedUtc=$child.StartTime.ToUniversalTime().ToString('o')} | ConvertTo-Json | Set-Content -LiteralPath $statePath
    $ready=$false; $deadline=(Get-Date).AddSeconds(30)
    do {
        if ($child.HasExited) { throw 'SIGER Institucional terminó durante el arranque; revisa .local/institucional/backend.log.' }
        try { $status=Invoke-RestMethod -Uri 'http://127.0.0.1:8081/api/estado' -TimeoutSec 2 -NoProxy; $ready=$status.aplicacion -eq 'siger-institucional' } catch { }
        if (!$ready) { Start-Sleep -Milliseconds 400 }
    } while (!$ready -and (Get-Date) -lt $deadline)
    if (!$ready) { throw 'SIGER Institucional no respondió a tiempo.' }
    Write-Host ('SIGER Institucional listo en http://127.0.0.1:8081/api/estado; recuperación de demostración=' + $DemoIntegracion.IsPresent)
} catch {
    if ($child -and !$child.HasExited) { Stop-Process -Id $child.Id }
    if (Test-Path -LiteralPath $statePath) { Remove-Item -LiteralPath $statePath }
    throw
} finally { foreach ($name in $previous.Keys) { [Environment]::SetEnvironmentVariable($name,$previous[$name],'Process') } }
