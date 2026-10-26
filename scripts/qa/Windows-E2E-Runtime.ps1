param([Parameter(Mandatory)][string]$Run,[ValidateSet('operate','after-restart')][string]$Mode='operate',[string]$Baseline='')
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath($Run)
if([IO.Path]::GetDirectoryName($root) -ne 'C:\req\testdata' -or [IO.Path]::GetFileName($root) -notmatch '^runtime-qa-[a-f0-9]{32}$'){throw 'Solo raiz QA propia'}
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$restricted=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes('C:\req\config\windows-runtime-qa-identidades.json.dpapi'),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
$nex=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes('C:\req\config\origen-dpapi\nexbyte-cuenta.json.dpapi'),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
try{
 $settings=@{run=$root;mode=$Mode;baseline=$Baseline;playwright='C:\Users\nexbyte.proveedor\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright-core';restricted=([Text.Encoding]::UTF8.GetString($restricted)|ConvertFrom-Json);nexbytePassword=([Text.Encoding]::UTF8.GetString($nex)|ConvertFrom-Json).password}
 $start=[Diagnostics.ProcessStartInfo]::new((Get-Command node.exe).Source);$start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true;$start.ArgumentList.Add('--use-system-ca');$start.ArgumentList.Add((Join-Path $PSScriptRoot 'runtime-edge-windows.cjs'))
 $process=[Diagnostics.Process]::Start($start)
 try{$payload=$settings|ConvertTo-Json -Compress -Depth 4;$process.StandardInput.Write($payload);$process.StandardInput.Close();$payload=$null;$settings.Clear();$out=$process.StandardOutput.ReadToEndAsync();$err=$process.StandardError.ReadToEndAsync();if(-not $process.WaitForExit(150000)){$process.Kill($true);throw 'E2E vencido'};if($process.ExitCode -ne 0){$err.Result.Trim()|Write-Output;throw 'E2E no comprobado; detalles de autenticacion omitidos'};$out.Result.Trim()}finally{$process.Dispose()}
}finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($restricted);[Security.Cryptography.CryptographicOperations]::ZeroMemory($nex);$settings=$null}
