#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[ValidateSet('recepcion','sintetico')][string]$Instance='recepcion',[switch]$DrainOnly)
. "$PSScriptRoot\Session-Smr.ps1"
$base=Join-Path $Root "instances\$Instance";$run=Get-Content -Raw "$base\run.json"|ConvertFrom-Json
$process=Get-CimInstance Win32_Process -Filter "ProcessId=$($run.pid)" -ErrorAction SilentlyContinue
if(-not $process){Write-Output 'La instancia SMR ya está detenida';return}
$config=Get-Content -Raw "$base\instance.json"|ConvertFrom-Json
if($process.ExecutablePath -ne (Join-Path $config.javaHome 'bin\java.exe') -or -not $process.CommandLine.Contains($run.jar) -or -not $process.CommandLine.Contains('--smr')){throw 'PID ajeno o cambiado: no detener'}
$session=New-SmrSession $Root $Instance;$state=Suspend-SmrConsumer $session
@{atUtc=[DateTime]::UtcNow.ToString('o');paused=$true;inFlight=$state.enCurso;ackPending=$state.ackPendientes;pid=$run.pid}|ConvertTo-Json|Set-Content -LiteralPath "$base\drain.json"
Invoke-SmrPost $session '/nucleo/logout' $null|Out-Null
if(-not $DrainOnly){Stop-Process -Id $run.pid;Write-Output "Proceso SMR $Instance detenido después del drenaje; ACK pendientes conservados."}else{Write-Output 'Consumidor pausado y drenado'}
