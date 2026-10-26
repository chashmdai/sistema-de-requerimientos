param()
$ErrorActionPreference='Stop'
if (-not $IsWindows) {throw 'Solo Windows'}
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$manifest=Get-Content C:\req\evidencias\respaldo-origen.json -Raw|ConvertFrom-Json
$snapshot=Get-Content C:\req\evidencias\sql-snapshot-origen.json -Raw|ConvertFrom-Json
$files=@(@{path='C:\req\config\origen-u4-datos.sql';sha=$manifest.dumpSha256},@{path='C:\req\config\origen-u4-snapshot.jsonl';sha=$snapshot.snapshotSha256})
foreach($item in $files) {
 $plain=$item.path;$target=$plain+'.dpapi'
 if ((Test-Path -LiteralPath $plain) -and (Test-Path -LiteralPath $target)) {throw 'Ambos existen: revisar, no sobrescribir'}
 if (Test-Path -LiteralPath $plain) {
  $bytes=[IO.File]::ReadAllBytes($plain)
  try {
   if ([Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes)).ToLowerInvariant() -ne $item.sha) {throw 'SHA snapshot invalido'}
   [IO.File]::WriteAllBytes($target,[Security.Cryptography.ProtectedData]::Protect($bytes,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))
  } finally {[Security.Cryptography.CryptographicOperations]::ZeroMemory($bytes)}
 }
 $clear=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($target),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
 try {if ([Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($clear)).ToLowerInvariant() -ne $item.sha) {throw 'DPAPI snapshot invalido'}} finally {[Security.Cryptography.CryptographicOperations]::ZeroMemory($clear)}
 # Solo se retira el temporal exacto creado por la exportacion y ya cotejado bajo DPAPI.
 if (Test-Path -LiteralPath $plain) {Remove-Item -LiteralPath $plain}
}
@{snapshots=$files.Count;dpapi='CurrentUser';sha256Verified=$true;plaintextRetained=$false}|ConvertTo-Json