[CmdletBinding()]
param([string]$Run=('C:\req\testdata\sin-agente-'+[guid]::NewGuid().ToString()))
$ErrorActionPreference='Stop'
$expected='847cd77c5af327be992bc4e46aa83038f35f9f7b67229829c661412857c3b471'
$artifact='C:\req\bin\siger-847cd77c5af327be.jar'
$resolved=[IO.Path]::GetFullPath($Run);$parent='C:\req\testdata'
if([IO.Path]::GetDirectoryName($resolved) -ne $parent -or [IO.Path]::GetFileName($resolved) -notmatch '^sin-agente-[a-f0-9-]{36}$' -or (Test-Path -LiteralPath $resolved)){throw 'Solo raíz QA nueva'}
if((Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected){throw 'Artefacto institucional distinto del checkpoint; revisar antes'}
$null=New-Item -ItemType Directory -Path $resolved
$archive=[IO.Compression.ZipFile]::OpenRead($artifact)
try{
 foreach($entry in $archive.Entries){
  if($entry.FullName.StartsWith('BOOT-INF/classes/') -and -not $entry.FullName.EndsWith('/')){
   $relative=$entry.FullName.Substring(17);$base=Join-Path $resolved 'institucional-classes'
   $target=[IO.Path]::GetFullPath((Join-Path $base $relative))
   if(-not $target.StartsWith($base+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Ruta de JAR inválida'}
   [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target))|Out-Null
   [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$target,$false)
  }
 }
}finally{$archive.Dispose()}
$java='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
& (Join-Path $java 'bin\javac.exe') -encoding UTF-8 -cp ((Join-Path $resolved 'institucional-classes')+';C:\req\bin\lib\*') -d (Join-Path $resolved 'classes') (Join-Path $PSScriptRoot 'RecepcionSinAgenteWindows.java')
if($LASTEXITCODE -ne 0){throw 'Compilación del cliente QA falló'}
@{run=$resolved;id=[IO.Path]::GetFileName($resolved).Substring(11);artifactSHA256=$expected;compiled=$true}|ConvertTo-Json -Compress
