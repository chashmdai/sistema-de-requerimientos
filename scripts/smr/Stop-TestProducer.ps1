#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr')
. "$PSScriptRoot\Session-Smr.ps1"
$configuration=Get-Content -Raw "$Root\instances\sintetico\instance.json"|ConvertFrom-Json
if(-not $configuration.synthetic -or $configuration.origin -ne 'https://localhost:19443'){throw 'Solo productor local sintético conocido'}
$session=New-SmrSession $Root 'sintetico';$null=Suspend-SmrConsumer $session
$listener=Get-NetTCPConnection -LocalPort 19443 -State Listen -ErrorAction SilentlyContinue;if(-not $listener){Write-Output 'Productor sintético ya detenido';return}
$process=Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
if(-not $process.ExecutablePath.StartsWith("$Root\releases\",[StringComparison]::OrdinalIgnoreCase) -or -not $process.CommandLine.Contains('LocalProducer.java')){throw 'Proceso ajeno; no detener'}
Stop-Process -Id $listener.OwningProcess;Write-Output 'Solo productor sintético SMR detenido; entregas/ACK de prueba conservados.'
