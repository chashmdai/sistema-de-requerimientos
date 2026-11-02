[CmdletBinding()]
param([Parameter(Mandatory)][string]$Run,[Parameter(Mandatory)][string]$Evidencia)
$ErrorActionPreference='Stop'
if(-not $IsWindows -or $Run -notmatch '^C:\\req\\testdata\\runtime-qa-[a-f0-9]{32}$' -or -not ([IO.Path]::GetFullPath($Evidencia)).StartsWith('C:\req\evidencias\')){throw 'Solo QA Windows'}
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$bytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes('C:\req\config\origen-dpapi\nexbyte-cuenta.json.dpapi'),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
$process=$null;$payload=$null
try{
 $credential=[Text.Encoding]::UTF8.GetString($bytes)|ConvertFrom-Json;if($credential.usuario -ne 'nexbyte'){throw 'Cuenta distinta'}
 $inputData=@{run=$Run;password=$credential.password;output=(Join-Path $Evidencia 'acceso-siger-verificado.json');playwright='C:\Users\nexbyte.proveedor\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright-core'}
 $start=[Diagnostics.ProcessStartInfo]::new((Get-Command node.exe).Source);$start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
 foreach($arg in @('--use-system-ca',(Join-Path $PSScriptRoot 'acceso-interactivo-siger.cjs'))){$start.ArgumentList.Add($arg)}
 $process=[Diagnostics.Process]::Start($start);$out=$process.StandardOutput.ReadToEndAsync();$err=$process.StandardError.ReadToEndAsync();$payload=$inputData|ConvertTo-Json -Compress;$process.StandardInput.Write($payload);$process.StandardInput.Close();$payload=$null;$inputData.Clear();$credential=$null
 if(-not $process.WaitForExit(90000)){$process.Kill($true);throw 'Comprobacion vencida'};$errorText=$err.GetAwaiter().GetResult();if($process.ExitCode -ne 0){$safe=@($errorText -split "`r?`n"|Where-Object {$_.StartsWith('{')})|Select-Object -Last 1;if($safe){$safe};throw 'Acceso QA no acreditado; valores privados omitidos'};$out.GetAwaiter().GetResult().Trim()
}finally{if($process){if(-not $process.HasExited){$process.Kill($true)};$process.Dispose()};[Security.Cryptography.CryptographicOperations]::ZeroMemory($bytes);$payload=$null;$credential=$null}
