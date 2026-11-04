#Requires -Version 7.4
[CmdletBinding()]param([string]$Root='C:\req\smr',[Parameter(Mandatory)][string]$Version,[string]$JavaSource='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot',[string]$NssmSource='C:\smid-ops\tools\nssm.exe')
. "$PSScriptRoot\Common.ps1"
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'));$release=Join-Path $Root "releases\$Version"
if($Version -notmatch '^[a-zA-Z0-9_-]{1,80}$' -or (Test-Path -LiteralPath $release)){throw 'Release debe ser nuevo'}
$java=Join-Path $JavaSource 'bin\java.exe';$ver=(& $java --version 2>&1)-join ' ';if($LASTEXITCODE -or $ver -notmatch 'openjdk 25\.') {throw 'Fuente Java25 requerida'}
$globalBefore=@{machineJavaHome=[Environment]::GetEnvironmentVariable('JAVA_HOME','Machine');userJavaHome=[Environment]::GetEnvironmentVariable('JAVA_HOME','User');machinePath=[Environment]::GetEnvironmentVariable('PATH','Machine');userPath=[Environment]::GetEnvironmentVariable('PATH','User');resolvedJava=(Get-Command java.exe).Source}
foreach($p in @($release,"$release\tools","$release\lib","$release\runtime")){[IO.Directory]::CreateDirectory($p)|Out-Null}
Copy-Item -LiteralPath "$repo\siger-institucional\backend\target\siger-institucional-0.1.0-SNAPSHOT.jar" -Destination "$release\smr-rc1.jar"
Copy-Item -LiteralPath "$repo\siger-institucional\backend\target\siger-institucional-0.1.0-SNAPSHOT.jar.original" -Destination "$release\tools\core.jar"
Copy-Item -LiteralPath $PSScriptRoot -Destination "$release\scripts" -Recurse
# JDK Temurin completo: runtime portable con licencias, módulos y herramientas; no instalador ni cambios globales.
Copy-Item -LiteralPath $JavaSource -Destination "$release\runtime\java25" -Recurse
Copy-Item -LiteralPath ([IO.Path]::GetDirectoryName((Get-Process -Id $PID).Path)) -Destination "$release\runtime\pwsh" -Recurse
if(Test-Path -LiteralPath $NssmSource){[IO.Directory]::CreateDirectory("$release\runtime\service")|Out-Null;Copy-Item -LiteralPath $NssmSource -Destination "$release\runtime\service\nssm.exe"}
$archive=[IO.Compression.ZipFile]::OpenRead("$release\smr-rc1.jar")
try{foreach($e in $archive.Entries){if($e.FullName -match '^BOOT-INF/lib/([^/]+\.jar)$'){[IO.Compression.ZipFileExtensions]::ExtractToFile($e,(Join-Path "$release\lib" $Matches[1]),$false)}}}finally{$archive.Dispose()}
$files=@(Get-ChildItem "$release\runtime" -Recurse -File|ForEach-Object {@{path=[IO.Path]::GetRelativePath($release,$_.FullName).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_.FullName).Hash.ToLowerInvariant()}})
$files|ConvertTo-Json -Depth 4|Set-Content -LiteralPath "$release\runtime-manifest.json"
$deps=@(Get-ChildItem "$release\lib" -File|ForEach-Object {@{file=$_.Name;sha256=(Get-FileHash -LiteralPath $_.FullName).Hash.ToLowerInvariant()}})
$deps|ConvertTo-Json -Depth 4|Set-Content -LiteralPath "$release\dependencies.json"
$manifest=@{version=$Version;sourceCommit=(git -C $repo rev-parse HEAD);sourceDirty=[bool](git -C $repo status --porcelain);jarSha256=(Get-FileHash "$release\smr-rc1.jar").Hash.ToLowerInvariant();javaExeSha256=(Get-FileHash "$release\runtime\java25\bin\java.exe").Hash.ToLowerInvariant();javaVersion=$ver;javaRuntime='runtime/java25';runtimeManifestSha256=(Get-FileHash "$release\runtime-manifest.json").Hash.ToLowerInvariant();createdUtc=[DateTime]::UtcNow.ToString('o');productionE2e=$false;secretMaterialIncluded=$false}
$manifest|ConvertTo-Json -Depth 5|Set-Content -LiteralPath "$release\manifest.json"
$globalAfter=@{machineJavaHome=[Environment]::GetEnvironmentVariable('JAVA_HOME','Machine');userJavaHome=[Environment]::GetEnvironmentVariable('JAVA_HOME','User');machinePath=[Environment]::GetEnvironmentVariable('PATH','Machine');userPath=[Environment]::GetEnvironmentVariable('PATH','User');resolvedJava=(Get-Command java.exe).Source}
foreach($k in $globalBefore.Keys){if($globalBefore[$k] -cne $globalAfter[$k]){throw 'Cambio global inesperado; investigar'}}
@{unchanged=$true;globalJavaExecutable=$globalAfter.resolvedJava;portableJavaExecutable="$release\runtime\java25\bin\java.exe";portableJavaVersion=$ver}|ConvertTo-Json|Set-Content -LiteralPath "$release\java-isolation.json"
Write-Output "Paquete SMR con Java25 portable: $release"
