param()
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows'}
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$path='C:\req\config\windows-runtime-qa-identidades.json.dpapi'
if(-not (Test-Path -LiteralPath $path)){
 $identities=@{};foreach($name in @('qa.windows.ti','qa.windows.otra')){$identities[$name]=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(30))}
 $clear=[Text.Encoding]::UTF8.GetBytes(($identities|ConvertTo-Json -Compress))
 try{[IO.File]::WriteAllBytes($path,[Security.Cryptography.ProtectedData]::Protect($clear,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))}finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($clear)}
}
$private=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($path),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
$nexbyte=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes('C:\req\config\origen-dpapi\nexbyte-cuenta.json.dpapi'),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
try{
 $creds=Import-Clixml C:\req\config\mysql-windows-qa-app.credential.xml
 if($creds.UserName -ne 'siger_windows_qa_app'){throw 'Solo QA DML'}
 $inputData=[Text.Encoding]::UTF8.GetString($private)|ConvertFrom-Json -AsHashtable
 $n=[Text.Encoding]::UTF8.GetString($nexbyte)|ConvertFrom-Json
 if($n.usuario -ne 'nexbyte'){throw 'Cuenta privada distinta'}
 $inputData.user=$creds.UserName;$inputData.password=$creds.GetNetworkCredential().Password;$inputData.nexbytePassword=$n.password
 $start=[Diagnostics.ProcessStartInfo]::new('C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe');$start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
 foreach($arg in @('-XX:-UsePerfData','--class-path','C:\req\repo\siger-institucional\backend\target\classes;C:\req\bin\lib\*',(Join-Path $PSScriptRoot 'IdentidadesRuntimeWindows.java'))){$start.ArgumentList.Add($arg)}
 $process=[Diagnostics.Process]::Start($start)
 try{$payload=$inputData|ConvertTo-Json -Compress;$process.StandardInput.Write($payload);$process.StandardInput.Close();$payload=$null;$inputData.Clear();$out=$process.StandardOutput.ReadToEndAsync();$err=$process.StandardError.ReadToEndAsync();if(-not $process.WaitForExit(60000)){$process.Kill();throw 'Provision QA vencida'};if($process.ExitCode -ne 0){throw 'Provision QA no comprobada; salida remota omitida'};$out.Result.Trim()|Tee-Object C:\req\evidencias\identidades-runtime-windows-2026-10-26.json}finally{$process.Dispose()}
}finally{[Security.Cryptography.CryptographicOperations]::ZeroMemory($private);[Security.Cryptography.CryptographicOperations]::ZeroMemory($nexbyte);$creds=$null;$inputData=$null;$n=$null}
