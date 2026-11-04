#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[ValidateSet('recepcion','sintetico')][string]$Instance='recepcion',[Parameter(Mandatory)][string]$Release)
. "$PSScriptRoot\Common.ps1"
$path="$Root\instances\$Instance\instance.json";$c=Get-Content -Raw $path|ConvertFrom-Json
if(Get-NetTCPConnection -LocalPort $c.port -State Listen -ErrorAction SilentlyContinue){throw 'Detener SOLO SMR con drenaje antes de actualizar/revertir'}
$m=Get-Content -Raw "$Release\manifest.json"|ConvertFrom-Json
if((Get-FileHash "$Release\smr-rc1.jar").Hash.ToLowerInvariant() -ne $m.jarSha256 -or (Get-FileHash "$Release\runtime\java25\bin\java.exe").Hash.ToLowerInvariant() -ne $m.javaExeSha256){throw 'Paquete inválido'}
@{atUtc=[DateTime]::UtcNow.ToString('o');previous=$c.release;next=[IO.Path]::GetFullPath($Release);scope='SOLO_CODIGO_Y_RUNTIME_SMR';databaseRestored=$false}|ConvertTo-Json -Compress|Add-Content -LiteralPath "$Root\instances\$Instance\release-history.jsonl"
$c.release=[IO.Path]::GetFullPath($Release);$c.javaHome=Join-Path $c.release 'runtime\java25';$c|ConvertTo-Json|Set-Content -LiteralPath $path
Write-Output 'Artefacto y Java portable seleccionados; datos/folios/ACK preservados. No se restauró una base anterior.'
