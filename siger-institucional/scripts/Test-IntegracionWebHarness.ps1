#Requires -Version 7.4
$ErrorActionPreference='Stop'
$base=Join-Path ([IO.Path]::GetTempPath()) ('siger-web-package-'+[guid]::NewGuid().ToString('N'))
$dist=Join-Path $base 'dist';[IO.Directory]::CreateDirectory($dist)|Out-Null
[IO.File]::WriteAllText((Join-Path $dist 'index.html'),'<html>Interfaz sintética</html>')
[IO.File]::WriteAllText((Join-Path $dist 'app.js'),'const sintetico=true;')
$jar=Join-Path $base 'fixture.jar';$script=Join-Path $PSScriptRoot 'Test-IntegracionWeb.ps1'
function Package-Fixture([string]$variation){
 $stream=[IO.File]::Open($jar,[IO.FileMode]::Create)
 $zip=[IO.Compression.ZipArchive]::new($stream,[IO.Compression.ZipArchiveMode]::Create)
 try{
  foreach($file in Get-ChildItem -LiteralPath $dist -File){
   if($variation -eq 'missing' -and $file.Name -eq 'app.js'){continue}
   $entry=$zip.CreateEntry('BOOT-INF/classes/static/'+$file.Name);$writer=[IO.StreamWriter]::new($entry.Open())
   try{$writer.Write($(if($variation -eq 'changed' -and $file.Name -eq 'app.js'){'alterado'}else{[IO.File]::ReadAllText($file.FullName)}))}finally{$writer.Dispose()}
  }
  if($variation -in @('extra','duplicate')){$entry=$zip.CreateEntry('BOOT-INF/classes/static/'+$(if($variation -eq 'extra'){'old.js'}else{'app.js'}));$entry.Open().Dispose()}
 }finally{$zip.Dispose();$stream.Dispose()}
}
Package-Fixture 'valid';& $script -JarPath $jar -DistPath $dist
$checks=0
foreach($bad in @('missing','changed','extra','duplicate')){
 Package-Fixture $bad;$denied=$false
 try{& $script -JarPath $jar -DistPath $dist|Out-Null}catch{$denied=$true}
 if(-not $denied){throw "Integración aceptó fixture inválido: $bad"};$checks++
}
Write-Output "Integración sintética: caso válido y $checks controles negativos; fixtures conservados en $base. No es un JAR ejecutable."
