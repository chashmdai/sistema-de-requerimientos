param()
$ErrorActionPreference='Stop'
if (-not $IsWindows) {throw 'Solo Windows'}
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$javaRoot='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin'
[IO.Directory]::CreateDirectory('C:\req\scratch\java-tools')|Out-Null
& "$javaRoot\javac.exe" --class-path 'C:\req\bin\lib\*' -d C:\req\scratch\java-tools (Join-Path $PSScriptRoot 'WindowsBaseDatos.java') (Join-Path $PSScriptRoot 'WindowsImportar.java') 2> C:\req\logs\import-compile.log
if ($LASTEXITCODE -ne 0) {throw 'No compilado; no se inicia importacion'}
$credential=Import-Clixml C:\req\config\mysql-windows-migrator.credential.xml
if ($credential.UserName -ne 'siger_windows_migrator') {throw 'Identidad no valida'}
$clear=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes('C:\req\config\origen-u4-snapshot.jsonl.dpapi'),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
$start=[Diagnostics.ProcessStartInfo]::new("$javaRoot\java.exe")
$start.UseShellExecute=$false;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true;$start.CreateNoWindow=$true
foreach($arg in @('--class-path','C:\req\bin\lib\*;C:\req\scratch\java-tools','WindowsImportar')) {$start.ArgumentList.Add($arg)}
$process=[Diagnostics.Process]::Start($start)
try {
 $payload=@{username=$credential.UserName;password=$credential.GetNetworkCredential().Password;snapshot=[Convert]::ToBase64String($clear)}|ConvertTo-Json -Compress
 $process.StandardInput.Write($payload);$process.StandardInput.Close();$payload=$null
 $stdout=$process.StandardOutput.ReadToEndAsync();$stderr=$process.StandardError.ReadToEndAsync()
 if (-not $process.WaitForExit(120000)) {$process.Kill($true);throw 'Importacion sin resultado conocido: comparar antes de reintentar'}
 $safe=($stdout.Result -split "\r?\n"|Where-Object {$_.StartsWith('{"')}|Select-Object -Last 1)
 if (-not $safe) {throw 'Sin resultado sanitizado; comparar antes de reintentar'}
 $safe
 if ($process.ExitCode -ne 0) {throw 'Importacion no completada; resultado SQL sanitizado'}
} finally {[Security.Cryptography.CryptographicOperations]::ZeroMemory($clear);$credential=$null;$process.Dispose()}