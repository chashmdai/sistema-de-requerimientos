[CmdletBinding()]
param([string]$Zip='C:\req\scratch\clamav-1.4.6.win.x64.zip')
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
# Portable: sin servicios, PATH global, exclusiones Defender ni documentos ciudadanos.
$sha='57b6fd1d60cd87bafe800f97407ecdef0576d36b3900b8b7abcfbbabe88295fd'
if((Get-FileHash -LiteralPath $Zip -Algorithm SHA256).Hash.ToLowerInvariant() -ne $sha){throw 'ZIP distinto del release oficial Cisco-Talos 1.4.6'}
$dest='C:\req\bin\clamav-1.4.6'
if(Test-Path -LiteralPath $dest){throw 'Conservar paquete existente; no sobrescribir'}
$null=New-Item -ItemType Directory -Path $dest
$archive=[IO.Compression.ZipFile]::OpenRead($Zip)
try{
 foreach($entry in $archive.Entries){
  if(-not $entry.FullName.StartsWith('clamav-1.4.6.win.x64/')){throw 'Raiz ZIP no admitida'}
  $relative=$entry.FullName.Substring('clamav-1.4.6.win.x64/'.Length)
  if(-not $relative -or $relative.EndsWith('/')){continue}
  if($relative -notmatch '^[^/]+\.dll$|^(clamscan|freshclam)\.exe$|^COPYING(\.txt|/[^/]+)$|^conf_examples/freshclam\.conf\.sample$'){continue}
  $target=[IO.Path]::GetFullPath((Join-Path $dest $relative))
  if(-not $target.StartsWith($dest+'\',[StringComparison]::OrdinalIgnoreCase) -or (($entry.ExternalAttributes -shr 16) -band 0xF000) -eq 0xA000){throw 'Entrada ZIP no regular'}
  [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target))|Out-Null
  [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$target,$false)
 }
}finally{$archive.Dispose()}
$manifest=Get-ChildItem -LiteralPath $dest -File -Recurse|ForEach-Object {@{name=[IO.Path]::GetRelativePath($dest,$_.FullName);bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName).Hash.ToLowerInvariant()}}
@{source='https://github.com/Cisco-Talos/clamav/releases/tag/clamav-1.4.6';zipSHA256=$sha;files=$manifest;serviceInstalled=$false;scannerIntegrated=$false}|ConvertTo-Json -Depth 6|Set-Content C:\req\evidencias\clamav-portable-2026-10-26.json
& (Join-Path $dest 'clamscan.exe') --version
if($LASTEXITCODE -ne 0){throw 'ClamAV no ejecutable'}
