#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr')
. "$PSScriptRoot\Common.ps1"
$p=Get-Content -Raw "$Root\mysql\platform.json"|ConvertFrom-Json
if($p.scope -ne 'SOLO_SMR' -or [IO.Path]::GetFullPath($p.config) -ne "$Root\mysql\my.ini"){throw 'Control exclusivo del motor SMR; no MySQL80'}
$listener=Get-NetTCPConnection -LocalPort $p.port -State Listen -ErrorAction SilentlyContinue
if($listener){$process=Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)";if(-not $process.CommandLine.Contains($p.config)){throw 'Puerto ocupado por proceso ajeno'};Write-Output 'MySQL SMR ya activo';return}
$process=Start-Process -FilePath "$($p.home)\bin\mysqld.exe" -ArgumentList "`"--defaults-file=$($p.config)`"" -WindowStyle Hidden -PassThru
$ready=$false;for($i=0;$i -lt 30;$i++){$listener=Get-NetTCPConnection -LocalPort $p.port -State Listen -ErrorAction SilentlyContinue;if($listener){$ready=$true;break};Start-Sleep -Milliseconds 500};if(-not $ready){throw 'MySQL SMR no disponible; ver log privado'}
$p.pid=$listener.OwningProcess;$p|ConvertTo-Json|Set-Content "$Root\mysql\platform.json";Write-Output "MySQL SMR disponible; PID $($p.pid)."
