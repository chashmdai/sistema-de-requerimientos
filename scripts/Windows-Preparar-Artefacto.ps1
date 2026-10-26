param([string]$Jar='C:\req\repo\siger-institucional\backend\target\siger-institucional-0.1.0-SNAPSHOT.jar')
$ErrorActionPreference='Stop'
if (-not $IsWindows) {throw 'Solo Windows nativo'}
$jarPath=[IO.Path]::GetFullPath($Jar)
if (-not $jarPath.StartsWith('C:\req\repo\',[StringComparison]::OrdinalIgnoreCase) -or (Get-Item -LiteralPath $jarPath).LinkType) {throw 'Solo artefacto inactivo del repositorio Windows'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$lib='C:\req\bin\lib';[IO.Directory]::CreateDirectory($lib)|Out-Null
$zip=[IO.Compression.ZipFile]::OpenRead($jarPath)
try {foreach($entry in $zip.Entries|Where-Object {$_.FullName -match '^BOOT-INF/lib/[^/]+\.jar$'}) {
 $target=Join-Path $lib $entry.Name
 if (Test-Path -LiteralPath $target) {
  $stream=$entry.Open();try {$hash=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($stream)).ToLowerInvariant()}finally{$stream.Dispose()}
  if ((Get-FileHash -LiteralPath $target).Hash.ToLowerInvariant() -ne $hash) {throw 'Dependencia existente distinta: no sobrescribir'}
 } else {[IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$target,$false)}
}} finally {$zip.Dispose()}
$sha=(Get-FileHash -LiteralPath $jarPath).Hash.ToLowerInvariant();$dest="C:\req\bin\siger-$($sha.Substring(0,16)).jar"
if (Test-Path -LiteralPath $dest) {if ((Get-FileHash -LiteralPath $dest).Hash.ToLowerInvariant() -ne $sha) {throw 'Artefacto existente distinto'}}else{Copy-Item -LiteralPath $jarPath -Destination $dest -ErrorAction Stop}
@{artifact=$dest;sha256=$sha;started=$false;libraries='C:\req\bin\lib'}|ConvertTo-Json