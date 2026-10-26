param()
$ErrorActionPreference='Stop'
if (-not $IsWindows) { throw 'Requiere Windows y DPAPI CurrentUser' }
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
$root=[string]::new([char]92,2)+'wsl.localhost\Ubuntu-24.04\home\nexbyteproveedor\.local\share\siger\institucional-completo-lab'
$dest='C:\req\config\origen-dpapi'
if (Test-Path -LiteralPath $dest) { throw 'Custodia ya existente: no sobrescribir' }
[IO.Directory]::CreateDirectory($dest) | Out-Null
$files=@('application.properties','institucional-key.pem','institucional-trust.json','publico-trust.json','servidor.p12','rootCA.pem','nexbyte-cuenta.json','identidad-qa.json','notificaciones/allowlist.txt','notificaciones/graph-client-secret.txt')
$inventory=@()
foreach ($relative in $files) {
 $src=Join-Path $root $relative
 if (-not (Test-Path -LiteralPath $src -PathType Leaf)) { throw 'Material privado requerido ausente' }
 $bytes=[IO.File]::ReadAllBytes($src)
 try {
  $protected=[Security.Cryptography.ProtectedData]::Protect($bytes,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
  $target=Join-Path $dest ($relative.Replace('/','-')+'.dpapi')
  [IO.File]::WriteAllBytes($target,$protected)
  $check=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($target),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
  if (-not [Security.Cryptography.CryptographicOperations]::FixedTimeEquals($bytes,$check)) { throw 'Verificación DPAPI fallida' }
  $inventory+=@{source=$relative;destination=[IO.Path]::GetFileName($target);roundtrip=$true}
 } finally { [Security.Cryptography.CryptographicOperations]::ZeroMemory($bytes);if ($check) { [Security.Cryptography.CryptographicOperations]::ZeroMemory($check) } }
}
$inventory | ConvertTo-Json | Set-Content C:\req\evidencias\custodia-dpapi.json
@{files=$files.Count;roundtrip=$true;scope='CurrentUser';runtimeConfigured=$false} | ConvertTo-Json -Compress