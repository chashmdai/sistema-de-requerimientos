#Requires -Version 7.4
[CmdletBinding()]
param([string]$JarPath=(Join-Path $PSScriptRoot '../backend/target/siger-institucional-0.1.0-SNAPSHOT.jar'),
      [string]$DistPath=(Join-Path $PSScriptRoot '../front/dist'))
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath($DistPath)
if(-not(Test-Path -LiteralPath (Join-Path $root 'index.html') -PathType Leaf)){throw 'No existe la interfaz compilada que se pretende integrar.'}
$files=@(Get-ChildItem -LiteralPath $root -Recurse -File)
$jar=[IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($JarPath))
try{
 $prefix='BOOT-INF/classes/static/'
 $entries=@($jar.Entries|Where-Object {$_.FullName.StartsWith($prefix,[StringComparison]::Ordinal) -and -not $_.FullName.EndsWith('/')})
 if($entries.Count -ne $files.Count -or @($entries.FullName|Sort-Object -Unique).Count -ne $entries.Count){throw 'Recursos web faltantes, adicionales o duplicados dentro del JAR.'}
 foreach($file in $files){
  $name=$prefix+[IO.Path]::GetRelativePath($root,$file.FullName).Replace('\','/')
  $entry=$jar.GetEntry($name);if(-not $entry){throw 'Recurso web compilado no incluido en el JAR.'}
  $source=$file.OpenRead();$packed=$entry.Open()
  try{if(-not [Security.Cryptography.CryptographicOperations]::FixedTimeEquals([Security.Cryptography.SHA256]::HashData($source),[Security.Cryptography.SHA256]::HashData($packed))){throw 'Recurso web empaquetado diferente del compilado verificado.'}}
  finally{$source.Dispose();$packed.Dispose()}
 }
 Write-Output "Integración web: $($files.Count) archivos compilados idénticos por SHA-256 dentro del JAR; no acredita ejecución o despliegue."
}finally{$jar.Dispose()}
