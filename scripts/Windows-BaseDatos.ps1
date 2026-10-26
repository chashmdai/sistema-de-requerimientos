param([ValidateSet('inspect','probe-ddl','probe-view','plan','migrate','validate','fingerprint','audit')][string]$Mode='inspect',[ValidateSet('app','migrator')][string]$Identity='app')
$ErrorActionPreference='Stop'
if (-not $IsWindows) { throw 'Requiere Windows' }
$repo=Split-Path $PSScriptRoot
$java='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe'
$credential=Import-Clixml -LiteralPath "C:\req\config\mysql-windows-$Identity.credential.xml"
if ($credential -isnot [pscredential] -or $credential.UserName -ne "siger_windows_$Identity") { throw 'Identidad DPAPI no válida' }
$start=[Diagnostics.ProcessStartInfo]::new($java)
$start.UseShellExecute=$false
$start.RedirectStandardInput=$true
$start.RedirectStandardOutput=$true
$start.RedirectStandardError=$true
$start.CreateNoWindow=$true
foreach($argument in @('--class-path','C:\req\bin\lib\*',(Join-Path $PSScriptRoot 'WindowsBaseDatos.java'),$Mode,(Join-Path $repo 'siger-institucional\backend\src\main\resources\db\institucional\mysql'))) { $start.ArgumentList.Add($argument) }
$process=[Diagnostics.Process]::Start($start)
try {
 $payload=@{username=$credential.UserName;password=$credential.GetNetworkCredential().Password}|ConvertTo-Json -Compress
 $process.StandardInput.Write($payload); $process.StandardInput.Close(); $payload=$null
 $stdout=$process.StandardOutput.ReadToEndAsync(); $stderr=$process.StandardError.ReadToEndAsync()
 if (-not $process.WaitForExit(120000)) { $process.Kill($true);throw 'Tiempo de operación excedido' }
 $safe=($stdout.Result -split "\r?\n" | Where-Object { $_.StartsWith('{"') } | Select-Object -Last 1)
 if (-not $safe) { throw 'Sin resultado sanitizado del comprobador JDBC' }; $safe
 if ($process.ExitCode -ne 0) { throw 'Operación JDBC no completada; detalle remoto omitido' }
} finally { $process.Dispose();$credential=$null }