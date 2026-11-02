[CmdletBinding()]
param([Parameter(Mandatory)][string]$Run,[Parameter(Mandatory)][string]$Evidencia)
$ErrorActionPreference='Stop'
if(-not $IsWindows -or $Run -notmatch '^C:\\req\\testdata\\runtime-qa-[a-f0-9]{32}$' -or -not ([IO.Path]::GetFullPath($Evidencia)).StartsWith('C:\req\evidencias\')){throw 'Solo QA Windows'}
if((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory*1KB -lt 4GB){throw 'Guarda documental4GiB no satisfecha'}
$path='C:\req\config\origen-dpapi\nexbyte-cuenta.json.dpapi';$allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544')
for($p=$path;$p;$p=[IO.Path]::GetDirectoryName($p)){if((Get-Item -LiteralPath $p -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Enlace no permitido'}}
foreach($r in (Get-Acl -LiteralPath $path).Access){if($r.AccessControlType -eq 'Allow' -and -not ($r.PropagationFlags -band [Security.AccessControl.PropagationFlags]::InheritOnly) -and $r.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowed){throw 'ACL no privada'}}
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$bytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($path),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
$process=$null;$payload=$null
try{
 $credential=[Text.Encoding]::UTF8.GetString($bytes)|ConvertFrom-Json;if($credential.usuario -ne 'nexbyte'){throw 'Cuenta distinta'}
 $data=@{run=$Run;verified=(Join-Path $Evidencia 'verificacion-institucional-interactiva.json');password=$credential.password;output=(Join-Path $Evidencia 'interfaz-nuevo-com.json');playwright='C:\Users\nexbyte.proveedor\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright-core'}
 $start=[Diagnostics.ProcessStartInfo]::new((Get-Command node.exe).Source);$start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
 foreach($arg in @('--use-system-ca',(Join-Path $PSScriptRoot 'recepcion-interactiva-siger.cjs'))){$start.ArgumentList.Add($arg)}
 $process=[Diagnostics.Process]::Start($start);$out=$process.StandardOutput.ReadToEndAsync();$err=$process.StandardError.ReadToEndAsync();$payload=$data|ConvertTo-Json -Compress;$process.StandardInput.Write($payload);$process.StandardInput.Close();$payload=$null;$credential=$null;$data.Clear()
 if(-not $process.WaitForExit(450000)){$process.Kill($true);throw 'Verificacion UI vencida'};$errorText=$err.GetAwaiter().GetResult();if($process.ExitCode -ne 0){$safe=@($errorText -split "`r?`n"|Where-Object {$_.StartsWith('{')})|Select-Object -Last 1;if($safe){$safe};throw 'UI no acreditada; valores privados omitidos'};$out.GetAwaiter().GetResult().Trim()
}finally{if($process){if(-not $process.HasExited){$process.Kill($true)};$process.Dispose()};[Security.Cryptography.CryptographicOperations]::ZeroMemory($bytes);$payload=$null;$credential=$null}
