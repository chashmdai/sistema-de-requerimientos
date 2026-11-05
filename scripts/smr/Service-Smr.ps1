#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr')
$ErrorActionPreference='Stop'
# Despachador estable: cada versión usa su launcher compatible y su Java25 propio.
$configuration=Get-Content -Raw -LiteralPath "$Root\instances\recepcion\instance.json"|ConvertFrom-Json
& (Join-Path $configuration.release 'scripts\Run-Smr.ps1') -Root $Root -Instance recepcion
