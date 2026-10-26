[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
$lock=Get-Content (Join-Path $PSScriptRoot 'windows-documental\tools-lock.json') -Raw|ConvertFrom-Json
foreach($p in $lock){
 $archive=Join-Path C:\req\scratch $p.asset
 if(-not (Test-Path -LiteralPath $archive)){Invoke-WebRequest -Uri $p.url -OutFile $archive}
 if((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $p.sha256){throw 'Archivo distinto de lock publicado'}
 $dest=Join-Path C:\req\bin ($p.key+'-'+$p.version)
 $manifest=Join-Path C:\req\config ($p.key+'-windows-manifest.json')
 if(Test-Path -LiteralPath $dest){
  if(-not (Test-Path -LiteralPath $manifest)){throw 'Paquete existente sin manifiesto; conciliar, no sobrescribir'}
  $saved=Get-Content -LiteralPath $manifest -Raw|ConvertFrom-Json
  if($saved.archiveSHA256 -ne $p.sha256){throw 'Version existente diferente'}
  foreach($f in $saved.files){if((Get-FileHash -LiteralPath (Join-Path $dest $f.name) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $f.sha256){throw 'Paquete instalado alterado'}}
  continue
 }
 $null=New-Item -ItemType Directory -Path $dest
 $zip=[IO.Compression.ZipFile]::OpenRead($archive)
 try{
  $prefix=switch($p.key){'qpdf'{"qpdf-$($p.version)-msvc64/"}'poppler'{"poppler-26.09.0/Library/"}'ffmpeg'{"ffmpeg-n8.1-latest-win64-lgpl-shared-8.1/"}}
  $allowed=switch($p.key){'qpdf'{@('qpdf.exe')}'poppler'{@('pdfinfo.exe','pdftoppm.exe')}'ffmpeg'{@('ffmpeg.exe','ffprobe.exe')}}
  foreach($entry in $zip.Entries){
   if(-not $entry.FullName.StartsWith($prefix,[StringComparison]::Ordinal)){continue}
   $relative=$entry.FullName.Substring($prefix.Length)
   if(-not $relative -or $relative.EndsWith('/')){continue}
   if($relative -match '(^|/)\.\.(/|$)|\\|:' -or (($entry.ExternalAttributes -shr 16)-band 0xF000) -eq 0xA000){throw 'Ruta de archivo no regular'}
   if($relative -match '\.exe$' -and (Split-Path -Leaf $relative) -notin $allowed){continue}
   if($relative -notmatch '^bin/|^share/|^etc/|^doc/|^LICENSE|^COPYING|^README'){continue}
   $target=[IO.Path]::GetFullPath((Join-Path $dest $relative))
   if(-not $target.StartsWith($dest+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Escape de ZIP'}
   [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target))|Out-Null
   [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$target,$false)
  }
 }finally{$zip.Dispose()}
 $files=@(Get-ChildItem -LiteralPath $dest -File -Recurse|ForEach-Object {@{name=[IO.Path]::GetRelativePath($dest,$_.FullName);bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}})
 @{root=$dest;version=$p.version;source=$p.release;archiveSHA256=$p.sha256;files=$files}|ConvertTo-Json -Depth 6|Set-Content -LiteralPath $manifest
 [pscustomobject]@{key=$p.key;root=$dest;files=$files.Count;archiveVerified=$true}|ConvertTo-Json -Compress
}
