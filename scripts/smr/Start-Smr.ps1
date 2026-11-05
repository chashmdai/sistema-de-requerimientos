#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[ValidateSet('recepcion','sintetico')][string]$Instance='recepcion')
. "$PSScriptRoot\Common.ps1"
$c=Get-Content -Raw "$Root\instances\$Instance\instance.json"|ConvertFrom-Json
if(Get-NetTCPConnection -LocalPort $c.port -State Listen -ErrorAction SilentlyContinue){throw 'Listener existente: inspeccionar antes de iniciar'}
$pwsh=Join-Path $c.release 'runtime\pwsh\pwsh.exe';if(-not(Test-Path -LiteralPath $pwsh)){$pwsh=(Get-Process -Id $PID).Path};$script=Join-Path $c.release 'scripts\Run-Smr.ps1'
# WMI crea un launcher independiente de la vida de la terminal/RDP. No es un servicio instalado.
$startup=New-CimInstance -ClassName Win32_ProcessStartup -ClientOnly -Property @{ShowWindow=[uint16]0}
$command="`"$pwsh`" -NoProfile -File `"$script`" -Root `"$Root`" -Instance $Instance"
$result=Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{CommandLine=$command;ProcessStartupInformation=$startup}
if($result.ReturnValue){throw "Launcher SMR rechazado: $($result.ReturnValue)"}
Write-Output "Launcher SMR independiente creado: PID $($result.ProcessId). URL https://localhost:$($c.port)/"
