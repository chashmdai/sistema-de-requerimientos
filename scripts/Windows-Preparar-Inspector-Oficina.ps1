[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
if(-not $IsWindows){throw 'Solo Windows nativo'}
$jdk='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
$source=Join-Path $PSScriptRoot 'windows-documental'
$sha=[Security.Cryptography.IncrementalHash]::CreateHash([Security.Cryptography.HashAlgorithmName]::SHA256)
try{foreach($file in @('module-info.java','InspectorOficina.java')){$sha.AppendData([IO.File]::ReadAllBytes((Join-Path $source $file)))};$digest=[Convert]::ToHexString($sha.GetHashAndReset()).ToLowerInvariant()}finally{$sha.Dispose()}
$dest='C:\req\bin\java-documental-'+$digest.Substring(0,16)
$manifest='C:\req\config\java-documental-'+$digest.Substring(0,16)+'-manifest.json'
if(Test-Path -LiteralPath $dest){
 if(-not(Test-Path -LiteralPath $manifest)){throw 'Imagen existente sin manifiesto; no sobrescribir'}
 $saved=Get-Content -LiteralPath $manifest -Raw|ConvertFrom-Json
 foreach($f in $saved.files){if((Get-FileHash -LiteralPath (Join-Path $dest $f.name)).Hash.ToLowerInvariant() -ne $f.sha256){throw 'Imagen alterada'}}
}else{
 $build='C:\req\scratch\oficina-build-'+[guid]::NewGuid().ToString('N');$null=New-Item -ItemType Directory -Path (Join-Path $build 'classes')
 & "$jdk\bin\javac.exe" -J-Xmx256m -J-XX:-UsePerfData -d (Join-Path $build 'classes') (Join-Path $source 'module-info.java') (Join-Path $source 'InspectorOficina.java') *> (Join-Path $build 'javac.log');if($LASTEXITCODE){throw 'Inspector no compila'}
 & "$jdk\bin\jar.exe" ('-J-Djava.io.tmpdir='+$build) --create --file (Join-Path $build 'worker.jar') -C (Join-Path $build 'classes') .;if($LASTEXITCODE){throw 'Modulo no construido'}
 & "$jdk\bin\jlink.exe" -J-Xmx256m -J-XX:-UsePerfData --module-path ($jdk+'\jmods;'+(Join-Path $build 'worker.jar')) --add-modules cl.siger.documental --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output $dest *> (Join-Path $build 'jlink.log');if($LASTEXITCODE){throw 'Imagen modular no construida'}
 $files=@(Get-ChildItem -LiteralPath $dest -Recurse -File|ForEach-Object {@{name=[IO.Path]::GetRelativePath($dest,$_.FullName);bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName).Hash.ToLowerInvariant()}})
 @{root=$dest;sourceSHA256=$digest;jdkReleaseSHA256=(Get-FileHash -LiteralPath "$jdk\release").Hash.ToLowerInvariant();modules=@('java.base','java.xml','cl.siger.documental');files=$files}|ConvertTo-Json -Depth 5|Set-Content -LiteralPath $manifest
}
@{root=$dest;manifest=$manifest;sourceSHA256=$digest}|ConvertTo-Json -Compress
