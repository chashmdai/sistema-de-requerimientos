#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr')
. "$PSScriptRoot\Common.ps1"
$p=Get-Content -Raw "$Root\mysql\platform.json"|ConvertFrom-Json
if($p.scope -ne 'SOLO_SMR' -or [IO.Path]::GetFullPath($p.config) -ne "$Root\mysql\my.ini"){throw 'No intervenir otro motor'}
foreach($instance in @('sintetico','recepcion')){if(Test-Path "$Root\instances\$instance\instance.json"){$c=Get-Content -Raw "$Root\instances\$instance\instance.json"|ConvertFrom-Json;if(Get-NetTCPConnection -LocalPort $c.port -State Listen -ErrorAction SilentlyContinue){throw 'Drenar y detener ambas aplicaciones SMR antes de parar su motor'}}}
$listener=Get-NetTCPConnection -LocalPort $p.port -State Listen -ErrorAction SilentlyContinue;if(-not $listener){Write-Output 'MySQL SMR ya detenido';return}
$process=Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)";if(-not $process.CommandLine.Contains($p.config)){throw 'Proceso ajeno; no detener'}
$admin=Read-SmrJson "$Root\secrets\mysql-admin.dpapi";$cnf="$Root\secrets\mysql-shutdown.cnf"
@"
[client]
host=127.0.0.1
port=$($p.port)
user=$($admin.username)
password=$($admin.password)
ssl-mode=VERIFY_IDENTITY
ssl-ca=$($Root.Replace('\','/'))/secrets/tls/ca.pem
"@|Set-Content -LiteralPath $cnf
try{& "$($p.home)\bin\mysqladmin.exe" "--defaults-extra-file=$cnf" shutdown 2> "$Root\logs\mysql-shutdown.log";if($LASTEXITCODE){throw 'Parada MySQL SMR no acreditada'};Write-Output 'Solo MySQL SMR detenido mediante shutdown normal.'}finally{$admin.Clear();Remove-Item -LiteralPath $cnf}
